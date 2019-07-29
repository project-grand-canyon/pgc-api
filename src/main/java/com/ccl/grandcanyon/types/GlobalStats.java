package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.YearMonth;
import java.util.SortedMap;

public class GlobalStats {

  private int totalCalls;
  private int totalCallers;
  private int recentDayCount;
  private int totalRecentCalls;

  private SortedMap<YearMonth, Integer> callsByMonth;
  private SortedMap<YearMonth, Integer> callersByMonth;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private SortedMap<YearMonth, Integer> activeCallersByMonth;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private SortedMap<YearMonth, Integer> remindersByMonth;

  public int getTotalCalls() {
    return totalCalls;
  }

  public void setTotalCalls(int totalCalls) {
    this.totalCalls = totalCalls;
  }

  public int getTotalCallers() {
    return totalCallers;
  }

  public void setTotalCallers(int totalCallers) {
    this.totalCallers = totalCallers;
  }

  public int getRecentDayCount() {
    return recentDayCount;
  }

  public void setRecentDayCount(int recentDayCount) {
    this.recentDayCount = recentDayCount;
  }

  public int getTotalRecentCalls() {
    return totalRecentCalls;
  }

  public void setTotalRecentCalls(int totalRecentCalls) {
    this.totalRecentCalls = totalRecentCalls;
  }

  public SortedMap<YearMonth, Integer> getCallsByMonth() {
    return callsByMonth;
  }

  public void setCallsByMonth(SortedMap<YearMonth, Integer> callsByMonth) {
    this.callsByMonth = callsByMonth;
  }

  public SortedMap<YearMonth, Integer> getCallersByMonth() {
    return callersByMonth;
  }

  public void setCallersByMonth(SortedMap<YearMonth, Integer> callersByMonth) {
    this.callersByMonth = callersByMonth;
  }

  public SortedMap<YearMonth, Integer> getActiveCallersByMonth() {
    return activeCallersByMonth;
  }

  public void setActiveCallersByMonth(SortedMap<YearMonth, Integer> activeCallersByMonth) {
    this.activeCallersByMonth = activeCallersByMonth;
  }

  public SortedMap<YearMonth, Integer> getRemindersByMonth() {
    return remindersByMonth;
  }

  public void setRemindersByMonth(SortedMap<YearMonth, Integer> remindersByMonth) {
    this.remindersByMonth = remindersByMonth;
  }
}
