package com.ccl.grandcanyon.types;

import jnr.x86asm.OP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

public class DistrictOffice extends GCBase {

  // District Office column names
  public static final String DISTRICT_OFFICE_ID = "district_office_id";
  public static final String DISTRICT_ID = "district_id";
  public static final String PHONE = "phone";
  public static final String ADDRESS_LINE1 = "address_line1";
  public static final String ADDRESS_LINE2 = "address_line2";
  public static final String CITY = "city";
  public static final String STATE = "state";
  public static final String COUNTRY = "country";
  public static final String ZIPCODE = "zipcode";
  public static final String EMAIL = "email";
  public static final String OPENS_AT = "opens_at";
  public static final String CLOSES_AT = "closes_at";

  private int districtOfficeId;
  private int districtId;
  private String phone;
  private Address address;
  private String email;
  private Time opensAt;
  private Time closesAt;

  public DistrictOffice() {}

  public DistrictOffice(ResultSet rs) throws SQLException  {
    super(rs);
    this.districtOfficeId = rs.getInt(DISTRICT_OFFICE_ID);
    this.districtId = rs.getInt(DISTRICT_ID);
    this.phone = rs.getString(PHONE);
    this.address = new Address();
    this.address.setAddressLine1(rs.getString(ADDRESS_LINE1));
    this.address.setAddressLine2(rs.getString(ADDRESS_LINE2));
    this.address.setCity(rs.getString(CITY));
    this.address.setState(rs.getString(STATE));
    this.address.setCountry(rs.getString(COUNTRY));
    this.address.setZipCode(rs.getString(ZIPCODE));
    this.email = rs.getString(EMAIL);
    this.opensAt = rs.getTime(OPENS_AT);
    this.closesAt = rs.getTime(CLOSES_AT);
  }

  public int getDistrictOfficeId() {
    return districtOfficeId;
  }

  public void setDistrictOfficeId(int districtOfficeId) {
    this.districtOfficeId = districtOfficeId;
  }

  public int getDistrictId() {
    return districtId;
  }

  public void setDistrictId(int districtId) {
    this.districtId = districtId;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Time getOpensAt() {
    return opensAt;
  }

  public void setOpensAt(Time opensAt) {
    this.opensAt = opensAt;
  }

  public Time getClosesAt() {
    return closesAt;
  }

  public void setClosesAt(Time closesAt) {
    this.closesAt = closesAt;
  }
}
