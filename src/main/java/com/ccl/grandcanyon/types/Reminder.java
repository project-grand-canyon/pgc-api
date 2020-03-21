package com.ccl.grandcanyon.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Reminder {

  // SQL column names
  public static final String CALLER_ID = "caller_id";
  public static final String DAY_OF_MONTH = "day_of_month";
  public static final String LAST_REMINDER_TIMESTAMP = "last_reminder_timestamp";
  public static final String SECOND_REMINDER_SENT = "second_reminder_timestamp";
  public static final String TRACKING_ID = "tracking_id";

  // These are the year/month that the reminder is for and don't necessarily reflect
  // the reminder timestamps
  public static final String REMINDER_YEAR = "reminder_year";
  public static final String REMINDER_MONTH = "reminder_month";

  private int callerId;
  private int dayOfMonth;
  private Timestamp lastReminderTimestamp;
  private Timestamp secondReminderTimestamp;
  private String trackingId;
  private int reminderYear;
  private int reminderMonth;

  public Reminder(ResultSet rs) throws SQLException {
    this.callerId = rs.getInt(CALLER_ID);
    this.dayOfMonth = rs.getInt(DAY_OF_MONTH);
    this.lastReminderTimestamp = rs.getTimestamp(LAST_REMINDER_TIMESTAMP);
    this.secondReminderTimestamp = rs.getTimestamp(SECOND_REMINDER_SENT);
    this.trackingId = rs.getString(TRACKING_ID);
    this.reminderYear = rs.getInt(REMINDER_YEAR);
    this.reminderMonth = rs.getInt(REMINDER_MONTH);
  }

  public int getCallerId() {
    return callerId;
  }

  public void setCallerId(int callerId) {
    this.callerId = callerId;
  }

  public int getDayOfMonth() {
    return dayOfMonth;
  }

  public void setDayOfMonth(int dayOfMonth) {
    this.dayOfMonth = dayOfMonth;
  }

  public Timestamp getLastReminderTimestamp() {
    return lastReminderTimestamp;
  }

  public void setLastReminderTimestamp(Timestamp lastReminderTimestamp) {
    this.lastReminderTimestamp = lastReminderTimestamp;
  }

  public Timestamp getSecondReminderTimestamp() {
    return secondReminderTimestamp;
  }

  public void setSecondReminderTimestamp(Timestamp secondReminderTimestamp) {
    this.secondReminderTimestamp = secondReminderTimestamp;
  }

  public String getTrackingId() {
    return trackingId;
  }

  public void setTrackingId(String trackingId) {
    this.trackingId = trackingId;
  }

  public int getReminderYear() {
    return reminderYear;
  }

  public void setReminderYear(int reminderYear) {
    this.reminderYear = reminderYear;
  }

  public int getReminderMonth() {
    return reminderMonth;
  }

  public void setReminderMonth(int reminderMonth) {
    this.reminderMonth = reminderMonth;
  }

  public Boolean hasBeenSent(ReminderDate reminderDate) {
    assert (reminderDate != null);
    // This will only be null if a reminder has never been sent
    if (getLastReminderTimestamp() == null) {
      return false;
    }

    // Note that REMINDER_YEAR and REMINDER_MONTH are updated when the reminder is sent,
    // so this won't result in duplicate reminders being sent
    return reminderDate.getYear() < getReminderYear() ||
            reminderDate.getYear() == getReminderYear() && reminderDate.getMonth() <= getReminderMonth();
  }
}
