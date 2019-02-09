package com.ccl.grandcanyon.reminders;

import com.ccl.grandcanyon.types.Caller;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.Properties;

public class TwilioReminderService {

  private final static String ACCOUNT_ID_PROP = "twilioSID";
  private final static String AUTH_TOKEN_PROP = "twilioAuthToken";

  // todo: temporary trial number
  private final static String fromPhoneNumber = "+15128724874";


  private Caller caller;
  private String trackingId;

  public static void init(Properties config) {
    Twilio.init(config.getProperty(ACCOUNT_ID_PROP),
        config.getProperty(AUTH_TOKEN_PROP));
  }

  public TwilioReminderService(Caller caller, String trackingId) {
    this.caller = caller;
    this.trackingId = trackingId;
  }

  public boolean send() {

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

    // todo: generate full link and retrieve message text from somewhere

    Message message = Message.creator(
        new PhoneNumber(formattedNumber.toString()),
        new PhoneNumber(fromPhoneNumber),
        "Your PGC call link is " + trackingId).create();
    return !(message.getStatus().equals(Message.Status.FAILED));
  }
}
