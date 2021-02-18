package com.ccl.grandcanyon.types;

import com.ccl.grandcanyon.types.District;

import java.util.List;

public class Message {

  private String subject;
  private String body;
  private List<District> targets;

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public List<District> getTargetDistricts() {
    return targets;
  }
  
  public void addTargetDistrict(District targetDistrict) {
    this.targets.add(targetDistrict);
  }
}
