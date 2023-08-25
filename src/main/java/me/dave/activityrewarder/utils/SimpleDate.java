package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.exceptions.SimpleDateParseException;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDate implements Cloneable {
    private static final Pattern DATE_FORMAT = Pattern.compile("([0-9]{1,2})/([0-9]{1,2})/([0-9]{4,})");
    private static final int[] DAYS_PER_MONTH = new int[]{ 31, 29 /* 28 on all years except leap years */, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    private int day;
    private int month;
    private int year;

    public SimpleDate(int day, int month, int year) {
        if ((day < 0 || (month < 0 || month > 12) || year < 0) || day > DAYS_PER_MONTH[month - 1]) {
            throw new SimpleDateParseException("Invalid date");
        } else if (day == 29 && month == 2 && year % 4 != 0) {
            throw new SimpleDateParseException("Invalid date");
        } else {
            this.day = day;
            this.month = month;
            this.year = year;
        }
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public void addDays(int days) {
        days += this.day;
        int months = 0;

        for (int monthIndex = this.month - 1; days > DAYS_PER_MONTH[monthIndex] || (days > 28 && monthIndex == 1 && year % 4 == 0); monthIndex++) {
            months++;
            days -= DAYS_PER_MONTH[monthIndex];
        }
        addMonths(months);

        if (!validateDate(days, month, year)) throw new SimpleDateParseException("Invalid date");

        this.day = days;
    }

    public void addMonths(int months) {
        int years = this.year + (int) Math.floor(months / (float) 12);
        months = (months + this.month) % 12;
        int days = Math.min(this.day, DAYS_PER_MONTH[months - 1]);

        if (!validateDate(days, months, years)) throw new SimpleDateParseException("Invalid date");

        this.month = months;
        this.year = years;
        this.day = days;
    }

    public void addYears(int years) {
        if (!validateDate(day, month, year + years)) throw new SimpleDateParseException("Invalid date");

        this.year += years;
    }

    public boolean validateDate() {
        return validateDate(day, month, year);
    }

    public boolean validateDate(int day, int month, int year) {
        if ((day < 0 || (month < 0 || month > 12) || year < 0) || day > DAYS_PER_MONTH[month - 1]) {
            return false;
        } else if (day != 29 || month != 2 || year % 4 == 0) {
            return true;
        } else {
            return false;
        }
    }

    public String asString(String format) {
        return format
                .replaceAll("dd", String.valueOf(day))
                .replaceAll("mm", String.valueOf(month))
                .replaceAll("yyyy", String.valueOf(year));
    }

    public static SimpleDate from(@NotNull String string) {
        Matcher matcher= DATE_FORMAT.matcher(string);

        if (matcher.find()) {
            return new SimpleDate(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        } else {
            throw new SimpleDateParseException("Invalid date format found");
        }
    }

    public static SimpleDate now() {
        LocalDate localDate = LocalDate.now();
        return new SimpleDate(localDate.getDayOfMonth(), localDate.getMonthValue(), localDate.getYear());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDate that = (SimpleDate) o;
        return day == that.day && month == that.month && year == that.year;
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, month, year);
    }

    @Override
    public SimpleDate clone() {
        try {
            return (SimpleDate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
