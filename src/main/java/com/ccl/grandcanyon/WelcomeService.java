package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ContactMethod;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class WelcomeService {

    private final static String SMS_DELIVERY_SERVICE = "smsDeliveryService";
    private final static String EMAIL_DELIVERY_SERVICE = "emailDeliveryService";

    private static final Logger logger = Logger.getLogger(WelcomeService.class.getName());

    private static WelcomeService instance;

    private DeliveryService smsDeliveryService;
    private DeliveryService emailDeliveryService;


    public static void init(Properties config) {
        assert (instance == null);
        instance = new WelcomeService(config);
    }

    public static WelcomeService getInstance() {
        return instance;
    }

    private WelcomeService(Properties config) {

        logger.info("Init Welcome Service");

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                this.smsDeliveryService = (DeliveryService) Class.forName(
                        config.getProperty(SMS_DELIVERY_SERVICE)).newInstance();
                this.smsDeliveryService.init(config);
            } catch (Exception e) {
                logger.warning("Failed to initialize SMS delivery service: " + e.getMessage());
                this.smsDeliveryService = null;
            }

            try {
                this.emailDeliveryService = (DeliveryService) Class.forName(
                        config.getProperty(EMAIL_DELIVERY_SERVICE)).newInstance();
                this.emailDeliveryService.init(config);
            } catch (Exception e) {
                logger.warning("Failed to initialize Email deliver service: " + e.getMessage());
                this.emailDeliveryService = null;
            }
        });
    }

    public void handleNewCaller(Caller caller) {
        logger.info("New Caller");
        if (caller.getContactMethods().contains(ContactMethod.sms)){
            try {
                this.smsDeliveryService.sendWelcomeMessage(caller);
            } catch (Exception e) {
                logger.severe(String.format("Failed to send welcome SMS to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
            }
        }

        if (caller.getContactMethods().contains(ContactMethod.email)){
            try {
                this.emailDeliveryService.sendWelcomeMessage(caller);
            } catch (Exception e) {
                logger.severe(String.format("Failed to send welcome email to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
            }
        }

    }
}