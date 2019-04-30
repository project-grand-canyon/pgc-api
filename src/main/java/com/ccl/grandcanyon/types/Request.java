package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Request extends GCBase {

  // Requests table column names
  public static final String REQUEST_ID = "request_id";
  public static final String DISTRICT_ID = "district_id";
  public static final String CONTENT = "content";

  private int requestId;
  private int districtId;
  private String content;

  public Request(ResultSet rs) throws SQLException {
    super(rs);
    this.requestId = rs.getInt(REQUEST_ID);
    this.districtId = rs.getInt(DISTRICT_ID);
    this.content = rs.getString(CONTENT);
  }

  public Request() {
  }

  public int getRequestId() {
    return requestId;
  }

  public void setRequestId(int requestId) {
    this.requestId = requestId;
  }

  public int getDistrictId() {
    return districtId;
  }

  public void setDistrictId(int districtId) {
    this.districtId = districtId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
