//Change callFromEmail to true to execute new email format

package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.ReminderMessageFormatter;
import com.ccl.grandcanyon.ReminderSQLFetcher;

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
  private final static String SMS_DELIVERY_SERVICE = "smsDeliveryService";
  private final static String EMAIL_DELIVERY_SERVICE = "emailDeliveryService";
  private final static String STALE_SCRIPT_WARNING_INTERVAL = "staleScriptWarningInterval";

  private static final Logger logger = Logger.getLogger(ReminderService.class.getName());

  private static ReminderService instance;

  // frequency, in minutes, that the service wakes up to send reminders
  private int serviceIntervalMinutes;

  private LocalTime earliestReminder;
  private LocalTime latestReminder;

  private ScheduledFuture reminderTask;

  private DeliveryService smsDeliveryService;
  private DeliveryService emailDeliveryService;

  private long staleScriptWarningInterval;

  private HolidayService holidayService;

  private static int dayOfMonthCounter = 1;

  private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private int secondReminderInterval;

  // TODO: Instead of distributing all callers across the month, do so for each
  // district
  public static int getNewDayOfMonth() {
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
      this.smsDeliveryService = (DeliveryService) Class.forName(config.getProperty(SMS_DELIVERY_SERVICE))
          .getDeclaredConstructor().newInstance();
      this.smsDeliveryService.init(config);
    } catch (Exception e) {
      logger.warning("Failed to initialize SMS delivery service: " + e.getMessage());
      this.smsDeliveryService = null;
    }

    try {
      this.emailDeliveryService = (DeliveryService) Class.forName(config.getProperty(EMAIL_DELIVERY_SERVICE))
          .getDeclaredConstructor().newInstance();
      this.emailDeliveryService.init(config);
    } catch (Exception e) {
      logger.warning("Failed to initialize Email deliver service: " + e.getMessage());
      this.emailDeliveryService = null;
    }

    try {
      this.holidayService = HolidayService.getInstance();
    } catch (Exception e) {
      logger.warning("Failed to initialize Holiday service: " + e.getMessage());
      this.holidayService = null;
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

    if (emailDeliveryService != null) {
      emailDeliveryService.tearDown();
    }

    if (smsDeliveryService != null) {
      smsDeliveryService.tearDown();
    }
  }

  boolean sendReminder(Caller caller, ReminderDate reminderDate) {
    ReminderMessageFormatter reminderMessageFormatter = ReminderMessageFormatter.getInstance();
    District callerDistrict = null;
    ReminderSQLFetcher fetcher = new ReminderSQLFetcher();
    try {
      callerDistrict = fetcher.getCallerDistrict(caller);
      if (callerDistrict.getStatus() != Status.active) {
        logger.info("Skipping caller with id " + caller.getCallerId() + " because their district status is "
            + callerDistrict.getStatus().toString());
        return false;
      }
    } catch (Exception e) {
      logger.severe(String.format("Failed to fetch caller district for caller: %d", caller.getDistrictId()));
    }
    String trackingId = RandomStringUtils.randomAlphanumeric(8);
    String trackingPackage = "?t=" + trackingId + "&c=" + caller.getCallerId() + "&d=" + callerDistrict.getNumber();
    boolean smsSuccess = false;
    boolean emailSuccess = false;
    Message reminderMessage;
    List<DistrictHydrated> targetDistricts = newGetDistrictToCall(callerDistrict);
    if (caller.getContactMethods().contains(ContactMethod.sms)) {
      reminderMessage = reminderMessageFormatter.getSMS(targetDistricts.get(0), trackingPackage);
      try {
        if (smsDeliveryService.sendHtmlMessage(caller, reminderMessage)) {
          logger.info(String.format("Sent SMS reminder to caller {id: %d name %s %s}.", caller.getCallerId(),
              caller.getFirstName(), caller.getLastName()));
          smsSuccess = true;
        }
      } catch (Exception e) {
        logger.warning(String.format("Failed to send SMS to caller {id: %d name %s %s}: %s", caller.getCallerId(),
            caller.getFirstName(), caller.getLastName(), e.getMessage()));
      }
    }
    if (caller.getContactMethods().contains(ContactMethod.email)) {
      List<String> phoneNumbers = getPhoneNumbersByDistrict(targetDistricts);
      reminderMessage = reminderMessageFormatter.getReminderEmail(targetDistricts, phoneNumbers, caller,
          trackingPackage);
      try {
        if (emailDeliveryService.sendHtmlMessage(caller, reminderMessage)) {
          logger.info(String.format("Sent email reminder to caller {id: %d, name %s %s}.", caller.getCallerId(),
              caller.getFirstName(), caller.getLastName()));
          emailSuccess = true;
        }
      } catch (Exception e) {
        logger.warning(String.format("Failed to send email to caller {id: %d, name %s %s}: %s", caller.getCallerId(),
            caller.getFirstName(), caller.getLastName(), e.getMessage()));
      }
    }
    try {
      fetcher.updateReminderStatus(
          new ReminderStatus(caller, targetDistricts.get(0).getDistrictId(), smsSuccess, emailSuccess, trackingId),
          reminderDate);
    } catch (Exception e) {
      logger.warning(String.format("Failed to update Reminder Status on caller: %d", caller.getCallerId()));
    }
    return smsSuccess || emailSuccess;
  }

  public DeliveryService getSmsDeliveryService() {
    return smsDeliveryService;
  }

  public DeliveryService getEmailDeliveryService() {
    return emailDeliveryService;
  }

  private boolean sendStaleScrptNotification(District district, String adminEmail) {
    boolean success = false;
    if (adminEmail != null) {
      Message message = ReminderMessageFormatter.getInstance().getAdminReminderEmail(district,
          dateFormat.format(district.getScriptModifiedTime()));
      Caller adminAsCaller = new Caller();
      adminAsCaller.setEmail(adminEmail);
      try {
        success = emailDeliveryService.sendTextMessage(adminAsCaller, message);
      } catch (Exception e) {
        logger.warning("Failed to deliver stale script message to: " + adminAsCaller.getEmail());
      }
    }
    if (success) {
      logger.info(String.format("Sent stale script warning to %s for %s district %d", adminEmail, district.getState(),
          district.getNumber()));
    } else {
      logger.warning(String.format(
          "Could not send stale script warning to Admin for %s district %d.  Possibly invalid email address '%s'.",
          district.getState(), district.getNumber(), adminEmail));
    }
    return success;
  }

  private List<DistrictHydrated> newGetDistrictToCall(District callerDistrict) {
    List<DistrictHydrated> districtsToCall = new LinkedList<DistrictHydrated>();
    List<CallTarget> targets = callerDistrict.getCallTargets();
    assert (targets != null && !targets.isEmpty());
    ReminderSQLFetcher fetcher = new ReminderSQLFetcher();
    while (!targets.isEmpty()) {
      Integer targetDistrictId = 0;
      Integer totalProbability = 0;
      for (CallTarget target : targets) {
        totalProbability += target.getPercentage();
      }
      if (totalProbability != 0) {
        int random = new Random().nextInt(totalProbability);
        int sum = 0;
        for (CallTarget target : targets) {
          sum += target.getPercentage();
          if (targetDistrictId == 0 && random < sum) {
            targetDistrictId = target.getTargetDistrictId();
            targets.remove(target);
          }
        }
      } else {
        CallTarget target = targets.get(new Random().nextInt(targets.size()));
        targetDistrictId = target.getTargetDistrictId();
        targets.remove(target);
      }

      if (targetDistrictId == 0) {
        logger.severe(String.format("District %d has invalid call target set", callerDistrict.getDistrictId()));
        targetDistrictId = callerDistrict.getDistrictId();
      }
      districtsToCall.add(fetcher.getDistrictHydratedById(targetDistrictId));

    }
    return districtsToCall;
  }

  private List<String> getPhoneNumbersByDistrict(List<DistrictHydrated> targetDistricts) {
    List<String> phoneNumbers = new LinkedList<String>();
    for (DistrictHydrated target : targetDistricts) {
      List<DistrictOffice> offices = target.getOffices();
      Integer numberOfOffices = offices.size();
      if (numberOfOffices == 0) {
        phoneNumbers.add("Number Not Found");
      } else {
        String number = offices.get(0).getPhone();
        for (DistrictOffice office : offices) {
          if (office.getAddress().getState() == "DC") {
            number = office.getPhone();
          }
        }
        phoneNumbers.add(number);
      }
    }
    return phoneNumbers;
  }

  class ReminderSender implements Runnable {

    @Override
    public void run() {

      logger.info("Waking up Reminder Sender");
      try {
        checkForStaleScripts();
        ReminderSQLFetcher fetcher = new ReminderSQLFetcher();
        ResultSet rs = fetcher.getDistrictSet();
        while (rs.next()) {
          District district = new District(rs);
          run(district);
        }
      } catch (Throwable e) {
        logger.severe("Reminder service select districts failure: " + e.toString());
      }
    }

    private void run(District district) {
      logger.info("Waking up reminder sender for " + district.readableName());
      ReminderSQLFetcher fetcher = new ReminderSQLFetcher();
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

      try {
        ResultSet rs = fetcher.getCallerSet(datesToQuery, district);
        int sentCount = 0;
        while (rs.next()) {
          logger.info("Sending for " + new Caller(rs).getCallerId());
          Reminder reminder = new Reminder(rs);
          ReminderDate correspondingReminderDate = getCorrespondingReminderDate(reminder, datesToQuery);
          if (correspondingReminderDate != null && !reminder.hasBeenSent(correspondingReminderDate)) {
            Caller caller = new Caller(rs);
            if (!caller.isPaused()) {
              if (sendReminder(caller, correspondingReminderDate)) {
                sentCount++;
              }
            }
          }
        }
      } catch (Exception e) {
        logger.severe("Message Sender Failed: " + e.toString());
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

    private void checkForStaleScripts() {

      Timestamp staleTime = new Timestamp(System.currentTimeMillis() - staleScriptWarningInterval);
      try {
        ReminderSQLFetcher fetcher = new ReminderSQLFetcher();
        ResultSet rs = fetcher.getStaleScriptSet();
        while (rs.next()) {
          District district = new District(rs);
          // send a notification if it's been > N days since script was modified and it's
          // been greater than
          // N days since the last time we sent a notification.
          if (district.needsStaleScriptNotification(staleTime)) {
            if (rs.getBoolean(Admin.LOGIN_ENABLED)) {
              if (sendStaleScrptNotification(district, rs.getString(Admin.EMAIL))) {
                fetcher.updateStaleScript(district);
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
