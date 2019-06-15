package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.District;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;


import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MailgunService implements DeliveryService {

  private final static String API_KEY_PROP = "mailgun.APIKey";
  private final static String BASE_URL_PROP = "mailgun.baseURL";
  private final static String FROM_ADDRESS_PROP ="emailFromAddress";

  private static final Logger logger = Logger.getLogger(MailgunService.class.getName());

  private String apiKey;
  private String targetUrl;
  private String fromAddress;
  private String regularCallInReminderHTML;
  private String welcomeHTML;


  public void init(Properties config) {

    this.apiKey = config.getProperty(API_KEY_PROP);
    this.targetUrl = config.getProperty(BASE_URL_PROP) + "/messages";
    this.fromAddress = config.getProperty(FROM_ADDRESS_PROP);
    if (apiKey == null || targetUrl == null || fromAddress == null) {
      throw new RuntimeException((
          "Missing one or more required configuration properties: " +
          API_KEY_PROP + ", " + BASE_URL_PROP + ", " + FROM_ADDRESS_PROP));
    }

    this.regularCallInReminderHTML = MailgunService.getRegularCallInReminderHTML(getClass());
    this.welcomeHTML = MailgunService.getWelcomeHTML(getClass());
  }


  public boolean sendRegularCallInReminder(Caller caller, District district, String trackingId) throws UnirestException {

    String callInPageUrl = "projectgrandcanyon.com/call/" + district.getState() + "/" + district.getNumber() + "?t=" + trackingId + "&c=" + caller.getCallerId();
    String html = new String(this.regularCallInReminderHTML).replaceAll("projectgrandcanyon.com/call/", callInPageUrl);

    HttpResponse<JsonNode> response = Unirest.post(targetUrl).
        basicAuth("api", apiKey).
            field("from", fromAddress).
            field("to", caller.getEmail()).
            field("subject", "It's your day to call!").
            field("html", html).asJson();

    boolean success = Response.Status.Family.familyOf(response.getStatus())
        == Response.Status.Family.SUCCESSFUL;
    if (!success) {
      logger.severe(String.format("Failed to send notification email to caller at address %s: %s",
          caller.getEmail(), response.getBody().toString()));
    }
    return success;
  }

  @Override
  public boolean sendWelcomeMessage(Caller caller) throws Exception {
    HttpResponse<JsonNode> response = Unirest.post(targetUrl).
            basicAuth("api", apiKey).
            field("from", fromAddress).
            field("to", caller.getEmail()).
            field("subject", "Welcome to Project Grand Canyon!").
            field("html", this.welcomeHTML).asJson();

    boolean success = Response.Status.Family.familyOf(response.getStatus())
            == Response.Status.Family.SUCCESSFUL;
    if (!success) {
      logger.severe(String.format("Failed to send welcome email to caller at address %s: %s",
              caller.getEmail(), response.getBody().toString()));
    }
    return success;
  }

  @Override
  public boolean sendEventAlert(String name, String details) throws Exception {
    HttpResponse<JsonNode> response = Unirest.post(targetUrl).
            basicAuth("api", apiKey).
            field("from", fromAddress).
            field("to", "boralben@gmail.com").
            field("subject", name).
            field("text", details).asJson();

    boolean success = Response.Status.Family.familyOf(response.getStatus())
            == Response.Status.Family.SUCCESSFUL;
    if (!success) {
      logger.severe("Failed to send new admin email");
    }
    return success;
  }

  private static String getRegularCallInReminderHTML(Class clazz) {
    File file = new File(clazz.getClassLoader().getResource("callNotificationEmail.html").getFile());
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Unable to load regular call-in notification email template: " + e.getLocalizedMessage());
    }
    return br.lines().collect(Collectors.joining());
  }

  private static String getWelcomeHTML(Class clazz) {
    File file = new File(clazz.getClassLoader().getResource("welcomeEmail.html").getFile());
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Unable to load welcome email template: " + e.getLocalizedMessage());
    }
    return br.lines().collect(Collectors.joining());
  }

}
