package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

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

  public static final String INFO = "info";

  private int districtId;
  private String state;
  private int number;
  private String repFirstName;
  private String repLastName;

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

  public String getInfo() {
    return info;
  }
  public void setInfo(String info) {
    this.info = info;
  }
}
