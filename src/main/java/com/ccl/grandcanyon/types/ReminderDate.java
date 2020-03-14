package com.ccl.grandcanyon.types;

import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;

public class ReminderDate {
    // Since a caller's call day might not exist in a given month, ReminderDate is used to represent
    // the date that a reminder is for, but not necessarily sent on.
    // e.g. This class can represent the reminder date Feb 30, which might be sent on Feb 28.

    public final static Integer MIN_DAY = 1;
    public final static Integer MAX_DAY = 31;

    Integer year;
    Integer month;
    Integer day; // Doesn't have to be a valid day in this year/month

    private ReminderDate(Builder builder) {
        this.year = builder.year;
        this.month = builder.month;
        this.day = builder.day;
    }

    public ReminderDate(LocalDate date) {
        this.year = date.getYear();
        this.day = date.getDayOfMonth();
        this.month = date.getMonth().getValue();
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getDay() {
        return day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReminderDate reminderDate = (ReminderDate) o;
        return year.equals(reminderDate.year) && month.equals(reminderDate.month) && day.equals(reminderDate.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    public static class Builder {
        Integer year;
        Integer month;
        Integer day; // Doesn't have to be a valid day in this year/month

        public Builder year(Integer year) {
            this.year = year;
            return this;
        }

        public Builder month(Integer month) {
            this.month = month;
            return this;
        }

        public Builder month(Month month) {
            this.month = month.getValue();
            return this;
        }

        public Builder day(Integer day) {
            this.day = day;
            return this;
        }

        public ReminderDate build() {
            return new ReminderDate(this);
        }
    }
}

