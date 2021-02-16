package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.FileReader;

import org.apache.commons.lang3.RandomStringUtils;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SmsSender {
    private final static String SMS_DELIVERY_SERVICE = "smsDeliveryService";
    private final static String APPLICATION_BASE_URL = "applicationBaseUrl";

    private static final Logger logger = Logger.getLogger(SMSSender.class.getName());

    private static ReminderService instance;

    private DeliveryService smsDeliveryService;
    private String applicationBaseUrl;

    public static void init(Properties config) {
        assert (instance == null);
        instance = new SmsSender(config);
    }
    
    public static ReminderService getInstance() {
        assert (instance != null);
        return instance;
    }
    

    private SmsSender(Properties config){
        
        this.applicationBaseUrl = config.getProperty(APPLICATION_BASE_URL);
        
        try {
            this.smsDeliveryService = (DeliveryService) Class.forName(config.getProperty(EMAIL_DELIVERY_SERVICE))
                .getDeclaredConstructor().newInstance();
            this.smsDeliveryService.init(config);
        } catch (Exception e) {
            logger.warning("Failed to initialize Email deliver service: " + e.getMessage());
            this.smsDeliveryService = null;
        }
    }

    public void tearDown(){
        if (smsDeliveryService != null) {
            smsDeliveryService.tearDown();
        }
    }

    public String getApplicationBaseUrl() {
        return applicationBaseUrl;
    }

    public DeliveryService getSmsDeliveryService() {
        return smsDeliveryService;
    }

    private District sendSms(Connection conn, Caller caller) {
        District callerDistrict = Districts.retrieveDistrictById(conn, caller.getDistrictId());
        String trackingId = RandomStringUtils.randomAlphanumeric(8);  
        District targetDistrict = getDistrictToCall(conn, caller);
        String callInPageUrl = "http://" + applicationBaseUrl + "/call/";
        String trackingPackage = "?t=" + trackingId + "&c=" + caller.getCallerId() + "&d=" + callerDistrict.getNumber();
        Message reminderMessage = new Message();
        String legislatorTitle = targetDistrict.getNumber() >= 0 ? "Rep." : "Senator";

        reminderMessage.setBody("It's your day to call " + legislatorTitle + " " + targetDistrict.getRepLastName() +
                ". " + callInPageUrl + targetDistrict.getState() + "/" + targetDistrict.getNumber() + trackingPackage);
        
        try {
            boolean smsReminderSent = smsDeliveryService.sendHtmlMessage(caller, reminderMessage);
            if (smsReminderSent) {
                logger.info(String.format("Sent SMS reminder to caller {id: %d name %s %s}.",
                        caller.getCallerId(), caller.getFirstName(), caller.getLastName()));
                
            return targetDistrict;
            }
        }
        catch (Exception e) {
            logger.warning(String.format("Failed to send SMS to caller {id: %d name %s %s}: %s",
                    caller.getCallerId(), caller.getFirstName(), caller.getLastName(), e.getMessage()));
            return null;
        }
    }
}