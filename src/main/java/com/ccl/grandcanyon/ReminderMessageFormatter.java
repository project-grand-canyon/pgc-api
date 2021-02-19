package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.FileReader;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class ReminderMessageFormatter {
    private final static boolean callFromEmail = false;
    
    private final static String APPLICATION_BASE_URL = "applicationBaseUrl";
    private final static String ADMIN_APPLICATION_BASE_URL = "adminApplicationBaseUrl";

    private static final Logger logger = Logger.getLogger(ReminderMessageFormatter.class.getName());

    private static ReminderMessageFormatter instance; 

    private String regularCallInReminderHTML;
    private String callReminderEmailResource = "callNotificationEmail.html";
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

    private ReminderMessageFormatter(Properties config){
        this.applicationBaseUrl = config.getProperty(APPLICATION_BASE_URL);
        this.adminApplicationBaseUrl = config.getProperty(ADMIN_APPLICATION_BASE_URL);
        
        try {
            this.regularCallInReminderHTML = FileReader.create().read(callFromEmail ? callGuideEmailResource : callReminderEmailResource);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load regular call-in notification email template: " + e.getLocalizedMessage());
        }
      
        try {
            this.callGuideCallBlockHTML = FileReader.create().read(callGuideEmailCallBlockResource);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load call guide block email template: " + e.getLocalizedMessage());
        }

        try {
            this.staleScriptHTML = FileReader.create().read(staleScriptEmailResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load stale script email template: " + e.getLocalizedMessage());
        }
    }
    
    private List<DistrictHydrated> newGetDistrictToCall(
            District callerDistrict) {
        List<DistrictHydrated> districtsToCall = new LinkedList<DistrictHydrated>();
        List<CallTarget> targets = callerDistrict.getCallTargets();
        assert (targets != null && !targets.isEmpty());
        while (!targets.isEmpty()) {
            Integer targetDistrictId = 0;
            Integer totalProbability = 0;
            for (CallTarget target : targets){
                totalProbability += target.getPercentage();
            }
            if (totalProbability != 0) {
                int random = new Random().nextInt(totalProbability);
                int sum = 0;
                for (CallTarget target : targets) {
                    sum += target.getPercentage();
                    if (targetDistrictId == 0 && random < sum) {
                        targetDistrictId = target.getTargetDistrictId();
                        targets.remove(target);
                    }
                }
            }
            else {
                CallTarget target = targets.get(new Random().nextInt(targets.size()));
                targetDistrictId = target.getTargetDistrictId();
                targets.remove(target);
            }

            if (targetDistrictId == 0) {
                logger.severe(String.format("District %d has invalid call target set",
                        callerDistrict.getDistrictId()));
                targetDistrictId = callerDistrict.getDistrictId();
            }
            districtsToCall.add(ReminderSQLFetcher.getDistrictHydratedById(targetDistrictId));
      
        }
        //TODO If the logger.severe error occurs there is the potential for duplicate districts maybe add a contingency plan?
        return districtsToCall;
    }

    private District getDistrictToCall(
            Caller caller) {
        District callerDistrict = ReminderSQLFetcher.getDistrictById(caller.getDistrictId());
        int targetDistrictId = 0;
        int random = new Random().nextInt(100);
        int sum = 0;
        List<CallTarget> targets = callerDistrict.getCallTargets();
        assert(targets != null && !targets.isEmpty());

        for (CallTarget target : targets) {
            sum += target.getPercentage();
            if (random < sum) {
                targetDistrictId = target.getTargetDistrictId();
                break;
            }
        }
        if (targetDistrictId == 0) {
            logger.severe(String.format("District %d has invalid call target set with sum = %d",
                    callerDistrict.getDistrictId(), sum));
            targetDistrictId = callerDistrict.getDistrictId();
        }
        return (targetDistrictId == callerDistrict.getDistrictId()) ?
            callerDistrict :
            ReminderSQLFetcher.getDistrictById(targetDistrictId);
    }

    private List<String> getPhoneNumbersByDistrict(List<DistrictHydrated> targetDistricts) {
        List<String> phoneNumbers = new LinkedList<String>();
        for (DistrictHydrated target: targetDistricts) {
          List<DistrictOffice> offices = target.getOffices();
          Integer numberOfOffices = offices.size();
          if (numberOfOffices == 0) {
            phoneNumbers.add("Number Not Found");
          }
          else {
            String number = offices.get(0).getPhone();
            for (DistrictOffice office : offices) {
              if (office.getAddress().getState() == "DC"){
                number = office.getPhone();
              }
            }
            phoneNumbers.add(number);
          }
        }
        return phoneNumbers;
      }    

    private String makeCallInReminderReplacements(List<DistrictHydrated> targetDistricts, Caller caller, String trackingPackage, String email) {
        String rootPath = adminApplicationBaseUrl + "/call/";
        List<String> phoneNumbers = getPhoneNumbersByDistrict(targetDistricts);
        email.replaceAll("{IInvited}", rootPath + "invite" + trackingPackage); //TODO MAKE EXTENSION
        // TODO: Figure out how to handle Puerto Rico
        Integer size = targetDistricts.size();
        String callBlock = "";
        for (Integer i = 0; i < size; ++i) {
            callBlock += this.callGuideCallBlockHTML;
            callBlock.replaceAll("{CallNumber}", i.toString());
            DistrictHydrated targetDistrict = targetDistricts.get(i);
            callBlock.replaceAll("{MOCName}", targetDistrict.readableName());
            callBlock.replaceAll("{MOCDistrict}", targetDistrict.isSenatorDistrict() ? targetDistrict.getState() : targetDistrict.getState() + " District " + String.valueOf(targetDistrict.getNumber()));
            callBlock.replaceAll("{MOCNumber}", phoneNumbers.get(i));
            callBlock.replaceAll("{MOCRequestScript}", targetDistrict.getRequests().get(0).getContent());
            callBlock.replaceAll("{MOCGuide}", rootPath + targetDistrict.getState() + "/" + targetDistrict.getNumber() + trackingPackage);
            callBlock.replaceAll("{MOCICalled}", rootPath + "thankyou" + trackingPackage);
        }
        email.replaceAll("{CallBlock}", callBlock);
        email.replaceAll("{CallerName}", caller.getFirstName() + " " + caller.getLastName());
        return email;
    }
    
    public Message getReminderEmail(Caller caller, District callerDistrict, String trackingPackage) {
        Message reminderMessage = new Message();
        reminderMessage.setSubject("It's time to call about climate change");
        if(callFromEmail) {
            List<DistrictHydrated> targetDistricts = newGetDistrictToCall(callerDistrict);
            reminderMessage.setBody(
                    makeCallInReminderReplacements(targetDistricts, caller, trackingPackage, this.regularCallInReminderHTML));
            for (DistrictHydrated target: targetDistricts){
                reminderMessage.addTargetDistrict(ReminderSQLFetcher.getDistrictById(target.getDistrictId()));
            }
            return reminderMessage; 
        }
        else {
            String callInPageUrl = "http://" + applicationBaseUrl + "/call/";
            District targetDistrict = getDistrictToCall(caller);      
            String URL = callInPageUrl + targetDistrict.getState() + "/" +
                    targetDistrict.getNumber() + trackingPackage;
            reminderMessage.setBody(this.regularCallInReminderHTML.replaceAll("https://cclcalls.org/call/", URL));
            reminderMessage.addTargetDistrict(targetDistrict);
            return reminderMessage;
        }
    }

    public Message getAdminReminderEmail(District district, String date){
        Message message = new Message();
        message.setSubject("Your district call-in script may need updating.");
        // todo: replace with HTML? Add a link to admin portal?
        message.setBody(district.getScriptModifiedTime() == null
                ? String.format("The call in script for %s district %d has not yet been created.", district.getState(),
                    district.getNumber())
                : staleScriptHTML.replace("$district$", district.readableName()).replace("$updateDate$",
                    date));
        return message;
    }

    public Message getSMS(Caller caller, String trackingPackage) {
        String callInPageUrl = "http://" + applicationBaseUrl + "/call/";
        Message reminderMessage = new Message();
        District targetDistrict = getDistrictToCall(caller);      
        String URL = callInPageUrl + targetDistrict.getState() + "/" +
                targetDistrict.getNumber() + trackingPackage;
        String legislatorTitle = targetDistrict.getNumber() >= 0 ? "Rep." : "Senator";
        reminderMessage.setBody("It's your day to call " + legislatorTitle + " " + targetDistrict.getRepLastName() +
                ". " + URL);
        reminderMessage.addTargetDistrict(targetDistrict);
        return reminderMessage;
    }

    public String getApplicationBaseURL() {
        return applicationBaseUrl;
    }

    public String getAdminApplicationBaseURL() {
        return adminApplicationBaseUrl;
    }
}

