package com.payroll.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateTimeUtil {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    public static final DateTimeFormatter SQL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public static String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }

    public static String getMonthName(int month) {
        if (month < 1 || month > 12) return "Invalid Month";
        return java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public static int getDaysInMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.lengthOfMonth();
    }

    /**
     * Estimates standard standard business working days (excluding Sundays) in a month.
     */
    public static int getWorkingDaysInMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int totalDays = yearMonth.lengthOfMonth();
        int workingDays = 0;
        for (int day = 1; day <= totalDays; day++) {
            LocalDate date = yearMonth.atDay(day);
            if (date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                workingDays++;
            }
        }
        return workingDays;
    }
}
