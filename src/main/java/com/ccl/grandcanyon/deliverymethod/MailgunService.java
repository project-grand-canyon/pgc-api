package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.District;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;


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


  public boolean sendRegularCallInReminder(Caller caller, District district, String trackingId) throws UnirestException {

    String body = "It's your day to call Rep. " + district.getRepLastName() + ". http://project-grand-canyon.com/call/" + district.getState() + "/" + district.getNumber() + "?track=" + trackingId + "&caller=" + caller.getCallerId();

    HttpRequestWithBody request = Unirest.post(targetUrl).
        basicAuth("api", apiKey).
        queryString("from", fromAddress).
        queryString("to", caller.getEmail()).
        queryString("subject", "Project Grand Canyon reminder").
        queryString("text", body);

    HttpResponse<JsonNode> response = request.asJson();
    boolean success = Response.Status.Family.familyOf(response.getStatus())
        == Response.Status.Family.SUCCESSFUL;
    if (!success) {
      logger.warning(String.format("Failed to send email to caller at address %s: %s",
          caller.getEmail(), response.getBody().toString()));
    }
    return success;
  }
}
