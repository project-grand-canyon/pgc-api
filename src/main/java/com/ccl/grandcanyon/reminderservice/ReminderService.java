//Change callFromEmail to true to execute new email format

package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.*;

import org.apache.commons.lang3.RandomStringUtils;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ReminderService {
  private final static String REMINDER_SERVICE_ENABLED = "reminderServiceEnabled";
  private final static String REMINDER_SERVICE_INTERVAL = "reminderServiceInterval";
  private final static String SECOND_REMINDER_INTERVAL = "secondReminderInterval";
  private final static String EARLIEST_REMINDER_TIME = "earliestReminderTime";
  private final static String LATEST_REMINDER_TIME = "latestReminderTime";
  private final static String STALE_SCRIPT_WARNING_INTERVAL = "staleScriptWarningInterval";
  
  private final static String SQL_SELECT_REMINDER = "SELECT * FROM reminders WHERE " + Reminder.TRACKING_ID + " = ?";
  
  private final static String SQL_INSERT_REMINDER = "INSERT into reminders (" + Reminder.CALLER_ID + ", "
      + Reminder.DAY_OF_MONTH + ") VALUES (?, ?)";
  
      private final static String SQL_UPDATE_REMINDER = "UPDATE reminders SET " + Reminder.LAST_REMINDER_TIMESTAMP
      + " = ?, " + Reminder.TRACKING_ID + " = ?, " + Reminder.REMINDER_YEAR + " = ?, " + Reminder.REMINDER_MONTH
      + " = ? " + "WHERE " + Reminder.CALLER_ID + " = ?";
  
      private final static String SQL_SELECT_REP_DISTRICTS = "SELECT * FROM districts WHERE district_number >= 0";
  
  private final static String SQL_SELECT_CALLERS = "SELECT r.*, c.*, ccm.contact_method, last_call_timestamp FROM reminders r "
      + "LEFT JOIN callers AS c ON c.caller_id = r.caller_id "
      + "LEFT JOIN callers_contact_methods AS ccm on c.caller_id = ccm.caller_id "
      + "LEFT JOIN (SELECT caller_id,  MAX(created) as last_call_timestamp FROM calls GROUP by caller_id) cls ON c.caller_id = cls.caller_id";

  private final static String SQL_INSERT_REMINDER_HISTORY = "INSERT into reminder_history (" + ReminderStatus.CALLER_ID
      + ", " + ReminderStatus.CALLER_DISTRICT_ID + ", " + ReminderStatus.TARGET_DISTRICT_ID + ", "
      + ReminderStatus.TIME_SENT + ", " + ReminderStatus.TRACKING_ID + ", " + ReminderStatus.EMAIL_DELIVERED + ", "
      + ReminderStatus.SMS_DELIVERED + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

  private final static String SQL_STALE_SCRIPT_QUERY = "SELECT d.*, a.admin_id, a.login_enabled, a.email from districts d "
      + "LEFT JOIN admins_districts as ad ON ad.district_id = d.district_id "
      + "LEFT JOIN admins as a ON a.admin_id = ad.admin_id";

  private final static String SQL_UPDATE_STALE_SCRIPT_NOTIFICATION = "UPDATE districts SET "
      + District.LAST_STALE_SCRIPT_NOTIFICATION + " = ? " + "WHERE " + District.DISTRICT_ID + " = ?";

  private static final Logger logger = Logger.getLogger(ReminderService.class.getName());

  private static ReminderService instance;

  // frequency, in minutes, that the service wakes up to send reminders
  private int serviceIntervalMinutes;

  private LocalTime earliestReminder;
  private LocalTime latestReminder;

  private ScheduledFuture reminderTask;

  private long staleScriptWarningInterval;

  private HolidayService holidayService;

  private EmailSender emailSender;

  private SmsSender smsSender;

  private static int dayOfMonthCounter = 1;

  private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private int secondReminderInterval;
  
  // TODO: Instead of distributing all callers across the month, do so for each
  // district
  private static int getNewDayOfMonth() {
    int dayToReturn = dayOfMonthCounter;
    dayOfMonthCounter = dayOfMonthCounter == ReminderDate.MAX_DAY ? ReminderDate.MIN_DAY : dayOfMonthCounter + 1;
    return dayToReturn;
  }

  public static void init(Properties config) {
    assert (instance == null);
    instance = new ReminderService(config);
  }

  public static ReminderService getInstance() {
    assert (instance != null);
    return instance;
  }

  private ReminderService(Properties config) {

    logger.info("Init Reminder Service");

    this.serviceIntervalMinutes = Integer.parseInt(config.getProperty(REMINDER_SERVICE_INTERVAL, "60"));
    this.secondReminderInterval = Integer.parseInt(config.getProperty(SECOND_REMINDER_INTERVAL, "4"));
    try {
      this.earliestReminder = LocalTime.parse(config.getProperty(EARLIEST_REMINDER_TIME));
    } catch (DateTimeParseException e) {
      logger.warning("Failed to parse property 'earliestReminderTime', using default of 09:00");
      this.earliestReminder = LocalTime.of(9, 0);
    }

    try {
      this.latestReminder = LocalTime.parse(config.getProperty(LATEST_REMINDER_TIME));
    } catch (DateTimeParseException e) {
      logger.warning("Failed to parse property 'latestReminderTime', using default of 18:00");
      this.latestReminder = LocalTime.of(18, 0, 0, 0);
    }

    try {
      this.holidayService = HolidayService.getInstance();
    } catch (Exception e) {
      logger.warning("Failed to initialize Holiday service: " + e.getMessage());
      this.holidayService = null;
    }

    try {
      this.emailSender = EmailSender.getInstance();
    } catch (Exception e) {
      logger.warning("Failed to initialize Email Sender: " + e.getMessage());
      this.emailSender = null;
    }

    
    try {
      this.smsSender = SmsSender.getInstance();
    } catch (Exception e) {
      logger.warning("Failed to initialize SMS Sender: " + e.getMessage());
      this.smsSender = null;
    }

    int staleScriptWarningInDays = Integer.parseInt(config.getProperty(STALE_SCRIPT_WARNING_INTERVAL, "30"));
    this.staleScriptWarningInterval = TimeUnit.DAYS.toMillis(staleScriptWarningInDays);

    if (Boolean.parseBoolean(config.getProperty(REMINDER_SERVICE_ENABLED))) {
      logger.info("Booting up the reminder task");
      // start the background task that will send reminders to callers
      this.reminderTask = executorService.scheduleAtFixedRate(new ReminderSender(), 10, serviceIntervalMinutes * 60,
          TimeUnit.SECONDS);
    }
  }

  public void tearDown() {
    if (reminderTask != null) {
      reminderTask.cancel(true);
    }
    executorService.shutdown();
  }

  public void createInitialReminder(Connection conn, int callerId) throws SQLException {

    PreparedStatement statement = conn.prepareStatement(SQL_INSERT_REMINDER);
    statement.setInt(1, callerId);
    statement.setInt(2, getNewDayOfMonth());
    statement.executeUpdate();
  }

  public Reminder getReminderByTrackingId(Connection conn, String trackingId) throws SQLException {

    PreparedStatement statement = conn.prepareStatement(SQL_SELECT_REMINDER);
    statement.setString(1, trackingId);
    ResultSet rs = statement.executeQuery();
    if (rs.next()) {
      return new Reminder(rs);
    }
    return null;
  }

  boolean sendReminder(Connection conn, Caller caller, ReminderDate reminderDate) throws SQLException {
    if (callerDistrict.getStatus() != Status.active) {
      logger.info("Skipping caller with id " + caller.getCallerId() + " because their district status is " + callerDistrict.getStatus().toString());
      return false;
    }

    District smsTarget = null;
    List<District> emailTargets = new ArrayList<District>();

    if (caller.getContactMethods().contains(ContactMethod.sms)) {
      smsTarget = smsSender.sendSmsReminder(conn, caller);
    }

    if (caller.getContactMethods().contains(ContactMethod.email)) {
      emailTargets = emailSender.sendEmailReminder(conn, caller);
    }

    for (District emailTarget : emailTargets) {
      if (smsTarget != null && emailTarget.getDistrictId() == smsTarget.getDistrictId()) {
        updateReminderStatus(conn, new ReminderStatus(caller, emailTarget, true, true, trackingId), reminderDate);
        smsTarget = null;
      }
      else {
        updateReminderStatus(conn, new ReminderStatus(caller, emailTarget, false, true, trackingId), reminderDate);
      }
    }

    if (smsTarget != null) {
      updateReminderStatus(conn, new ReminderStatus(caller, smsTarget, true, false, trackingId), reminderDate);
    }
  }

  private void updateReminderStatus(Connection conn, ReminderStatus reminderStatus, ReminderDate reminderDate)
      throws SQLException {

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    PreparedStatement update = conn.prepareStatement(SQL_UPDATE_REMINDER);
    int idx = 1;
    update.setTimestamp(idx++, timestamp);
    update.setString(idx++, reminderStatus.getTrackingId());
    update.setInt(idx++, reminderDate.getYear());
    update.setInt(idx++, reminderDate.getMonth());
    update.setInt(idx, reminderStatus.getCaller().getCallerId());
    update.executeUpdate();

    // add history record
    PreparedStatement history = conn.prepareStatement(SQL_INSERT_REMINDER_HISTORY);
    idx = 1;
    history.setInt(idx++, reminderStatus.getCaller().getCallerId());
    history.setInt(idx++, reminderStatus.getCaller().getDistrictId());
    history.setInt(idx++, reminderStatus.getTargetDistrict().getDistrictId());
    history.setTimestamp(idx++, timestamp);
    history.setString(idx++, reminderStatus.getTrackingId());
    history.setBoolean(idx++, reminderStatus.getEmailDelivered());
    history.setBoolean(idx, reminderStatus.getSmsDelivered());
    history.executeUpdate();
  }
  
  class ReminderSender implements Runnable {

    @Override
    public void run() {

      logger.info("Waking up Reminder Sender");

      Connection conn = null;
      try {
        conn = SQLHelper.getInstance().getConnection();
        checkForStaleScripts(conn);
        String query = SQL_SELECT_REP_DISTRICTS;
        logger.info(query);
        ResultSet rs = conn.createStatement().executeQuery(query);
        while (rs.next()) {
          District district = new District(rs);
          run(district, conn);
        }
      } catch (Throwable e) {
        logger.severe("Reminder service select districts failure: " + e.toString());
      } finally {
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e) {
            logger.warning("Failed to close SQL connection during reminder check: " + e.getMessage());
          }
        }
      }
    }

    private void run(District district, Connection conn) throws SQLException {

      logger.info("Waking up reminder sender for " + district.readableName());

      ZoneId timezoneId = ZoneId.of(district.getTimeZone());
      LocalDateTime currentDateTime = LocalDateTime.now(timezoneId);
      logger.info("Time in " + district.readableName() + ": " + currentDateTime + ". (Sending window:"
          + earliestReminder + "-" + latestReminder + ")");

      DayOfWeek dayOfWeek = currentDateTime.getDayOfWeek();
      if (dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) {
        logger.info("It's a weekend. Do nothing.");
        return;
      }

      holidayService.refresh();
      if (holidayService.isHoliday(currentDateTime.toLocalDate())) {
        logger.info("It's a holiday. Do nothing.");
        return;
      }

      LocalTime currentTime = currentDateTime.toLocalTime();
      if (currentTime.isBefore(earliestReminder) || currentTime.isAfter(latestReminder)) {
        logger.info("It's after hours in " + district.readableName() + ". Do nothing.");
        return;
      }

      logger.info("The sending window is open for " + district.readableName());

      // In order to select all callers who might get a reminder today, first
      // determine what days of the month are in play. Today is always included.
      Set<ReminderDate> datesToQuery = new HashSet<>();
      datesToQuery.add(new ReminderDate(currentDateTime.toLocalDate()));

      Set<ReminderDate> missedDates = getMissedDaysBefore(currentDateTime.toLocalDate());
      datesToQuery.addAll(missedDates);

      StringBuilder whereClause = new StringBuilder(" WHERE " + Reminder.DAY_OF_MONTH);
      if (datesToQuery.size() == 1) {
        whereClause.append(" = ").append(datesToQuery.iterator().next().getDay());
      } else {
        whereClause.append(" IN (");
        for (ReminderDate date : datesToQuery) {
          whereClause.append(date.getDay()).append(",");
        }
        whereClause.deleteCharAt(whereClause.length() - 1);
        whereClause.append(")");
      }

      whereClause.append(" AND c." + Caller.DISTRICT_ID + " = " + district.getDistrictId());

      int sentCount = 0;

      String query = SQL_SELECT_CALLERS + whereClause.toString();
      logger.info(query);
      ResultSet rs = conn.createStatement().executeQuery(query);
      while (rs.next()) {
        logger.info("Sending for " + new Caller(rs).getCallerId());
        Reminder reminder = new Reminder(rs);
        ReminderDate correspondingReminderDate = getCorrespondingReminderDate(reminder, datesToQuery);
        if (correspondingReminderDate != null && !reminder.hasBeenSent(correspondingReminderDate)) {
          Caller caller = new Caller(rs);
          if (!caller.isPaused()) {
            ReminderStatus reminderStatus = sendReminder(conn, caller, correspondingReminderDate);
            if (reminderStatus != null) {
              sentCount++;
            }
          }
        }
      }
    }

    // Assumes number of missed days < 1 month
    private Set<ReminderDate> getMissedDaysBefore(LocalDate startingDate) {
      Set<ReminderDate> missedDays = new HashSet<>();
      LocalDate date = startingDate.minusDays(1);
      // Go back 1 day at a time and add it to `missedDays` if the day isn't a
      // valid call day e.g. weekend, holiday, etc.
      for (; !isValidCallDate(date); date = date.minusDays(1)) {
        missedDays.add(new ReminderDate(date));
      }
      // If we have gone back to a previous month and that previous month is shorter
      // than 31 days,
      // add all days up to and including day 31
      if (!date.getMonth().equals(startingDate.getMonth())) {
        for (int i = date.lengthOfMonth() + 1; i <= ReminderDate.MAX_DAY; i++) {
          ReminderDate reminderDate = new ReminderDate.Builder().year(date.getYear()).month(date.getMonth()).day(i)
              .build();
          missedDays.add(reminderDate);
        }
      }
      return missedDays;
    }

    private Boolean isValidCallDate(LocalDate date) {
      DayOfWeek dayOfWeek = date.getDayOfWeek();
      return !dayOfWeek.equals(DayOfWeek.SATURDAY) && !dayOfWeek.equals(DayOfWeek.SUNDAY)
          && !holidayService.isHoliday(date);
    }

    // Assumes unique day of month across all ReminderDates
    private ReminderDate getCorrespondingReminderDate(Reminder reminder, Collection<ReminderDate> reminderDates) {
      for (ReminderDate reminderDate : reminderDates) {
        if (reminder.getDayOfMonth() == reminderDate.getDay()) {
          return reminderDate;
        }
      }
      logger.warning("no matching ReminderDate found for reminder");
      return null;
    }

    private void checkForStaleScripts(Connection conn) {
      Timestamp staleTime = new Timestamp(System.currentTimeMillis() - staleScriptWarningInterval);
      Timestamp now = new Timestamp(System.currentTimeMillis());
      try {
        ResultSet rs = conn.createStatement().executeQuery(SQL_STALE_SCRIPT_QUERY);
        while (rs.next()) {
          District district = new District(rs);
          // send a notification if it's been > N days since script was modified and it's
          // been greater than
          // N days since the last time we sent a notification.
          if (district.needsStaleScriptNotification(staleTime)) {
            if (rs.getBoolean(Admin.LOGIN_ENABLED)) {
              if (emailSender.sendStaleScrptNotification(district, rs.getString(Admin.EMAIL))) {
                PreparedStatement update = conn.prepareStatement(SQL_UPDATE_STALE_SCRIPT_NOTIFICATION);
                int idx = 1;
                update.setTimestamp(idx++, now);
                update.setInt(idx, district.getDistrictId());
                update.executeUpdate();
              }
            } else {
              logger.warning(String.format(
                  "Could not send stale script warning to Admin for %s district %d:  Admin account not enabled.",
                  district.getState(), district.getNumber()));
            }
          }
        }
      } catch (Throwable e) {
        logger.severe("Unexpected error checking for stale scripts: " + e.toString());
      }
    }
  }

}
