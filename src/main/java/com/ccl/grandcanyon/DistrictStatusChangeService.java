package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.ContactMethod;
import com.ccl.grandcanyon.types.Message;
import com.ccl.grandcanyon.types.Status;
import com.ccl.grandcanyon.utils.HtmlUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class DistrictStatusChangeService {
    private static final Logger logger = Logger.getLogger(DistrictStatusChangeService.class.getName());
    private static DistrictStatusChangeService instance;
    private DeliveryService smsDeliveryService;
    private DeliveryService emailDeliveryService;
    private static final String covidPauseEmailResource = "covidPause.html";
    private String covidPauseHtmlBody;
    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

    public static DistrictStatusChangeService getInstance() {
        if (instance == null) {
            instance = new DistrictStatusChangeService();
        }
        return instance;
    }

    public void tearDown(){
        executorService.shutdown();
    }

    private DistrictStatusChangeService() {
        ReminderService reminderService = ReminderService.getInstance();
        emailDeliveryService = reminderService.getEmailDeliveryService();
        smsDeliveryService = reminderService.getSmsDeliveryService();
        covidPauseHtmlBody = HtmlUtils.ReadHtmlFile(getClass().getClassLoader(), covidPauseEmailResource);
    }

    public void handleStatusChange(int districtId, Status oldStatus, Status newStatus) throws SQLException {
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            if (oldStatus == Status.active && newStatus == Status.covid_paused) {
                List<Caller> callers = Callers.getCallers(conn, districtId);
                callers.stream().filter(caller -> !caller.isPaused()).forEach(this::sendCovidPausedMessages);
            }
        }
    }

    private void sendCovidPausedMessages(Caller caller) {
        if (caller.getContactMethods().contains(ContactMethod.sms)) {
            executorService.execute(() -> sendCovidPausedSms(caller));
        }
        if (caller.getContactMethods().contains(ContactMethod.email)) {
            executorService.execute(() -> sendCovidPausedEmail(caller));
        }
    }

    private void sendCovidPausedSms(Caller caller) {
        final String messsageBody = "Update: CCL's Monthly Calling Campaign is temporarily paused in order to give your congressional office time to respond to the COVID-19 crisis. Your notifications will resume when the crisis abates.";
        Message message = new Message();
        message.setBody(messsageBody);
        try {
            boolean messageSent = smsDeliveryService.sendTextMessage(caller, message);
            if (messageSent) {
                logger.info(String.format("Sent SMS covid pause message to caller {id: %d}", caller.getCallerId()));
            }
        } catch (Exception e) {
            logger.severe(String.format("Failed to send SMS covid pause message to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
        }
    }

    private void sendCovidPausedEmail(Caller caller) {
        final String messageSubject = "Monthly Calling Campaign Paused For COVID-19";
        Message message = new Message();
        message.setSubject(messageSubject);
        message.setBody(covidPauseHtmlBody);
        try {
            boolean messageSent = emailDeliveryService.sendHtmlMessage(caller, message);
            if (messageSent) {
                logger.info(String.format("Sent email covid pause message to caller {id: %d}", caller.getCallerId()));
            }
        } catch (Exception e) {
            logger.severe(String.format("Failed to send email covid pause message to caller {id: %d}: %s", caller.getCallerId(), e.getMessage()));
        }
    }
}
