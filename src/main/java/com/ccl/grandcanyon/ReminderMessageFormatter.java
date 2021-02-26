package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.FileReader;

import java.util.*;

public class ReminderMessageFormatter {
    private final static boolean callFromEmail = false;

    private final static String APPLICATION_BASE_URL = "applicationBaseUrl";
    private final static String ADMIN_APPLICATION_BASE_URL = "adminApplicationBaseUrl";

    private static ReminderMessageFormatter instance;

    private String regularCallInReminderHTML;
    private String callReminderEmailResource = "callNotificationEmail.html";

    private String callGuideReminderHTML;
    private String callGuideEmailResource = "callGuideEmail.html";

    private String callGuideCallBlockHTML;
    private String callGuideEmailCallBlockResource = "callGuideEmailCallBlock.html";

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

        try {
            this.regularCallInReminderHTML = FileReader.create().read(callReminderEmailResource);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load regular call-in notification email template: " + e.getLocalizedMessage());
        }

        try {
            this.callGuideReminderHTML = FileReader.create().read(callGuideEmailResource);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load regular call-in notification email template: " + e.getLocalizedMessage());
        }

        try {
            this.callGuideCallBlockHTML = FileReader.create().read(callGuideEmailCallBlockResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load call guide block email template: " + e.getLocalizedMessage());
        }

        try {
            this.staleScriptHTML = FileReader.create().read(staleScriptEmailResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load stale script email template: " + e.getLocalizedMessage());
        }
    }

    private String makeCallInReminderReplacements(List<DistrictHydrated> targetDistricts, List<String> phoneNumbers,
            Caller caller, String trackingPackage, String email) {
        String rootPath = adminApplicationBaseUrl + "/call/";
        email.replaceAll("{IInvited}", rootPath + "invite" + trackingPackage);
        Integer size = targetDistricts.size();
        String callBlock = "";
        for (Integer i = 0; i < size; ++i) {
            callBlock += this.callGuideCallBlockHTML;
            callBlock.replaceAll("{CallNumber}", i.toString());
            DistrictHydrated targetDistrict = targetDistricts.get(i);
            callBlock.replaceAll("{MOCName}", targetDistrict.readableName());
            callBlock.replaceAll("{MOCDistrict}", targetDistrict.isSenatorDistrict() ? targetDistrict.getState()
                    : targetDistrict.getState() + " District " + String.valueOf(targetDistrict.getNumber()));
            callBlock.replaceAll("{MOCNumber}", phoneNumbers.get(i));
            callBlock.replaceAll("{MOCRequestScript}", targetDistrict.getRequests().get(0).getContent());
            callBlock.replaceAll("{MOCGuide}",
                    rootPath + targetDistrict.getState() + "/" + targetDistrict.getNumber() + trackingPackage);
            callBlock.replaceAll("{MOCICalled}", rootPath + "thankyou" + trackingPackage);
        }
        email.replaceAll("{CallBlock}", callBlock);
        email.replaceAll("{CallerName}", caller.getFirstName() + " " + caller.getLastName());
        return email;
    }

    public Message getReminderEmail(List<DistrictHydrated> targetDistricts, List<String> phoneNumbers, Caller caller,
            String trackingPackage) {
        Message reminderMessage = new Message();
        reminderMessage.setSubject("It's time to call about climate change");
        if (callFromEmail) {
            reminderMessage.setBody(makeCallInReminderReplacements(targetDistricts, phoneNumbers, caller,
                    trackingPackage, this.callGuideReminderHTML));
            return reminderMessage;
        } else {
            String callInPageUrl = "http://" + applicationBaseUrl + "/call/";
            District targetDistrict = targetDistricts.get(0);
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

    public Message getSMS(District targetDistrict, String trackingPackage) {
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
}
