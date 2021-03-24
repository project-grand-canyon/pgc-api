package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.FileReader;
import java.util.logging.Logger;

import java.util.*;

public class ReminderMessageFormatter {
    private final static boolean callFromEmail = true;

    private final static String APPLICATION_BASE_URL = "applicationBaseUrl";
    private final static String ADMIN_APPLICATION_BASE_URL = "adminApplicationBaseUrl";

    private static final Logger logger = Logger.getLogger(ReminderMessageFormatter.class.getName());

    private static ReminderMessageFormatter instance;

    private String regularCallInReminderHTML;
    private String callReminderEmailResource = "callNotificationEmail.html";

    private String callGuideReminderHTML;
    private String callGuideEmailResource = "callGuideEmail.html";

    private String staleScriptHTML;
    private String staleScriptEmailResource = "staleScriptEmail.html";

    private String applicationBaseUrl;
    private String adminApplicationBaseUrl;

    public static void init(Properties config) {
        assert (instance == null);
        instance = new ReminderMessageFormatter(config);
    }

    public static ReminderMessageFormatter getInstance() {
        assert (instance != null);
        return instance;
    }

    private ReminderMessageFormatter(Properties config) {
        this.applicationBaseUrl = config.getProperty(APPLICATION_BASE_URL);
        this.adminApplicationBaseUrl = config.getProperty(ADMIN_APPLICATION_BASE_URL);
        logger.info("Initializing Reminder Message Formatter");
        try {
            this.regularCallInReminderHTML = FileReader.create().read(callReminderEmailResource);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load regular call-in notification email template: " + e.getLocalizedMessage());
        }

        try {
            this.callGuideReminderHTML = FileReader.create().read(callGuideEmailResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load call-in guide email template: " + e.getLocalizedMessage());
        }

        try {
            this.staleScriptHTML = FileReader.create().read(staleScriptEmailResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load stale script email template: " + e.getLocalizedMessage());
        }
    }

    private String makeCallInReminderReplacements(DistrictHydrated targetDistrict, String phoneNumber, Caller caller,
            String trackingPackage, String email) {
        String rootPath = adminApplicationBaseUrl + "/call/";
        email = email.replaceAll("fieldmocNumberfield", phoneNumber);
        email = email.replaceAll("fieldmocNamefield", (targetDistrict.isSenatorDistrict() ? "Senator " : "Representative ") +  targetDistrict.getRepFirstName() + " " + targetDistrict.getRepLastName());
        email = email.replaceAll("fieldaskfield", targetDistrict.getRequests().get(0).getContent());
        email = email.replaceAll("fieldthankYouUrlfield", rootPath + "thankyou" + trackingPackage);
        email = email.replaceAll("fieldcallerNamefield", caller.getFirstName() + " " + caller.getLastName());
        return email;
    }

    public Message getReminderEmail(DistrictHydrated targetDistrict, Caller caller, District callerDistrict,
            String trackingId) {
        Message reminderMessage = new Message();
        reminderMessage.setSubject("It's time to call about climate change");
        String phoneNumber = getPhoneNumbersByDistrict(targetDistrict);
        String trackingPackage = "?t=" + trackingId + "&c=" + caller.getCallerId() + "&d=" + callerDistrict.getNumber();
        if (callFromEmail) {
            reminderMessage.setBody(makeCallInReminderReplacements(targetDistrict, phoneNumber, caller, trackingPackage,
                    this.callGuideReminderHTML));
            return reminderMessage;
        } else {
            String callInPageUrl = "http://" + applicationBaseUrl + "/call/";
            String URL = callInPageUrl + targetDistrict.getState() + "/" + targetDistrict.getNumber() + trackingPackage;
            reminderMessage.setBody(this.regularCallInReminderHTML.replaceAll("https://cclcalls.org/call/", URL));
            return reminderMessage;
        }
    }

    public Message getAdminReminderEmail(District district, String date) {
        Message message = new Message();
        message.setSubject("Your district call-in script may need updating.");
        // todo: replace with HTML? Add a link to admin portal?
        message.setBody(district.getScriptModifiedTime() == null
                ? String.format("The call in script for %s district %d has not yet been created.", district.getState(),
                        district.getNumber())
                : staleScriptHTML.replace("$district$", district.readableName()).replace("$updateDate$", date));
        return message;
    }

    public Message getSMS(District targetDistrict, Caller caller, District callerDistrict, String trackingId) {
        String trackingPackage = "?t=" + trackingId + "&c=" + caller.getCallerId() + "&d=" + callerDistrict.getNumber();
        String callInPageUrl = "http://" + applicationBaseUrl + "/call/";
        Message reminderMessage = new Message();
        String URL = callInPageUrl + targetDistrict.getState() + "/" + targetDistrict.getNumber() + trackingPackage;
        String legislatorTitle = targetDistrict.getNumber() >= 0 ? "Rep." : "Senator";
        reminderMessage.setBody(
                "It's your day to call " + legislatorTitle + " " + targetDistrict.getRepLastName() + ". " + URL);
        return reminderMessage;
    }

    public String getApplicationBaseURL() {
        return applicationBaseUrl;
    }

    public String getAdminApplicationBaseURL() {
        return adminApplicationBaseUrl;
    }

    private String getPhoneNumbersByDistrict(DistrictHydrated targetDistrict) {
        String phoneNumber;
        List<DistrictOffice> offices = targetDistrict.getOffices();
        Integer numberOfOffices = offices.size();
        if (numberOfOffices == 0) {
            phoneNumber = "Number Not Found";
        } else {
            phoneNumber = offices.get(0).getPhone();
            for (DistrictOffice office : offices) {
                if (office.getAddress().getState() == "DC") {
                    phoneNumber = office.getPhone();
                }
            }
        }
        return phoneNumber;
    }
}
