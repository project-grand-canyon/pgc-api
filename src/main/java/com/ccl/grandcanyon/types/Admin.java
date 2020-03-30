package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Grand Canyon administrator.
 */
public class Admin extends GCBase {

  // SQL column names
  public static final String ADMIN_ID = "admin_id";
  public static final String USER_NAME = "user_name";
  public static final String EMAIL = "email";
  public static final String IS_ROOT = "is_root";
  public static final String TOKEN = "token";
  public static final String DISTRICT_ID = "district_id";
  public static final String LOGIN_ENABLED = "login_enabled";


  private int adminId;
  private String userName;
  private String email;
  private List<Integer> districts;
  private boolean isRoot;
  private boolean loginEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String password;

  @JsonIgnore
  private String token;

  public Admin() {}

  public enum IncludeDistricts{YES, NO}
  public Admin(ResultSet rs, IncludeDistricts includeDistricts) throws SQLException {
    super(rs);
    this.adminId = rs.getInt(ADMIN_ID);
    this.userName = rs.getString(USER_NAME);
    this.email = rs.getString(EMAIL);
    this.isRoot = rs.getBoolean(IS_ROOT);
    this.token = rs.getString(TOKEN);
    this.loginEnabled = rs.getBoolean(LOGIN_ENABLED);
    this.password = null;  // always
    if(includeDistricts == IncludeDistricts.YES){
      this.districts = new ArrayList<>();
      do {
        int districtId = rs.getInt(DISTRICT_ID);
        if (districtId != 0) {
          districts.add(districtId);
        }
      } while (rs.next() && rs.getInt(ADMIN_ID) == this.adminId);
      // undo the last result set row since it doesn't belong to this admin
      rs.previous();
    }

  }


  public int getAdminId() {
    return adminId;
  }

  public void setAdminId(int adminId) {
    this.adminId = adminId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public List<Integer> getDistricts() {
    return districts;
  }

  public void setDistricts(List<Integer> districts) {
    this.districts = districts;
  }

  public boolean isRoot() {
    return isRoot;
  }

  public void setRoot(boolean root) {
    isRoot = root;
  }

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

  public boolean isLoginEnabled() {
    return loginEnabled;
  }

  public void setLoginEnabled(boolean loginEnabled) {
    this.loginEnabled = loginEnabled;
  }

}
