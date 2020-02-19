package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class representing a congressional district or senate seat.
 */
public class District extends GCBase {

  // District table column names
  public static final String DISTRICT_ID = "district_id";
  public static final String STATE = "state";
  public static final String DISTRICT_NUMBER = "district_number";
  public static final String REP_FIRST_NAME = "rep_first_name";
  public static final String REP_LAST_NAME = "rep_last_name";
  public static final String REP_IMAGE_URL = "rep_image_url";
  public static final String INFO = "info";
  public static final String SCRIPT_MODIFIED_TIME = "script_modified_time";
  public static final String LAST_STALE_SCRIPT_NOTIFICATION = "last_stale_script_notification";

  private int districtId;
  private String state;
  private int number;
  private String repFirstName;
  private String repLastName;
  private String repImageUrl;
  private List<CallTarget> callTargets;
  private Timestamp scriptModifiedTime;
  private Timestamp lastStaleScriptNotification;

  private String info;

  /**
   * Create District object from SQL result set
   * @param rs
   * @throws SQLException
   */
  public District(ResultSet rs) throws SQLException {
    super(rs);
    this.districtId = rs.getInt(DISTRICT_ID);
    this.state = rs.getString(STATE);
    this.number = rs.getInt(DISTRICT_NUMBER);
    this.repFirstName = rs.getString(REP_FIRST_NAME);
    this.repLastName = rs.getString(REP_LAST_NAME);
    this.repImageUrl = rs.getString(REP_IMAGE_URL);
    this.info = rs.getString(INFO);
    this.scriptModifiedTime = rs.getTimestamp(SCRIPT_MODIFIED_TIME);
    this.lastStaleScriptNotification = rs.getTimestamp(LAST_STALE_SCRIPT_NOTIFICATION);
    this.callTargets = new ArrayList<>();

    boolean retrieveCallTargets = false;
    ResultSetMetaData metaData = rs.getMetaData();
    for (int i = 1; i < metaData.getColumnCount(); i++) {
      if (CallTarget.TARGET_DISTRICT_ID.equalsIgnoreCase(metaData.getColumnName(i))) {
        retrieveCallTargets = true;
        break;
      }
    }

    if (retrieveCallTargets) {
      do {
        CallTarget callTarget = new CallTarget();
        int targetDistrictId = rs.getInt(CallTarget.TARGET_DISTRICT_ID);
        if (targetDistrictId == 0) {
          callTarget.setTargetDistrictId(this.districtId);
          callTarget.setPercentage(100);
        }
        else {
          callTarget.setTargetDistrictId(targetDistrictId);
          callTarget.setPercentage(rs.getInt(CallTarget.PERCENTAGE));
        }
        callTargets.add(callTarget);
      }
      while (rs.next() && rs.getInt(DISTRICT_ID) == this.districtId);
      // undo the last result set since it doesn't belong to this District
      rs.previous();
    }
  }

  // default, for JSON
  public District() {
    this.callTargets = Collections.emptyList();
  }

  public int getDistrictId() {
    return districtId;
  }

  public void setDistrictId(int districtId) {
    this.districtId = districtId;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public int getNumber() {
    return number;
  }
  public void setNumber(int number) {
    this.number = number;
  }

  public String getRepFirstName() {
    return repFirstName;
  }

  public void setRepFirstName(String repFirstName) {
    this.repFirstName = repFirstName;
  }

  public String getRepLastName() {
    return repLastName;
  }

  public void setRepLastName(String repLastName) {
    this.repLastName = repLastName;
  }

  public String getRepImageUrl() {
    return repImageUrl;
  }

  public void setRepImageUrl(String repImageUrl) {
    this.repImageUrl = repImageUrl;
  }

  public String getInfo() {
    return info;
  }
  public void setInfo(String info) {
    this.info = info;
  }

  public List<CallTarget> getCallTargets() {
    return callTargets;
  }

  public void setCallTargets(List<CallTarget> callTargets) {
    this.callTargets = callTargets;
  }

  public Timestamp getScriptModifiedTime() {
    return scriptModifiedTime;
  }

  public void setScriptModifiedTime(Timestamp scriptModifiedTime) {
    this.scriptModifiedTime = scriptModifiedTime;
  }

  public Timestamp getLastStaleScriptNotification() {
    return lastStaleScriptNotification;
  }

  public void setLastStaleScriptNotification(Timestamp lastStaleScriptNotification) {
    this.lastStaleScriptNotification = lastStaleScriptNotification;
  }

  public String readableName() {
    switch (this.getNumber()){
      case 0: return this.getState();
      case -1: return String.format("%s Sr. Senator", this.getState());
      case -2: return String.format("%s Jr. Senator", this.getState());
      default: return String.format("%s-%s", this.getState(), this.getNumber());
    }
  }

  public boolean isSenatorDistrict(){
    return getNumber() < 0;
  }

}
