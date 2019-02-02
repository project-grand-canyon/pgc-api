package com.ccl.grandcanyon.deliverymethod;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.District;
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


  public boolean sendRegularCallInReminder(Caller caller, District district, String trackingId) {

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

    String body = "It's your day to call Rep. " + district.getRepLastName() + ". http://project-grand-canyon.com/call/" + district.getState() + "/" + district.getNumber() + "?track=" + trackingId + "&caller=" + caller.getCallerId();

    Message message = Message.creator(
        new PhoneNumber(formattedNumber.toString()),
        new PhoneNumber(fromPhoneNumber),
            body).create();
    return !(message.getStatus().equals(Message.Status.FAILED));
  }
}
