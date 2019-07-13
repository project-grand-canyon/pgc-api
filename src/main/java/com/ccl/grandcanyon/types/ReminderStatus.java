package com.ccl.grandcanyon.types;

/**
 * Status returned from sending a reminder message to a caller.
 */
public class ReminderStatus {

  // Reminder history column names
  public static final String CALLER_ID = "caller_id";
  public static final String CALLER_DISTRICT_ID = "caller_district_id";
  public static final String TARGET_DISTRICT_ID = "target_district_id";
  public static final String TIME_SENT = "time_sent";
  public static final String TRACKING_ID = "tracking_id";
  public static final String EMAIL_DELIVERED = "email_delivered";
  public static final String SMS_DELIVERED = "sms_delivered";

  // the caller who was reminded
  private Caller caller;

  // the district that the caller was asked to contact
  private District targetDistrict;

  // whether an SMS message was delivered to the caller
  private boolean smsDelivered;

  // whether an email was delivered to the caller
  private boolean emailDelivered;

  // unique tracking ID for this reminder
  private String trackingId;

  public ReminderStatus(
      Caller caller,
      District targetDistrict,
      boolean smsDelivered,
      boolean emailDelivered,
      String trackingId) {

    this.caller = caller;
    this.targetDistrict = targetDistrict;
    this.smsDelivered = smsDelivered;
    this.emailDelivered = emailDelivered;
    this.trackingId = trackingId;
  }

  public Caller getCaller() {
    return caller;
  }

  public District getTargetDistrict() {
    return targetDistrict;
  }

  public boolean smsDelivered() {
    return smsDelivered;
  }

  public boolean emailDelivered() {
    return emailDelivered;
  }

  public String getTrackingId() {
    return trackingId;
  }

  public boolean success() {
    return smsDelivered || emailDelivered;
  }
}
