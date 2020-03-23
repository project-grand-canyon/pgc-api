package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Caller;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.*;

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
    String toNumber = getCallerNumber(caller);
    String body = message.getBody();
    if (body.length() <= 140) {
      return sendTwilioMessage(toNumber, body);
    }

    // If the message is longer than 140 (the max length for a message)
    // then we chunk it up and append page numbers ex. 1/3
    // We assume not <10 chunks
    int MESSAGE_LENGTH = 136; // SMS is 140, but we leave space for page number
    int messageCount = body.length() / MESSAGE_LENGTH;
    messageCount += body.length() % MESSAGE_LENGTH == 0 ? 0 : 1;
    boolean overallSendResult = true;
    // TODO: Currently this cuts of words mid-word. In future, separate messages on spaces between words.
    for (int i = 0; i < messageCount; i++) {
      int humanReadableIndex = i + 1;
      int start = i*MESSAGE_LENGTH;
      int end = i*MESSAGE_LENGTH+MESSAGE_LENGTH < body.length() ? i*MESSAGE_LENGTH+MESSAGE_LENGTH :body.length();
      String currentMessage = String.format("%s %s/%s", body.substring(start, end), humanReadableIndex, messageCount);
      boolean sendResult = sendTwilioMessage(toNumber, currentMessage);
      if (sendResult == false) {
        overallSendResult = false;
      }
    }
    return overallSendResult;
  }

  private boolean sendTwilioMessage(String toNumber, String messageBody) {
    Message twilioResult = Message.creator(
            new PhoneNumber(toNumber),
            new PhoneNumber(fromPhoneNumber), messageBody).create();
    return !twilioResult.getStatus().equals(Message.Status.FAILED);
  }

  private String getCallerNumber(Caller caller) {
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
    return formattedNumber.toString();
  }

  @Override
  public void tearDown(){
    // Do nothing
  }

  @Override
  public boolean sendTextMessage(Admin admin, com.ccl.grandcanyon.types.Message message) throws Exception {
    throw new Exception("Twilio service::sendTextMessage(admin, message)-Not Implemented!");
  }


  @Override
  public boolean sendHtmlMessage(
      Caller caller,
      com.ccl.grandcanyon.types.Message message) {

    // HTML not supported
    return sendTextMessage(caller, message);
  }
}
