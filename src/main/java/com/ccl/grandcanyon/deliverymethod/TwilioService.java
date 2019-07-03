package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Caller;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.Properties;

public class TwilioService implements DeliveryService {

  private final static String ACCOUNT_ID_PROP = "twilio.SID";
  private final static String AUTH_TOKEN_PROP = "twilio.AuthToken";

  private final static String fromPhoneNumber = "+15123557939";


  public void init(Properties config) {
    Twilio.init(config.getProperty(ACCOUNT_ID_PROP),
        config.getProperty(AUTH_TOKEN_PROP));
  }


  public boolean sendTextMessage(
      Caller caller,
      com.ccl.grandcanyon.types.Message message) {

    // format caller phone number:  this is for USA numbers only!!
    StringBuilder formattedNumber = new StringBuilder("+");
    if (!caller.getPhone().startsWith("1")) {
      formattedNumber.append('1');
    }
    for (char c : caller.getPhone().toCharArray()) {
      if (Character.isDigit(c)) {
        formattedNumber.append(c);
      }
    }

    Message twilioMessage = Message.creator(
        new PhoneNumber(formattedNumber.toString()),
        new PhoneNumber(fromPhoneNumber), message.getBody()).create();
    return !(twilioMessage.getStatus().equals(Message.Status.FAILED));
  }


  @Override
  public boolean sendHtmlMessage(
      Caller caller,
      com.ccl.grandcanyon.types.Message message) {

    // HTML not supported
    return sendTextMessage(caller, message);
  }
}
