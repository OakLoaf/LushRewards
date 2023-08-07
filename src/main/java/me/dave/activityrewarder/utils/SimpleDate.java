package me.dave.activityrewarder.utils;

import org.jetbrains.annotations.Nullable;

public class SimpleDate {
    private int day;
    private int month;
    private int year;

    private SimpleDate(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public String asString(String format) {
        return format
                .replaceAll("dd", String.valueOf(day))
                .replaceAll("mm", String.valueOf(month))
                .replaceAll("yyyy", String.valueOf(year));
    }

    @Nullable
    private static SimpleDate from(int day, int month) {
        if ((day < 0 || day > 31) || (month < 0 || month > 12)) return null;
        return new SimpleDate(day, month, 1994);
    }

    @Nullable
    private static SimpleDate from(int day, int month, int year) {
        if ((day < 0 || day > 31) || (month < 0 || month > 12) || (year < 0)) return null;
        return new SimpleDate(day, month, year);
    }
}
