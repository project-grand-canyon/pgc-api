package com.ccl.grandcanyon.auth;

/**
 * JSON content of a token response to a login request.
 */
public class TokenResponse {

  private String accessToken;
  private int expiresIn;

  public TokenResponse() {}

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public int getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(int expiresIn) {
    this.expiresIn = expiresIn;
  }
}
