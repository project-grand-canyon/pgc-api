package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.Message;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

// For emailing super-admins about key events
public class EventAlertingService {

  private final static String EVENT_SERVICE_ADDRESS_PROP = "eventService.address";
  private final static String EVENT_SERVICE_NEW_ADMIN_ADDRESS_PROP = "eventService.newAdminAddress";

  private static final Logger logger = Logger.getLogger(EventAlertingService.class.getName());

  private static EventAlertingService instance;

  // Not really a "Caller", actually contains the alert recipient address
  private Caller generalRecipient;
  private Caller newAdminRecipient;

  public static void init(Properties config) {
    assert (instance == null);
    instance = new EventAlertingService(config);
  }

  public static EventAlertingService getInstance() {
        return instance;
    }

  private EventAlertingService(Properties config) {
    logger.info("Init EventAlertingService");
    generalRecipient = new Caller();
    newAdminRecipient = new Caller();
    generalRecipient.setEmail(config.getProperty(EVENT_SERVICE_ADDRESS_PROP));
    newAdminRecipient.setEmail(config.getProperty(EVENT_SERVICE_NEW_ADMIN_ADDRESS_PROP));
  }

  public void handleEvent(String name, String details) {

    Caller recipient = name.equals("New Admin Sign Up") ? newAdminRecipient : generalRecipient;

    logger.info("New event: " + name);
    Executors.newSingleThreadExecutor().submit(() -> {
      try {
        Message alert = new Message();
        alert.setSubject(name);
        alert.setBody(details);
        ReminderService.getInstance().getEmailDeliveryService().sendTextMessage(recipient, alert);
      }
      catch (Exception e) {
        logger.severe(String.format("Failed to send email alert for event: %s. Err: %s", name, e.getMessage()));
      }
    });
  }
}