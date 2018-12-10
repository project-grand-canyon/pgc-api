package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TalkingPoint extends GCBase {

  // Talking Points table column names
  public static final String TALKING_POINT_ID = "talking_point_id";
  public static final String CONTENT = "content";
  public static final String THEME_ID = "theme_id";
  public static final String SCOPE = "scope";
  public static final String ENABLED = "enabled";
  public static final String DISTRICT_ID = "district_id";
  public static final String STATE = "state";

  private int talkingPointId;
  private int themeId;
  private String content;
  private Scope scope;
  private boolean enabled;
  private List<Integer> districts;
  private List<String> states;



  public TalkingPoint(ResultSet rs) throws SQLException  {
    super(rs);
    this.talkingPointId = rs.getInt(TALKING_POINT_ID);
    this.content = rs.getString(CONTENT);
    this.themeId = rs.getInt(THEME_ID);
    this.enabled = rs.getBoolean(ENABLED);
    this.scope = Scope.valueOf(rs.getString(SCOPE));
    this.districts = new ArrayList<>();
    this.states = new ArrayList<>();
    do {
      int districtId = rs.getInt(DISTRICT_ID);
      if (districtId != 0) {
        districts.add(districtId);
      }
      else {
        String state = rs.getString(STATE);
        if (state != null) {
          states.add(state);
        }
      }
    } while (rs.next() && rs.getInt(TALKING_POINT_ID) == this.talkingPointId);
    // undo the last result set row since it doesn't belong to this talking point
    rs.previous();
  }

  public TalkingPoint() {
    this.districts = Collections.emptyList();
    this.states = Collections.emptyList();
  }

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

  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<Integer> getDistricts() {
    return districts;
  }

  public void setDistricts(List<Integer> districts) {
    this.districts = districts;
  }

  public List<String> getStates() {
    return states;
  }

  public void setStates(List<String> states) {
    this.states = states;
  }
}
