package com.ccl.grandcanyon.types;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
  public static final String PAUSED = "paused";
  public static final String CCL_ID = "ccl_id";
  public static final String REFERRER = "referrer";
  public static final String DAY_OF_MONTH = Reminder.DAY_OF_MONTH;
  public static final String LAST_REMINDER_TIMESTAMP = Reminder.LAST_REMINDER_TIMESTAMP;
  public static final String LAST_CALL_TIMESTAMP = "last_call_timestamp";

  private int callerId;
  private String firstName;
  private String lastName;
  private List<ContactMethod> contactMethods;
  private String phone;
  private String email;
  private int districtId;
  private String zipCode;
  private boolean paused;
  private int reminderDayOfMonth;
  private String cclId;
  private String referrer;
  private Timestamp lastReminderTimestamp;
  private Timestamp lastCallTimestamp;


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
    phone = rs.getString(PHONE);
    email = rs.getString(EMAIL);
    districtId = rs.getInt(DISTRICT_ID);
    zipCode = rs.getString(ZIPCODE);
    paused = rs.getBoolean(PAUSED);
    reminderDayOfMonth = rs.getInt(DAY_OF_MONTH);
    lastReminderTimestamp = rs.getTimestamp(LAST_REMINDER_TIMESTAMP);
    lastCallTimestamp = rs.getTimestamp(LAST_CALL_TIMESTAMP);
    cclId = rs.getString(CCL_ID);
    referrer = rs.getString(REFERRER);
    this.contactMethods = new ArrayList<>();
    do {
      String cm = rs.getString(CONTACT_METHOD);
      if (cm != null) {
        contactMethods.add(ContactMethod.valueOf(cm));
      }
    } while (rs.next() && rs.getInt(CALLER_ID) == this.callerId);
    // undo the last result set row since it doesn't belong to this admin
    rs.previous();
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

  public List<ContactMethod> getContactMethods() {
    return contactMethods;
  }

  public void setContactMethods(List<ContactMethod> contactMethods) {
    this.contactMethods = contactMethods;
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

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public int getReminderDayOfMonth() {
    return reminderDayOfMonth;
  }

  public void setReminderDayOfMonth(int reminderDayOfMonth) {
    this.reminderDayOfMonth = reminderDayOfMonth;
  }

  public String getCclId() { return cclId; }

  public  void setCclId(String cclId) { this.cclId = cclId; }

  public String getReferrer() { return referrer; }

  public  void setReferrer(String referrer) { this.referrer= referrer; }

  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getLastReminderTimestamp() {
    return lastReminderTimestamp;
  }

  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getLastCallTimestamp() {
    return lastCallTimestamp;
  }
}
