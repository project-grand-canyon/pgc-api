package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.Message;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;


import javax.ws.rs.core.Response;
import java.util.Properties;
import java.util.logging.Logger;

public class MailgunService implements DeliveryService {

  private final static String API_KEY_PROP = "mailgun.APIKey";
  private final static String BASE_URL_PROP = "mailgun.baseURL";
  private final static String FROM_ADDRESS_PROP ="emailFromAddress";

  private static final Logger logger = Logger.getLogger(MailgunService.class.getName());

  private String apiKey;
  private String targetUrl;
  private String fromAddress;


  public void init(Properties config) {

    this.apiKey = config.getProperty(API_KEY_PROP);
    this.targetUrl = config.getProperty(BASE_URL_PROP) + "/messages";
    this.fromAddress = config.getProperty(FROM_ADDRESS_PROP);
    if (apiKey == null || targetUrl == null || fromAddress == null) {
      throw new RuntimeException((
          "Missing one or more required configuration properties: " +
          API_KEY_PROP + ", " + BASE_URL_PROP + ", " + FROM_ADDRESS_PROP));
    }
  }



  public boolean sendHtmlMessage(
      Caller caller,
      Message message) throws Exception {

    return sendMessage(caller, message, "html");
  }

  public boolean sendTextMessage(
      Caller caller,
      Message message) throws Exception {

    return sendMessage(caller, message, "text");
  }

  public boolean sendMessage(
      Caller caller,
      Message message,
      String bodyFormat) throws UnirestException {

    HttpResponse<JsonNode> response = Unirest.post(targetUrl).
        basicAuth("api", apiKey).
        field("from", fromAddress).
        field("to", caller.getEmail()).
        field("subject", message.getSubject()).
        field(bodyFormat, message.getBody()).asJson();

    boolean success = Response.Status.Family.familyOf(response.getStatus())
        == Response.Status.Family.SUCCESSFUL;
    if (!success) {
      logger.warning(String.format("Failed to send email to caller at address %s: %s",
          caller.getEmail(), response.getBody().toString()));
    }
    return success;
  }
}
