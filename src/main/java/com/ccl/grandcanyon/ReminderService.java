package com.ccl.grandcanyon;

import com.ccl.grandcanyon.reminders.TwilioReminderService;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ContactMethod;
import com.ccl.grandcanyon.types.Reminder;
import com.nimbusds.jose.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ReminderService {

  private final static String REMINDER_SERVICE_ENABLED = "reminderServiceEnabled";
  private final static String REMINDER_SERVICE_INTERVAL = "reminderServiceInterval";
  private final static String SECOND_REMINDER_INTERVAL = "secondReminderInterval";
  private final static String EARLIEST_REMINDER_TIME = "earliestReminderTime";
  private final static String LATEST_REMINDER_TIME = "latestReminderTime";

  private final static String SQL_SELECT_REMINDER =
      "SELECT * FROM reminders WHERE " +
          Reminder.TRACKING_ID + " = ?";

  private final static String SQL_INSERT_REMINDER =
      "INSERT into reminders (" +
          Reminder.CALLER_ID + ", " +
          Reminder.DAY_OF_MONTH +
          ") VALUES (?, ?)";

  private final static String SQL_UPDATE_REMINDER =
      "UPDATE reminders SET " +
          Reminder.LAST_REMINDER_TIMESTAMP + " = ?, " +
          Reminder.DAY_OF_MONTH + " = ?, " +
          Reminder.TRACKING_ID + " = ? " +
          "WHERE " + Reminder.CALLER_ID + " = ?";


  private final static String SQL_SELECT_CALLERS =
      "SELECT r.*, c.*, ccm.contact_method FROM reminders r " +
          "LEFT JOIN callers AS c ON c.caller_id = r.caller_id " +
          "LEFT JOIN callers_contact_methods AS ccm on c.caller_id = ccm.caller_id";


  private static final Logger logger = Logger.getLogger(ReminderService.class.getName());

  private static ReminderService instance;

  // frequency, in minutes, that the service wakes up to send reminders
  private int serviceIntervalMinutes;
  // number of days before sending second reminder to a caller
  private int secondReminderInterval;

  private OffsetTime earliestReminder;
  private OffsetTime latestReminder;

  private ScheduledFuture reminderTask;
  private SecureRandom rand = new SecureRandom();



  public static void init(Properties config) throws JOSEException {

    assert(instance == null);
    instance = new ReminderService(config);
  }

  public static ReminderService getInstance() {
    return instance;
  }


  private ReminderService(Properties config) {

    this.serviceIntervalMinutes = Integer.parseInt(config.getProperty(REMINDER_SERVICE_INTERVAL, "60"));
    this.secondReminderInterval = Integer.parseInt(config.getProperty(SECOND_REMINDER_INTERVAL, "4"));
    try {
      this.earliestReminder = OffsetTime.parse(config.getProperty(EARLIEST_REMINDER_TIME));
    }
    catch (DateTimeParseException e) {
      logger.warning("Failed to parse property 'earliestReminderTime', using default of 09:00 EST");
      this.earliestReminder = OffsetTime.of(9, 0, 0, 0, ZoneOffset.ofHours(-5));
    }
    try {
      this.latestReminder = OffsetTime.parse(config.getProperty(LATEST_REMINDER_TIME));
    }
    catch (DateTimeParseException e) {
      logger.warning("Failed to parse property 'latestReminderTime', using default of 18:00 EST");
      this.latestReminder = OffsetTime.of(18, 0, 0, 0, ZoneOffset.ofHours(-5));
    }

    // todo: make SMS/Email services more generic
    TwilioReminderService.init(config);

    if (Boolean.parseBoolean(config.getProperty(REMINDER_SERVICE_ENABLED))) {
      // start the background task that will send reminders to callers
      this.reminderTask = Executors.newSingleThreadScheduledExecutor().
          scheduleAtFixedRate(new ReminderSender(), 10, serviceIntervalMinutes * 60, TimeUnit.SECONDS);
    }
  }


  public void tearDown() {
    if (reminderTask != null) {
      reminderTask.cancel(true);
    }
  }


  public void createInitialReminder(
      Connection conn,
      int callerId) throws SQLException {

    OffsetDateTime currentDateTime = OffsetDateTime.now();
    int dayOfMonth = currentDateTime.toOffsetTime().isAfter(latestReminder) ?
        currentDateTime.plusDays(1).getDayOfMonth() :
        currentDateTime.getDayOfMonth();

    PreparedStatement statement = conn.prepareStatement(SQL_INSERT_REMINDER);
    statement.setInt(1, callerId);
    statement.setInt(2, dayOfMonth);
    statement.executeUpdate();
  }


  public Reminder getReminderByTrackingId(
      Connection conn,
      String trackingId) throws SQLException {

    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_REMINDER);
    statement.setString(1, trackingId);
    ResultSet rs = statement.executeQuery();
    if (rs.next()) {
      return new Reminder(rs);
    }
    return null;
  }



  private boolean sendReminder(
      Caller caller,
      String trackingId) {

    boolean reminderSent = false;
    if (caller.getContactMethods().contains(ContactMethod.sms)) {
      try {
        reminderSent = new TwilioReminderService(caller, trackingId).send();
      }
      catch (Exception e) {
        logger.warning(String.format("Failed to send SMS to caller {id: %d name %s %s}: %s",
            caller.getCallerId(), caller.getFirstName(), caller.getLastName(), e.getMessage()));
        reminderSent = false;
      }
    }
    if (caller.getContactMethods().contains(ContactMethod.email)) {
      // todo: email
    }
    if (reminderSent) {
      logger.info(String.format("Sent reminder to caller {id: %d name %s %s}.",
          caller.getCallerId(), caller.getFirstName(), caller.getLastName()));
    }
    return reminderSent;
  }

  private void updateReminderStatus(
      Connection conn,
      int callerId,
      String trackingId) throws SQLException {

    PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_REMINDER);
    int idx = 1;
    statement.setTimestamp(idx++, new Timestamp(System.currentTimeMillis()));
    statement.setInt(idx++, LocalDateTime.now().getDayOfMonth());
    statement.setString(idx++, trackingId);
    statement.setInt(idx, callerId);
    statement.executeUpdate();
  }



  class ReminderSender implements Runnable {

    @Override
    public void run() {

      LocalDateTime currentDateTime = LocalDateTime.now();

      DayOfWeek dayOfWeek = currentDateTime.getDayOfWeek();
      if (dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) {
        // do nothing
        return;
      }

      LocalTime currentTime = currentDateTime.toLocalTime();
      if (currentTime.isBefore(earliestReminder.toLocalTime()) ||
          currentTime.isAfter(latestReminder.toLocalTime())) {
        // do nothing
        return;
      }

      // In order to select all callers who might get a reminder today, first
      // determine what days of the month are in play.  Today is always included.
      Set<Integer> daysToQuery = new HashSet<>();

      int todaysDayOfMonth = currentDateTime.getDayOfMonth();
      daysToQuery.add(todaysDayOfMonth);

      // If today is the last day of a short month, include callers whose
      // reminder day is greater than today.
      int numberofDaysInMonth = currentDateTime.toLocalDate().lengthOfMonth();
      if (todaysDayOfMonth == numberofDaysInMonth && todaysDayOfMonth < 31) {
        for (int i=todaysDayOfMonth+1; i<=31; i++) {
          daysToQuery.add(i);
        }
      }

      // since no reminders are sent on the weekend, include callers who
      // would have been called on those days.
      if (dayOfWeek.equals(DayOfWeek.MONDAY)) {
        daysToQuery.add(currentDateTime.minusDays(1).getDayOfMonth());
        daysToQuery.add(currentDateTime.minusDays(2).getDayOfMonth());
      }

      StringBuilder whereClause = new StringBuilder(" WHERE " + Reminder.DAY_OF_MONTH);
      if (daysToQuery.size() == 1) {
        whereClause.append(" = ").
            append(daysToQuery.iterator().next());
      }
      else {
        whereClause.append(" IN (");
        for (Integer day : daysToQuery) {
          whereClause.append(day).append(",");
        }
        whereClause.deleteCharAt(whereClause.length()-1);
        whereClause.append(")");
      }

      Connection conn = null;
      try {
        conn = SQLHelper.getInstance().getConnection();
        ResultSet rs = conn.createStatement().executeQuery(
            SQL_SELECT_CALLERS + whereClause.toString());
        Month currentMonth = currentDateTime.getMonth();
        int currentYear = currentDateTime.getYear();

        while (rs.next()) {
          Reminder reminder = new Reminder(rs);
          LocalDateTime lastReminderTime = (reminder.getLastReminderTimestamp() == null) ?
              null : reminder.getLastReminderTimestamp().toLocalDateTime();

          if (lastReminderTime == null ||
              lastReminderTime.getMonth() != currentMonth ||
              lastReminderTime.getYear() != currentYear) {

            Caller caller = new Caller(rs);
            if (!caller.isPaused()) {
              String trackingId = RandomStringUtils.randomAlphanumeric(24);
              if (sendReminder(caller, trackingId)) {
                updateReminderStatus(conn, caller.getCallerId(), trackingId);
              }
            }
          }
        }

        // todo: send second reminders where applicable
        /*
          1. Get list of callers whose call date is today - <secondReminderInterval>
          2. if first reminder sent but no call recorded, send second reminder.
         */
      }
      catch (Throwable e) {
        logger.severe("Reminder service failure: " + e.toString());
      }
      finally {
        if (conn != null) {
          try {
            conn.close();
          }
          catch (SQLException e) {
            logger.warning("Failed to close SQL connection: " + e.getMessage());
          }
        }
      }
    }


  }
}
