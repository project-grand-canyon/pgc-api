package com.ccl.grandcanyon.types;

public class CallTarget {

  // column names
  public static final String DISTRICT_ID = "district_id";
  public static final String TARGET_DISTRICT_ID = "target_district_id";
  public static final String PERCENTAGE = "percentage";

  private int targetDistrictId;
  private int percentage;

  public CallTarget() {}

  public int getTargetDistrictId() {
    return targetDistrictId;
  }

  public void setTargetDistrictId(int targetDistrictId) {
    this.targetDistrictId = targetDistrictId;
  }

  public int getPercentage() {
    return percentage;
  }

  public void setPercentage(int percentage) {
    this.percentage = percentage;
  }
}
