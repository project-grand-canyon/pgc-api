package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;

import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class AdminWelcomeService {

    private static final Logger logger = Logger.getLogger(AdminWelcomeService.class.getName());

    private static AdminWelcomeService instance;

    public static void init() {
        assert (instance == null);
        instance = new AdminWelcomeService();
    }

    public static AdminWelcomeService getInstance() {
        return instance;
    }

    public void handleNewAdmin(Admin admin) {
        logger.info("New Admin");
        // do this asynchronously so as not to delay response to end-user
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Message message = new Message();
                message.setSubject("Monthly Calling Campaign Admin Sign Up");
                message.setBody("Thank you for signing up as an admin with the Monthly Calling Campaign. Before you can login, your admin account must be approved by a CCL staff-member. This might take a few days. We appreciate your patience. We'll be in touch with you with further instructions after your admin account is approved. (Any questions? Just repond to this email.\n\nThank you,\nThe MCC Team");
                ReminderService.getInstance().getEmailDeliveryService().sendTextMessage(admin, message);
            } catch (Exception e) {
                logger.severe(String.format("Failed to send admin welcome email to admin {id: %d}: %s", admin.getAdminId(), e.getMessage()));
            }
        });
    }
}