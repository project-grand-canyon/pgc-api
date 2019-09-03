package com.ccl.grandcanyon.utils;

public class DayOfMonthFormatter {

    public static String getAdjective(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            String message = String.format("%s is not a day of the month", dayOfMonth);
            throw new NonexistentDayOfMonthException(message);
        }

        if (dayOfMonth == 1 || dayOfMonth == 21 || dayOfMonth == 31) {
            return String.format("%sst", dayOfMonth);
        }

        if (dayOfMonth == 2 || dayOfMonth == 22) {
            return String.format("%snd", dayOfMonth);
        }

        if (dayOfMonth == 3 || dayOfMonth == 23) {
            return String.format("%srd", dayOfMonth);
        }

        return String.format("%sth", dayOfMonth);
    }

}
