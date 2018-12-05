package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Theme extends GCBase {

  // Theme table column names
  public static final String THEME_ID = "theme_id";
  public static final String THEME_NAME = "theme_name";

  private int themeId;
  private String name;


  public Theme(ResultSet rs) throws SQLException {
    super(rs);
    this.themeId = rs.getInt(THEME_ID);
    this.name = rs.getString(THEME_NAME);
  }

  public Theme() {
  }

  public int getThemeId() {
    return themeId;
  }
  public void setThemeId(int themeId) {
    this.themeId = themeId;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
}
