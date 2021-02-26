package com.ccl.grandcanyon.types;

/**
 * Status returned from sending a manual reminder message to a caller.
 */
public class ReminderStatus extends ReminderBase {

  // the caller who was reminded
  private Caller caller;

  // the district id that the caller was asked to contact
  private Integer targetDistrictId;

  public ReminderStatus(
      Caller caller,
      Integer targetDistrictId,
      boolean smsDelivered,
      boolean emailDelivered,
      String trackingId) {

    super(smsDelivered, emailDelivered, trackingId);
    this.caller = caller;
    this.targetDistrictId = targetDistrictId;
  }

  public Caller getCaller() {
    return caller;
  }

  public Integer getTargetDistrictId() {
    return targetDistrictId;
  }

  public boolean success() {
    return getSmsDelivered() || getEmailDelivered();
  }
}
