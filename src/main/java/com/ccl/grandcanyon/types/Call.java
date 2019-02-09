package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Call {

  public static final String CALLER_ID = "caller_id";
  public static final String DISTRICT_ID = "district_id";
  public static final String TALKING_POINT_ID = "talking_point_id";
  public static final String MONTH = "month";
  public static final String YEAR = "year";
  public static final String CREATED = "created";

  private int callerId;
  private Integer districtId;
  private Integer talkingPointId;
  private int month;
  private int year;
  private Timestamp created;
  // tracking ID is not persisted.   It is used only on input from client to
  // record a new call.
  private String trackingId;


  public Call(ResultSet rs) throws SQLException {
    this.created = rs.getTimestamp(CREATED);
    this.callerId = rs.getInt(CALLER_ID);
    this.districtId = rs.getInt(DISTRICT_ID);
    this.month = rs.getInt(MONTH);
    this.year = rs.getInt(YEAR);
    this.talkingPointId = rs.getInt(TALKING_POINT_ID);
  }

  public Call() {}

  public int getCallerId() {
    return callerId;
  }

  public void setCallerId(int callerId) {
    this.callerId = callerId;
  }

  public Integer getDistrictId() {
    return districtId;
  }

  public void setDistrictId(Integer districtId) {
    this.districtId = districtId;
  }

  public Integer getTalkingPointId() {
    return talkingPointId;
  }

  public void setTalkingPointId(Integer talkingPointId) {
    this.talkingPointId = talkingPointId;
  }

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  @JsonIgnore
  public String getTrackingId() {
    return trackingId;
  }

  @JsonProperty(value = "trackingId")
  public void setTrackingId(String trackingId) {
    this.trackingId = trackingId;
  }

  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getCreated() {
    return created;
  }

}
