package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Persistent SQL object for password reset token.
 */
public class PasswordResetToken {

  // column names
  public static final String ADMIN_ID = "admin_id";
  public static final String TOKEN = "token";
  public static final String EXPIRATION = "expiration";

  // ID of the Admin who requested the reset
  private int adminId;
  // token string issued for the reset flow
  private String token;
  // token expiration time
  private Timestamp expiration;

  public PasswordResetToken(
      int adminId,
      String token,
      Timestamp expiration) {

    this.adminId = adminId;
    this.token = token;
    this.expiration = expiration;
  }

  public PasswordResetToken(ResultSet rs) throws SQLException {
    this.adminId = rs.getInt(ADMIN_ID);
    this.token = rs.getString(TOKEN);
    this.expiration = rs.getTimestamp(EXPIRATION);
  }

  public int getAdminId() {
    return adminId;
  }

  public String getToken() {
    return token;
  }

  public Timestamp getExpiration() {
    return expiration;
  }

}
