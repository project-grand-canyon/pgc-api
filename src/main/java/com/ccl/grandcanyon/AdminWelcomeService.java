package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.FileReader;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class AdminWelcomeService {

    private String adminWelcomeHtml;
    private String adminWelcomeResource = "adminWelcomeEmail.html";


    private static final Logger logger = Logger.getLogger(AdminWelcomeService.class.getName());

    private static AdminWelcomeService instance;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void init(Properties config) {
        assert (instance == null);
        instance = new AdminWelcomeService(config);
    }

    public void tearDown(){
        executorService.shutdown();
    }


    public static AdminWelcomeService getInstance() {
        assert(instance != null);
        return instance;
    }

    private AdminWelcomeService(Properties config) {
        logger.info("Init Welcome Service");

        try {
            this.adminWelcomeHtml = FileReader.create().read(adminWelcomeResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load welcome email template: " + e.getLocalizedMessage());
        }
    }

    public void handleNewAdmin(Admin admin) {
        logger.info("New Admin");
        // do this asynchronously so as not to delay response to end-user
        executorService.submit(() -> {
            try {
                Message message = new Message();
                message.setSubject("Monthly Calling Campaign Admin Sign Up");
                message.setBody(adminWelcomeHtml);
                ReminderService.getInstance().getEmailDeliveryService().sendTextMessage(admin, message);
            } catch (Exception e) {
                logger.severe(String.format("Failed to send welcome email to admins {id: %d}: %s", admin.getAdminId(), e.getMessage()));
            }
        });
    }
}
