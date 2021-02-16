package com.ccl.grandcanyon.types;

/**
 * Status returned from sending a manual reminder message to a caller.
 */
public class ReminderStatus extends ReminderBase {

  // the caller who was reminded
  private Caller caller;

  // the district that the caller was asked to contact
  private District targetDistrict;

  public ReminderStatus(
      Caller caller,
      District targetDistrict,
      boolean smsDelivered,
      boolean emailDelivered,
      String trackingId) {

    super(smsDelivered, emailDelivered, trackingId);
    this.caller = caller;
    this.targetDistrict = targetDistrict;
  }

  public Caller getCaller() {
    return caller;
  }

  public District getTargetDistrict() {
    return targetDistrict;
  }
}
