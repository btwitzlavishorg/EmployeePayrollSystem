package com.payroll.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class DateTimeUtilTest {

    @Test
    public void daysInFebruaryLeapYear() {
        assertEquals(29, DateTimeUtil.getDaysInMonth(2, 2024));
    }

    @Test
    public void daysInFebruaryNonLeapYear() {
        assertEquals(28, DateTimeUtil.getDaysInMonth(2, 2023));
    }

    @Test
    public void monthNameIsEnglish() {
        assertEquals("January", DateTimeUtil.getMonthName(1));
        assertEquals("December", DateTimeUtil.getMonthName(12));
    }
}
