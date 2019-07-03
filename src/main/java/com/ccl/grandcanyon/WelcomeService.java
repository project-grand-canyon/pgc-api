package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ContactMethod;
import com.ccl.grandcanyon.types.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WelcomeService {

  private static final Logger logger = Logger.getLogger(WelcomeService.class.getName());

  private static WelcomeService instance;

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
        Message message = new Message();
        message.setBody("You're all signed up for Project Grand Canyon. Thanks for joining!");
        try {
          ReminderService.getInstance().getSmsDeliveryService().sendTextMessage(caller, message);
        }
        catch (Exception e) {
          logger.severe(String.format("Failed to send welcome SMS to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
        }
      }

      if (caller.getContactMethods().contains(ContactMethod.email)) {
        Message message = new Message();
        message.setSubject("Welcome to Project Grand Canyon!");
        message.setBody(welcomeHtml);
        try {
          ReminderService.getInstance().getEmailDeliveryService().sendHtmlMessage(caller, message);
        }
        catch (Exception e) {
          logger.severe(String.format("Failed to send welcome email to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
        }
      }

    });
  }
}
