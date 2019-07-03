package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.Message;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

// For emailing super-admins about key events
public class EventAlertingService {

  private final static String EVENT_SERVICE_ADDRESS_PROP = "eventService.address";

  private static final Logger logger = Logger.getLogger(EventAlertingService.class.getName());

  private static EventAlertingService instance;

  // Not really a "Caller", actually contains the alert recipient address
  private Caller recipient;

  public static void init(Properties config) {
    assert (instance == null);
    instance = new EventAlertingService(config);
  }

  public static EventAlertingService getInstance() {
        return instance;
    }

  private EventAlertingService(Properties config) {
    logger.info("Init EventAlertingService");
    recipient = new Caller();
    recipient.setEmail(config.getProperty(EVENT_SERVICE_ADDRESS_PROP));
  }

  public void handleEvent(String name, String details) {
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