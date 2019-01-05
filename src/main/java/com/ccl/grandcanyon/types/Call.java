package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Call {

  public static final String CALLER_ID = "caller_id";
  public static final String DISTRICT_ID = "district_id";
  public static final String TALKING_POINT_ID = "talking_point_id";
  public static final String CREATED = "created";

  private int callerId;
  private int districtId;
  private int talkingPointId;
  private Timestamp created;

  public Call(ResultSet rs) throws SQLException {
    this.created = rs.getTimestamp(CREATED);
    this.callerId = rs.getInt(CALLER_ID);
    this.districtId = rs.getInt(DISTRICT_ID);
    this.talkingPointId = rs.getInt(TALKING_POINT_ID);
  }

  public Call() {}

  public int getCallerId() {
    return callerId;
  }

  public void setCallerId(int callerId) {
    this.callerId = callerId;
  }

  public int getDistrictId() {
    return districtId;
  }

  public void setDistrictId(int districtId) {
    this.districtId = districtId;
  }

  public int getTalkingPointId() {
    return talkingPointId;
  }

  public void setTalkingPointId(int talkingPointId) {
    this.talkingPointId = talkingPointId;
  }

  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getCreated() {
    return created;
  }

}
