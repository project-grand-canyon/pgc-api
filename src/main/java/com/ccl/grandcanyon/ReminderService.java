package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReminderService {

  private final static String REMINDER_SERVICE_ENABLED = "reminderServiceEnabled";
  private final static String REMINDER_SERVICE_INTERVAL = "reminderServiceInterval";
  private final static String SECOND_REMINDER_INTERVAL = "secondReminderInterval";
  private final static String EARLIEST_REMINDER_TIME = "earliestReminderTime";
  private final static String LATEST_REMINDER_TIME = "latestReminderTime";
  private final static String SMS_DELIVERY_SERVICE = "smsDeliveryService";
  private final static String EMAIL_DELIVERY_SERVICE = "emailDeliveryService";

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

  private final static String SQL_SELECT_DISTRICT =
      "SELECT * FROM districts WHERE " +
          District.DISTRICT_ID + " = ?";


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

  private DeliveryService smsDeliveryService;
  private DeliveryService emailDeliveryService;

  private String regularCallInReminderHTML;
  private String callReminderEmailResource = "callNotificationEmail.html";

  private static int dayOfMonthCounter = 1;

  // TODO: Instead of distributing all callers across the month, do so for each district
  private static int getNewDayOfMonth() {
    int dayToReturn = dayOfMonthCounter;
    dayOfMonthCounter = dayOfMonthCounter == 31 ? 1 : dayOfMonthCounter + 1;
    return dayToReturn;
  }

  public static void init(Properties config) {
    assert(instance == null);
    instance = new ReminderService(config);
  }

  public static ReminderService getInstance() {
    return instance;
  }


  private ReminderService(Properties config) {

    logger.info("Init Reminder Service");

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

    try {
      this.smsDeliveryService = (DeliveryService)Class.forName(
          config.getProperty(SMS_DELIVERY_SERVICE)).newInstance();
      this.smsDeliveryService.init(config);
    }
    catch (Exception e) {
      logger.warning("Failed to initialize SMS delivery service: " + e.getMessage());
      this.smsDeliveryService = null;
    }

    try {
      this.emailDeliveryService = (DeliveryService)Class.forName(
          config.getProperty(EMAIL_DELIVERY_SERVICE)).newInstance();
      this.emailDeliveryService.init(config);
    }
    catch (Exception e) {
      logger.warning("Failed to initialize Email deliver service: " + e.getMessage());
      this.emailDeliveryService = null;
    }

    try {
      URL resource = getClass().getClassLoader().getResource(callReminderEmailResource);
      if (resource == null) {
        throw new FileNotFoundException("File '" + callReminderEmailResource + "' not found.");
      }
      File file = new File(resource.getFile());
      BufferedReader br = new BufferedReader(new FileReader(file));
      this.regularCallInReminderHTML = br.lines().collect(Collectors.joining());
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to load regular call-in notification email template: " + e.getLocalizedMessage());
    }


    if (Boolean.parseBoolean(config.getProperty(REMINDER_SERVICE_ENABLED))) {
      logger.info("Booting up the reminder task");
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

    PreparedStatement statement = conn.prepareStatement(SQL_INSERT_REMINDER);
    statement.setInt(1, callerId);
    statement.setInt(2, getNewDayOfMonth());
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



  boolean sendReminder(
      Caller caller,
      District district,
      String trackingId) {

    boolean smsReminderSent = false;
    boolean emailReminderSent = false;

    String callInPageUrl = "projectgrandcanyon.com/call/" + district.getState() + "/" + district.getNumber() +
        "?t=" + trackingId + "&c=" + caller.getCallerId();

    if (caller.getContactMethods().contains(ContactMethod.sms)) {

      Message reminderMessage = new Message();
      reminderMessage.setBody("It's your day to call Rep. " + district.getRepLastName() + ". http://" + callInPageUrl);
      try {
        smsReminderSent = smsDeliveryService.sendHtmlMessage(caller, reminderMessage);
        if (smsReminderSent) {
          logger.info(String.format("Sent SMS reminder to caller {id: %d name %s %s}.",
              caller.getCallerId(), caller.getFirstName(), caller.getLastName()));
        }
      }
      catch (Exception e) {
        logger.warning(String.format("Failed to send SMS to caller {id: %d name %s %s}: %s",
            caller.getCallerId(), caller.getFirstName(), caller.getLastName(), e.getMessage()));
        smsReminderSent = false;
      }
    }

    if (caller.getContactMethods().contains(ContactMethod.email)) {

      Message reminderMessage = new Message();
      reminderMessage.setSubject("It's your day to call!");
      reminderMessage.setBody(this.regularCallInReminderHTML.replaceAll("projectgrandcanyon.com/call/", callInPageUrl));
      try {
        emailReminderSent = emailDeliveryService.sendHtmlMessage(caller, reminderMessage);
        if (emailReminderSent) {
          logger.info(String.format("Sent email reminder to caller {id: %d, name %s %s}.",
              caller.getCallerId(), caller.getFirstName(), caller.getLastName()));
        }
      }
      catch (Exception e) {
        logger.warning(String.format("Failed to send email to caller {id: %d, name %s %s}: %s",
            caller.getCallerId(), caller.getFirstName(), caller.getLastName(), e.getMessage()));
        emailReminderSent = false;
      }
    }

    return smsReminderSent | emailReminderSent;
  }

  public DeliveryService getSmsDeliveryService() {
    return smsDeliveryService;
  }

  public DeliveryService getEmailDeliveryService() {
    return emailDeliveryService;
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

      logger.info("Running reminder sender");

      OffsetDateTime currentDateTime = OffsetDateTime.now();

      DayOfWeek dayOfWeek = currentDateTime.getDayOfWeek();
      if (dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) {
        logger.info("It's a weekend. Do nothing.");
        return;
      }

      OffsetTime currentTime = currentDateTime.toOffsetTime();
      if (currentTime.isBefore(earliestReminder) ||
          currentTime.isAfter(latestReminder)) {
        logger.info("It's after hours. Do nothing.");
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
      int sentCount = 0;
      try {
        conn = SQLHelper.getInstance().getConnection();
        String query = SQL_SELECT_CALLERS + whereClause.toString();
        logger.info(query);
        ResultSet rs = conn.createStatement().executeQuery(query);
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
            PreparedStatement statement = conn.prepareStatement(SQL_SELECT_DISTRICT);
            statement.setInt(1, caller.getDistrictId());
            ResultSet rs2 = statement.executeQuery();
            if (rs2.next()){
              District district = new District(rs2);
              if (!caller.isPaused()) {
                String trackingId = RandomStringUtils.randomAlphanumeric(8);
                if (sendReminder(caller, district, trackingId)) {
                  sentCount++;
                  updateReminderStatus(conn, caller.getCallerId(), trackingId);
                }
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
        logger.info(String.format("Sent %s reminders", sentCount));
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
