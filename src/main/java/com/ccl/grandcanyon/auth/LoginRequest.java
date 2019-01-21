package com.ccl.grandcanyon.auth;


public class LoginRequest {

  private String userName;
  private String password;

  public LoginRequest() {}

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
