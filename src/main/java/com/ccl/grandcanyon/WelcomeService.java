package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.DayOfMonthFormatter;

import javax.ws.rs.NotFoundException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
      URL resource = getClass().getClassLoader().getResource(welcomeResource);
      if (resource == null) {
        throw new FileNotFoundException("File '" + welcomeResource + "' not found.");
      }
      File file = new File(resource.getFile());
      BufferedReader br = new BufferedReader(new FileReader(file));
      this.welcomeHtml = br.lines().collect(Collectors.joining());
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to load welcome email template: " + e.getLocalizedMessage());
    }

  }

  public void handleNewCaller(Caller caller) {
    logger.info("New Caller");
    // do this asynchronously so as not to delay response to end-user
    Executors.newSingleThreadExecutor().submit(() -> {
      if (caller.getContactMethods().contains(ContactMethod.sms)) {
        sendWelcomeSMS(caller);
      }
      if (caller.getContactMethods().contains(ContactMethod.email)) {
        sendWelcomeEmail(caller);
      }
    });
  }

  private void sendWelcomeSMS(Caller caller) {
    Message message = new Message();
    message.setBody("You're all signed up for Project Grand Canyon. Thanks for joining!");
    try {
      ReminderService.getInstance().getSmsDeliveryService().sendTextMessage(caller, message);
    }
    catch (Exception e) {
      logger.severe(String.format("Failed to send welcome SMS to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
    }
  }

  private void sendWelcomeEmail(Caller caller) {
    try (Connection conn = SQLHelper.getInstance().getConnection()) {
        PreparedStatement selectStatement = conn.prepareStatement(SQL_SELECT_CALL_IN_INFO);
        selectStatement.setInt(1, caller.getCallerId());
        ResultSet rs = selectStatement.executeQuery();
        if (!rs.next()) {
          throw new NotFoundException("No district or reminder found with caller ID '" + caller.getDistrictId() + "'");
        }
        District district = new District(rs);
        Reminder reminder = new Reminder(rs);
        Message message = new Message();
        message.setSubject("Welcome to Project Grand Canyon!");
        String personalizedHtml = this.welcomeHtml
                .replaceAll("\\{state\\}", district.getState())
                .replaceAll("\\{district_number\\}", String.valueOf(district.getNumber()))
                .replaceAll("\\{day_of_month\\}", DayOfMonthFormatter.getAdjective(reminder.getDayOfMonth()));
        message.setBody(personalizedHtml);
        ReminderService.getInstance().getEmailDeliveryService().sendHtmlMessage(caller, message);
    } catch (Exception e) {
      logger.severe(String.format("Failed to send welcome email to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
    }
  }
}
