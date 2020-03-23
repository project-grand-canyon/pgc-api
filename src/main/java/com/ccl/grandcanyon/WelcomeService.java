package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.DayOfMonthFormatter;
import com.ccl.grandcanyon.utils.FileReader;

import javax.ws.rs.NotFoundException;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
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

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  public static void init(Properties config) {
    assert (instance == null);
    instance = new WelcomeService(config);
  }

  public void tearDown(){
    executorService.shutdown();
  }

  public static WelcomeService getInstance() {
        assert(instance != null);
        return instance;
    }

  private WelcomeService(Properties config) {
      logger.info("Init Welcome Service");

      try {
          this.welcomeHtml = FileReader.create().read(welcomeResource);
      } catch (Exception e) {
          throw new RuntimeException("Unable to load welcome email template: " + e.getLocalizedMessage());
      }
      try {
          this.welcomeHtmlCovid = FileReader.create().read(welcomeResourceCovid);
      } catch (Exception e) {
          throw new RuntimeException("Unable to load covid welcome email template: " + e.getLocalizedMessage());
      }
  }

  public void handleNewCaller(Caller caller) {
    logger.info("New Caller");
    // do this asynchronously so as not to delay response to end-user
    executorService.submit(() -> {
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
    try {
        ReminderService.getInstance().getSmsDeliveryService().sendTextMessage(caller, getWelcomeSMSMessage(reminder, district));
    }
    catch (Exception e) {
        logger.severe(String.format("Failed to send welcome SMS to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
    }
  }

  private Message getWelcomeSMSMessage(Reminder reminder, District district) {
      Message message = null;
      switch (district.getStatus()) {
          case active:
              message = getActiveWelcomeSMSMessage(reminder, district);
              break;
          case covid_paused:
              message = getCovidWelcomeSMSMessage(reminder);
              break;
      }
      return message;
  }

    private Message getActiveWelcomeSMSMessage(Reminder reminder, District district) {
        Message message = new Message();
        message.setBody(
            String.format(
                "You're signed up for the Monthly Calling Campaign. We have randomized your call to the %s of the month. Thanks for joining!\nWant to start calling now? Check out the current call-in guide at https://cclcalls.org/call/%s/%s",
                DayOfMonthFormatter.getAdjective(reminder.getDayOfMonth()),
                district.getState(),
                district.getNumber()
            )
        );
        return message;
    }

    private Message getCovidWelcomeSMSMessage(Reminder reminder) {
        Message message = new Message();
        message.setBody(
            String.format(
                "You're signed up for the Monthly Calling Campaign. We have randomized your call to the %s of the month. Thanks for joining!\nNOTE: the campaign is temporarily paused in order to give your Congressional Office time to respond to the COVID-19 crisis. Your notifications will begin when the crisis abates.",
                DayOfMonthFormatter.getAdjective(reminder.getDayOfMonth())
        ));
        return message;
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
