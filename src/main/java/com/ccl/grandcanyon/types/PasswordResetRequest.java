package com.ccl.grandcanyon.types;

public class PasswordResetRequest {

  // the new password
  private String password;
  // reset token previously issued by service
  private String token;

  /**
   * JSON passed from client to request a password reset.
   */
  public PasswordResetRequest() {}

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
