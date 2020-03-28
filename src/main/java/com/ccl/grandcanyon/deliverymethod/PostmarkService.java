package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Caller;
import com.wildbit.java.postmark.Postmark;
import com.wildbit.java.postmark.client.ApiClient;
import com.wildbit.java.postmark.client.data.model.message.Message;
import com.wildbit.java.postmark.client.data.model.message.MessageResponse;

import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PostmarkService implements DeliveryService {

    private final static String API_KEY_PROP = "postmark.APIKey";
    private final static String FROM_ADDRESS_PROP ="emailFromAddress";
    // They rate limit
    private final static Long SEND_FREQUENCY = 5L;

    private static final Logger logger = Logger.getLogger(PostmarkService.class.getName());

    private ApiClient apiClient;
    private String fromAddress;
    private Queue<Message> messageQueue;
    private ScheduledFuture sendingTask;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void init(Properties config) {
        String apiKey = config.getProperty(API_KEY_PROP);
        this.fromAddress = config.getProperty(FROM_ADDRESS_PROP);
        if (apiKey == null || fromAddress == null) {
            throw new RuntimeException((
                    "Missing one or more required configuration properties: " +
                            API_KEY_PROP + ", " + FROM_ADDRESS_PROP));
        }
        apiClient = Postmark.getApiClient(apiKey);

        // Postmark rate limit
        messageQueue = new LinkedBlockingQueue<>();
        this.sendingTask = executorService.scheduleAtFixedRate(new PostmarkSender(), 10, SEND_FREQUENCY, TimeUnit.SECONDS);
    }

    @Override
    public void tearDown() {
        if (sendingTask != null) {
            sendingTask.cancel(true);
        }
        executorService.shutdown();
    }

    @Override
    public boolean sendTextMessage(Caller caller, com.ccl.grandcanyon.types.Message message) throws Exception {
        return sendMessage(caller.getEmail(), message);
    }

    @Override
    public boolean sendHtmlMessage(Caller caller, com.ccl.grandcanyon.types.Message message) throws Exception {
        return sendMessage(caller.getEmail(), message);
    }

    @Override
    public boolean sendTextMessage(Admin admin, com.ccl.grandcanyon.types.Message message) throws Exception {
        return sendMessage(admin.getEmail(), message);
    }

    public boolean sendMessage(
            String recipientEmailAddress,
            com.ccl.grandcanyon.types.Message message) {

        Message postmarkMessage = new Message(
                fromAddress,
                recipientEmailAddress,
                message.getSubject(),
                message.getBody());
        postmarkMessage.setTrackLinks(Message.TRACK_LINKS.HtmlAndText);
        postmarkMessage.setTrackOpens(true);
        messageQueue.add(postmarkMessage);
        return true; //TODO: this is misleading b/c it hasn't actually succeeded yet
    }

    class PostmarkSender implements Runnable {
        @Override
        public void run() {
            Message message = messageQueue.poll();
            if (message != null) {
                try {
                    MessageResponse response = apiClient.deliverMessage(message);
                    logger.info(String.format("Sent message %s to %s", response.getMessageId(), message.getTo()));
                } catch (Exception e) {
                    logger.severe(String.format("Failed to send email to caller at address %s: %s",
                            message.getTo(), e.getLocalizedMessage()));
                }
            }
        }
    }
}
