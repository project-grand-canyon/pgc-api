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

public class EmailSenderService {
    private final static boolean callFromEmail = false;
    private final static String EMAIL_DELIVERY_SERVICE = "emailDeliveryService";
    private final static String APPLICATION_BASE_URL = "applicationBaseUrl";
    private final static String ADMIN_APPLICATION_BASE_URL = "adminApplicationBaseUrl";

    private static final Logger logger = Logger.getLogger(EmailSenderService.class.getName());

    private static EmailSenderService instance;

    private DeliveryService emailDeliveryService;
    private String applicationBaseUrl;
    private String adminApplicationBaseUrl;

    private String regularCallInReminderHTML;
    private String callReminderEmailResource = "callNotificationEmail.html";
  
    private String callFromReminderEmailResource = "newCallNotificationEmail.html";
  
    private String staleScriptHTML;
    private String staleScriptEmailResource = "staleScriptEmail.html";
  
    public static void init(Properties config) {
        assert (instance == null);
        instance = new EmailSenderService(config);
    }
    
    public static EmailSenderService getInstance() {
        assert (instance != null);
        return instance;
    }
    

    private EmailSenderService(Properties config){
        
        this.applicationBaseUrl = config.getProperty(APPLICATION_BASE_URL);
        this.adminApplicationBaseUrl = config.getProperty(ADMIN_APPLICATION_BASE_URL);

        try {
            this.emailDeliveryService = (DeliveryService) Class.forName(config.getProperty(EMAIL_DELIVERY_SERVICE))
                .getDeclaredConstructor().newInstance();
            this.emailDeliveryService.init(config);
        } catch (Exception e) {
            logger.warning("Failed to initialize Email deliver service: " + e.getMessage());
            this.emailDeliveryService = null;
        }

        try {
            if (callFromEmail){
                this.regularCallInReminderHTML = FileReader.create().read(callFromReminderEmailResource);
            } else {
                this.regularCallInReminderHTML = FileReader.create().read(callReminderEmailResource);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Unable to load regular call-in notification email template: " + e.getLocalizedMessage());
        }
        
        try {
            this.staleScriptHTML = FileReader.create().read(staleScriptEmailResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load stale script email template: " + e.getLocalizedMessage());
        }
    }

    public void tearDown(){
        if (emailDeliveryService != null) {
            emailDeliveryService.tearDown();
        }
    }

    public String getApplicationBaseUrl() {
        return applicationBaseUrl;
    }

    public String getAdminApplicationBaseUrl() {
        return adminApplicationBaseUrl;
    }

    public DeliveryService getEmailDeliveryService() {
        return emailDeliveryService;
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
        String rootPath = applicationBaseUrl + "/call/";
        List<String> phoneNumbers = getPhoneNumbersByDistrict(targetDistricts);
        email.replaceAll("{CallerName}", caller.getFirstName() + " " + caller.getLastName());
        email.replaceAll("{IInvited}", rootPath + "invite" + trackingPackage); //TODO MAKE EXTENSION
        Integer size = targetDistricts.size();
        for (Integer i = 0; i < size; ++i) {
            DistrictHydrated targetDistrict = targetDistricts.get(i);
            String MOC = "{MOC" + String.valueOf(i + 1);
            String title = targetDistrict.isSenatorDistrict() ? "Senator " : "Representative ";
            email.replaceAll(MOC + "Name}", title + targetDistrict.getRepFirstName() + " " + targetDistrict.getRepLastName());
            email.replaceAll(MOC + "District}", targetDistrict.isSenatorDistrict() ? targetDistrict.getState() : targetDistrict.getState() + " District " + String.valueOf(targetDistrict.getNumber()));
            email.replaceAll(MOC + "Number}", phoneNumbers.get(i));
            email.replaceAll(MOC + "RequestScript}", targetDistrict.getRequests().get(0).getContent());
            email.replaceAll(MOC + "Guide}", rootPath + targetDistrict.getState() + "/" + targetDistrict.getNumber() + trackingPackage);
            email.replaceAll(MOC + "ICalled}", rootPath + "thankyou" + trackingPackage);
        }
        return email;
    }
    

    public List<District> sendEmailReminder(Connection conn, Caller caller) {
        District callerDistrict = Districts.retrieveDistrictById(conn, caller.getDistrictId());
        String trackingId = RandomStringUtils.randomAlphanumeric(8);
        List<District> successfulTargets = new ArrayList<District>();
        String callInPageUrl = "http://" + applicationBaseUrl + "/call/";
        String trackingPackage = "?t=" + trackingId + "&c=" + caller.getCallerId() + "&d=" + callerDistrict.getNumber();
        if (callFromEmail) {
            List<DistrictHydrated> targetDistricts = newGetDistrictToCall(conn, callerDistrict);
            Message reminderMessage = new Message();
            reminderMessage.setSubject("It's time to call about climate change");
            reminderMessage.setBody(
                makeCallInReminderReplacements(targetDistricts, caller, trackingPackage, this.regularCallInReminderHTML));
            try {
                if (emailDeliveryService.sendHtmlMessage(caller, reminderMessage)) {
                    logger.info(String.format("Sent email reminder to caller {id: %d, name %s %s}.", caller.getCallerId(),
                    caller.getFirstName(), caller.getLastName()));
                    for(DistrictHydrated target : targetDistricts){
                        successfulTargets.add(Districts.retrieveDistrictById(target.getDistrictId()));
                    }
                }
            } catch (Exception e) {
              logger.warning(String.format("Failed to send email to caller {id: %d, name %s %s}: %s", caller.getCallerId(),
                  caller.getFirstName(), caller.getLastName(), e.getMessage()));
            }    
        } else {
            District targetDistrict = getDistrictToCall(conn, caller);            
            Message reminderMessage = new Message();
            reminderMessage.setSubject("It's time to call about climate change");
            reminderMessage.setBody(this.regularCallInReminderHTML.replaceAll("cclcalls.org/call/", callInPageUrl + targetDistrict.getState() + "/" + targetDistrict.getNumber() + trackingPackage));
            try {
                if (emailDeliveryService.sendHtmlMessage(caller, reminderMessage)) {
                    logger.info(String.format("Sent email reminder to caller {id: %d, name %s %s}.",
                            caller.getCallerId(), caller.getFirstName(), caller.getLastName()));
                    successfulTargets.add(targetDistrict);
                }
            }
            catch (Exception e) {
                logger.warning(String.format("Failed to send email to caller {id: %d, name %s %s}: %s",
                        caller.getCallerId(), caller.getFirstName(), caller.getLastName(), e.getMessage()));
                emailReminderSent = false;
            }
        }
        return successfulTargets;
    }

    private List<DistrictHydrated> newGetDistrictToCall( 
            Connection conn,
            District callerDistrict) throws SQLException {
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
            districtsToCall.add(Districts.retrieveDistrictHydratedById(conn, targetDistrictId));
        }
        return districtsToCall;
    }

    private District getDistrictToCall(
            Connection conn,
            Caller caller) throws SQLException {
        District callerDistrict = Districts.retrieveDistrictById(conn, caller.getDistrictId());
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
            Districts.retrieveDistrictById(conn, targetDistrictId);
    }

    public boolean sendStaleScrptNotification(
            District district,
            String adminEmail)
        throws Exception {
        boolean success = false;
        if (adminEmail != null) {
            Message message = new Message();
            message.setSubject("Your district call-in script may need updating.");
            // todo: replace with HTML? Add a link to admin portal?
            message.setBody(district.getScriptModifiedTime() == null
                    ? String.format("The call in script for %s district %d has not yet been created.", district.getState(),
                        district.getNumber())
                    : staleScriptHTML.replace("$district$", district.readableName()).replace("$updateDate$",
                        dateFormat.format(district.getScriptModifiedTime())));
            Caller adminAsCaller = new Caller();
            adminAsCaller.setEmail(adminEmail);
            success = emailDeliveryService.sendTextMessage(adminAsCaller, message);
        }
        if (success) {
            logger.info(String.format("Sent stale script warning to %s for %s district %d", adminEmail, district.getState(),
                    district.getNumber()));
        } else {
            logger.warning(String.format(
                    "Could not send stale script warning to Admin for %s district %d.  Possibly invalid email address '%s'.",
                    district.getState(), district.getNumber(), adminEmail));
        }
        return success;
  }

}
