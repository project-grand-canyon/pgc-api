package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Response type when querying reminder history table.
 */
public class ReminderHistory extends ReminderBase {

  private int callerId;

  private int callerDistrictId;

  private int targetDistrictId;

  private Timestamp timeSent;

  public ReminderHistory(ResultSet rs) throws SQLException {

    super(rs.getBoolean(SMS_DELIVERED),
        rs.getBoolean(EMAIL_DELIVERED),
        rs.getString(TRACKING_ID));
    this.callerId = rs.getInt(CALLER_ID);
    this.callerDistrictId = rs.getInt(CALLER_DISTRICT_ID);
    this.targetDistrictId = rs.getInt(TARGET_DISTRICT_ID);
    this.timeSent = rs.getTimestamp(TIME_SENT);
  }

  public int getCallerId() {
    return callerId;
  }

  public int getCallerDistrictId() {
    return callerDistrictId;
  }

  public int getTargetDistrictId() {
    return targetDistrictId;
  }

  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getTimeSent() {
    return timeSent;
  }
}
