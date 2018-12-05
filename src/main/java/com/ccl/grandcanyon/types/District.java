package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class District extends GCBase {

  // District table column names
  public static final String DISTRICT_ID = "district_id";
  public static final String STATE = "state";
  public static final String DISTRICT_NUMBER = "district_number";
  public static final String REPRESENTATIVE = "representative";
  public static final String INFO = "info";


  private int districtId;
  private String state;
  private int number;
  private String representative;
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
    this.representative = rs.getString(REPRESENTATIVE);
    this.info = rs.getString(INFO);
  }

  // default, for JSON
  public District() {
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
  public String getRepresentative() {
    return representative;
  }
  public void setRepresentative(String representative) {
    this.representative = representative;
  }
  public String getInfo() {
    return info;
  }
  public void setInfo(String info) {
    this.info = info;
  }
}
