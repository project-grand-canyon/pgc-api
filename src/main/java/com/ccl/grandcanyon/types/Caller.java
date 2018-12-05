package com.ccl.grandcanyon.types;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Caller extends GCBase {

  // Caller table column names
  public static final String CALLER_ID = "caller_id";
  public static final String FIRST_NAME = "first_name";
  public static final String LAST_NAME = "last_name";
  public static final String CONTACT_METHOD = "contact_method";
  public static final String PHONE = "phone";
  public static final String EMAIL = "email";
  public static final String DISTRICT_ID = "district_id";
  public static final String ZIPCODE = "zipcode";

  private int callerId;
  private String firstName;
  private String lastName;
  private ContactMethod contactMethod;
  private String phone;
  private String email;
  private int districtId;
  private String zipCode;


  /**
   * Create caller object from SQL result set
   * @param rs
   * @throws SQLException
   */
  public Caller(ResultSet rs) throws SQLException {
    super(rs);
    callerId = rs.getInt(CALLER_ID);
    firstName = rs.getString(FIRST_NAME);
    lastName = rs.getString(LAST_NAME);
    contactMethod = ContactMethod.valueOf(rs.getString(CONTACT_METHOD));
    phone = rs.getString(PHONE);
    email = rs.getString(EMAIL);
    districtId = rs.getInt(DISTRICT_ID);
    zipCode = rs.getString(ZIPCODE);
  }

  // default, for JSON
  public Caller() {}

  public int getCallerId() {
    return callerId;
  }

  public void setCallerId(int callerId) {
    this.callerId = callerId;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public ContactMethod getContactMethod() {
    return contactMethod;
  }

  public void setContactMethod(ContactMethod contactMethod) {
    this.contactMethod = contactMethod;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public int getDistrictId() {
    return districtId;
  }

  public void setDistrictId(int districtId) {
    this.districtId = districtId;
  }

  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }

}
