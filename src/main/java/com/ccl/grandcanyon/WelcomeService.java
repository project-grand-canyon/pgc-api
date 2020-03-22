package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.DayOfMonthFormatter;
import com.ccl.grandcanyon.utils.FileReader;

import javax.ws.rs.NotFoundException;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class WelcomeService {

  private static final Logger logger = Logger.getLogger(WelcomeService.class.getName());

  private static WelcomeService instance;

  private static final String SQL_SELECT_CALL_IN_INFO =
          "SELECT d.*, c.*, r.* FROM districts AS d " +
          "JOIN callers AS c ON c.district_id = d.district_id " +
          "JOIN reminders AS r ON r.caller_id = c.caller_id " +
          "WHERE c.caller_id = ?";

  private String welcomeHtml;
  private String welcomeResource = "welcomeEmail.html";

  private String welcomeHtmlCovid;
  private String welcomeResourceCovid = "welcomeEmailCovid.html";

  public static void init(Properties config) {
    assert (instance == null);
    instance = new WelcomeService(config);
  }

  public static WelcomeService getInstance() {
        return instance;
    }

  private WelcomeService(Properties config) {

    logger.info("Init Welcome Service");

      try {
          this.welcomeHtml = FileReader.getInstance().read(welcomeResource);
      }
      catch (Exception e) {
          throw new RuntimeException("Unable to load welcome email template: " + e.getLocalizedMessage());
      }
      try {
          this.welcomeHtmlCovid = FileReader.getInstance().read(welcomeResourceCovid);
      }
      catch (Exception e) {
          throw new RuntimeException("Unable to load covid welcome email template: " + e.getLocalizedMessage());
      }

  }

  public void handleNewCaller(Caller caller) {
    logger.info("New Caller");
    // do this asynchronously so as not to delay response to end-user
    Executors.newSingleThreadExecutor().submit(() -> {
      try (Connection conn = SQLHelper.getInstance().getConnection()) {
        PreparedStatement selectStatement = conn.prepareStatement(SQL_SELECT_CALL_IN_INFO);
        selectStatement.setInt(1, caller.getCallerId());
        ResultSet rs = selectStatement.executeQuery();
        if (!rs.next()) {
          throw new NotFoundException("No district or reminder found with caller ID '" + caller.getDistrictId() + "'");
        }
        District district = new District(rs);
        Reminder reminder = new Reminder(rs);
        if (caller.getContactMethods().contains(ContactMethod.sms)) {
          sendWelcomeSMS(reminder, district, caller);
        }
        if (caller.getContactMethods().contains(ContactMethod.email)) {
          sendWelcomeEmail(reminder, district, caller);
        }
      } catch (Exception e) {
        logger.severe(String.format("Failed to send welcome messages: %s", e.getLocalizedMessage()));
      }
    });
  }

  private void sendWelcomeSMS(Reminder reminder, District district, Caller caller) {
    for (Message message: getWelcomeSMSMessages(reminder, district)) {
        try {
            ReminderService.getInstance().getSmsDeliveryService().sendTextMessage(caller, message);
        }
        catch (Exception e) {
            logger.severe(String.format("Failed to send welcome SMS to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
            break;
        }
    }
  }

  private List<Message> getWelcomeSMSMessages(Reminder reminder, District district) {
      List<Message> messages = null;
      switch (district.getStatus()) {
          case active:
              messages = getActiveWelcomeSMSMessages(reminder, district);
              break;
          case covid_paused:
              messages = getCovidWelcomeSMSMessages(reminder);
              break;
      }
      return messages;
  }

    private List<Message> getActiveWelcomeSMSMessages(Reminder reminder, District district) {
        Message message1 = new Message();
        message1.setBody(String.format("You're signed up for the Monthly Calling Campaign. We have randomized your call to the %s of the month. Thanks for joining!", DayOfMonthFormatter.getAdjective(reminder.getDayOfMonth())));
        Message message2 = new Message();
        message2.setBody(String.format("Want to start calling now? Check out the current call-in guide at https://cclcalls.org/call/%s/%s", district.getState(), district.getNumber()));
        return Arrays.asList(message1, message2);
    }

    private List<Message> getCovidWelcomeSMSMessages(Reminder reminder) {
        Message message1 = new Message();
        message1.setBody(String.format("You're signed up for the Monthly Calling Campaign. We have randomized your call to the %s of the month. Thanks for joining!", DayOfMonthFormatter.getAdjective(reminder.getDayOfMonth())));
        Message message2 = new Message();
        message2.setBody("NOTE: the campaign is temporarily paused in order to give your Congressional Office time to respond to the COVID-19 crisis. Your notifications will begin when the crisis abates.");
        return Arrays.asList(message1, message2);
    }

  private void sendWelcomeEmail(Reminder reminder, District district, Caller caller) {
    try {
      Message message = new Message();
      message.setSubject("Welcome to CCL's Monthly Calling Campaign!");
      message.setBody(getWelcomeEmailBody(reminder, district));
      ReminderService.getInstance().getEmailDeliveryService().sendHtmlMessage(caller, message);
    } catch (Exception e) {
      logger.severe(String.format("Failed to send welcome email to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
    }
  }

    private String getWelcomeEmailBody(Reminder reminder, District district) {
        String emailHtml = null;
        switch (district.getStatus()) {
            case active:
                emailHtml = this.welcomeHtml;
                break;
            case covid_paused:
                emailHtml = this.welcomeHtmlCovid;
                break;
        }
        return emailHtml
                .replaceAll("\\{state\\}", district.getState())
                .replaceAll("\\{district_number\\}", String.valueOf(district.getNumber()))
                .replaceAll("\\{day_of_month\\}", DayOfMonthFormatter.getAdjective(reminder.getDayOfMonth()));
    }
}
