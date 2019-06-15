package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ContactMethod;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

// For emailing super-admins about key events
public class EventAlertingService {

    private final static String EMAIL_DELIVERY_SERVICE = "emailDeliveryService";

    private static final Logger logger = Logger.getLogger(EventAlertingService.class.getName());

    private static EventAlertingService instance;

    private DeliveryService emailDeliveryService;

    public static void init(Properties config) {
        assert (instance == null);
        instance = new EventAlertingService(config);
    }

    public static EventAlertingService getInstance() {
        return instance;
    }

    private EventAlertingService(Properties config) {

        logger.info("Init EventAlertingService");

        try {
            this.emailDeliveryService = (DeliveryService) Class.forName(
                    config.getProperty(EMAIL_DELIVERY_SERVICE)).newInstance();
            this.emailDeliveryService.init(config);
        } catch (Exception e) {
            logger.warning("Failed to initialize Email deliver service: " + e.getMessage());
            this.emailDeliveryService = null;
        }

    }

    public void handleEvent(String name, String details) {
        logger.info("New event: " + name);
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                this.emailDeliveryService.sendEventAlert(name, details);
            } catch (Exception e) {
                logger.severe(String.format("Failed to send welcome email to event: %s. Err: %s", name, e.getMessage()));
            }
        });
    }
}