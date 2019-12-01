package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.*;
import com.wildbit.java.postmark.Postmark;
import com.wildbit.java.postmark.client.ApiClient;
import com.wildbit.java.postmark.client.data.model.message.Message;
import com.wildbit.java.postmark.client.data.model.message.MessageResponse;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PostmarkService implements DeliveryService {

    private final static String API_KEY_PROP = "postmark.APIKey";
    private final static String FROM_ADDRESS_PROP ="emailFromAddress";
    // Space out email sends to smooth out traffic
    private final static Long SEND_FREQUENCY = 30L;

    private static final Logger logger = Logger.getLogger(PostmarkService.class.getName());

    private ApiClient apiClient;
    private String fromAddress;
    private Queue<Message> messageQueue;
    private ScheduledFuture sendingTask;

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
        messageQueue = new LinkedList();
        this.sendingTask = Executors.newSingleThreadScheduledExecutor().
                scheduleAtFixedRate(new PostmarkSender(), 10, SEND_FREQUENCY, TimeUnit.SECONDS);
    }

    public void tearDown() {
        if (sendingTask != null) {
            sendingTask.cancel(true);
        }
    }

    @Override
    public boolean sendTextMessage(Caller caller, com.ccl.grandcanyon.types.Message message) throws Exception {
        return sendMessage(caller, message);
    }

    @Override
    public boolean sendHtmlMessage(Caller caller, com.ccl.grandcanyon.types.Message message) throws Exception {
        return sendMessage(caller, message);
    }

    public boolean sendMessage(
            Caller caller,
            com.ccl.grandcanyon.types.Message message) {

        Message postmarkMessage = new Message(
                fromAddress,
                caller.getEmail(),
                message.getSubject(),
                message.getBody());
        postmarkMessage.setTrackLinks(Message.TRACK_LINKS.HtmlAndText);
        postmarkMessage.setTrackOpens(true);
        messageQueue.add(postmarkMessage);
        return true;
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
