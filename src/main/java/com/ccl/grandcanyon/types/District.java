package com.ccl.grandcanyon.types;

public class District {

  private int districtId;
  private String state;
  private int number;
  private String representative;
  private String info;

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
