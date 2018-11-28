package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.Timestamp;

public class TalkingPoint {

  private int talkingPointId;
  private int themeId;
  private String content;
  private Timestamp created;
  private Timestamp lastModified;

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

  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getCreated() {
    return created;
  }


  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getLastModified() {
    return lastModified;
  }

}
