package com.ccl.grandcanyon.types;

public abstract class ReminderBase {

  // Reminder history column names
  public static final String CALLER_ID = "caller_id";
  public static final String CALLER_DISTRICT_ID = "caller_district_id";
  public static final String TARGET_DISTRICT_ID = "target_district_id";
  public static final String TIME_SENT = "time_sent";
  public static final String TRACKING_ID = "tracking_id";
  public static final String EMAIL_DELIVERED = "email_delivered";
  public static final String SMS_DELIVERED = "sms_delivered";


  // whether an SMS message was delivered to the caller
  protected boolean smsDelivered;

  // whether an email was delivered to the caller
  protected boolean emailDelivered;

  // unique tracking ID for this reminder
  protected String trackingId;

  protected ReminderBase(
      boolean smsDelivered,
      boolean emailDelivered,
      String trackingId) {

    this.smsDelivered = smsDelivered;
    this.emailDelivered = emailDelivered;
    this.trackingId = trackingId;
  }

  public boolean getSmsDelivered() {
    return smsDelivered;
  }

  public boolean getEmailDelivered() {
    return emailDelivered;
  }

  public String getTrackingId() {
    return trackingId;
  }
}
