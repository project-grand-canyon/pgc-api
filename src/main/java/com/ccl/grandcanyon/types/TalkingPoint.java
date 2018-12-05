package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TalkingPoint extends GCBase {

  // Talking Points table column names
  public static final String TALKING_POINT_ID = "talking_point_id";
  public static final String CONTENT = "content";
  public static final String THEME_ID = "theme_id";

  private int talkingPointId;
  private int themeId;
  private String content;


  public TalkingPoint(ResultSet rs) throws SQLException  {
    super(rs);
    this.talkingPointId = rs.getInt(TALKING_POINT_ID);
    this.content = rs.getString(CONTENT);
    this.themeId = rs.getInt(THEME_ID);
  }

  public TalkingPoint() {}

  public int getTalkingPointId() {
    return talkingPointId;
  }

  public void setTalkingPointId(int talkingPointId) {
    this.talkingPointId = talkingPointId;
  }

  public int getThemeId() {
    return themeId;
  }

  public void setThemeId(int themeId) {
    this.themeId = themeId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

}
