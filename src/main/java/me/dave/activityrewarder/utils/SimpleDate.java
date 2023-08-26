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
        if ((day <= 0 || (month <= 0 || month > 12) || year < 0) || day > DAYS_PER_MONTH[month - 1]) {
            throw new SimpleDateParseException(day + "/" + month + "/" + year + " is not a valid date");
        } else if (day == 29 && month == 2 && year % 4 != 0) {
            throw new SimpleDateParseException(day + "/" + month + "/" + year + " is not a valid date");
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

    public void addDays(int daysToAdd) {
        if (daysToAdd < 0) {
            minusDays(-daysToAdd);
            return;
        }

        daysToAdd += this.day;
        int months = 0;

        for (int monthIndex = this.month - 1; daysToAdd > DAYS_PER_MONTH[monthIndex] || (daysToAdd > 28 && monthIndex == 1 && year % 4 == 0); monthIndex++) {
            months++;
            daysToAdd -= DAYS_PER_MONTH[monthIndex];
        }
        addMonths(months);

        if (!validateDate(daysToAdd, month, year)) throw new SimpleDateParseException(daysToAdd + "/" + month + "/" + year + " is not a valid date");

        this.day = daysToAdd;
    }

    public void minusDays(int daysToSubtract) {
        daysToSubtract = daysToSubtract < 0 ? -daysToSubtract : daysToSubtract;
        int months = 0;

        if (daysToSubtract > this.day) {
            minusMonths(1);
            daysToSubtract -= this.day;
        }

        for (int monthIndex = this.month - 1; daysToSubtract > DAYS_PER_MONTH[monthIndex] || (daysToSubtract > 28 && monthIndex == 1 && year % 4 == 0); monthIndex--) {
            months++;
            daysToSubtract -= DAYS_PER_MONTH[monthIndex];
        }

        minusMonths(months);
        int days = DAYS_PER_MONTH[month] - daysToSubtract;

        if (!validateDate(days, month, year)) throw new SimpleDateParseException(daysToSubtract + "/" + month + "/" + year + " is not a valid date");

        this.day = days;
    }

    public void addMonths(int monthsToAdd) {
        if (monthsToAdd < 0) {
            minusMonths(-monthsToAdd);
            return;
        }

        int years = this.year + (int) Math.floor(monthsToAdd / (float) 12);
        monthsToAdd = (monthsToAdd + this.month) % 12;
        int days = Math.min(this.day, DAYS_PER_MONTH[monthsToAdd - 1]);

        if (!validateDate(days, monthsToAdd, years)) throw new SimpleDateParseException(days + "/" + monthsToAdd + "/" + years + " is not a valid date");

        this.month = monthsToAdd;
        this.year = years;
        this.day = days;
    }

    public void minusMonths(int monthsToSubtract) {
        int years = this.year - (int) Math.floor(monthsToSubtract / (float) 12);
        monthsToSubtract = (this.month - (monthsToSubtract < 0 ? -monthsToSubtract : monthsToSubtract)) % 12;
        int days = Math.min(this.day, DAYS_PER_MONTH[monthsToSubtract - 1]);

        if (!validateDate(days, monthsToSubtract, years)) throw new SimpleDateParseException(days + "/" + monthsToSubtract + "/" + years + " is not a valid date");

        this.month = monthsToSubtract;
        this.year = years;
        this.day = days;
    }

    public void addYears(int yearsToAdd) {
        if (!validateDate(day, month, year + yearsToAdd)) throw new SimpleDateParseException(day + "/" + month + "/" + (year + yearsToAdd) + " is not a valid date");

        this.year += yearsToAdd;
    }

    public void minusYears(int yearsToSubtract) {
        addYears(-yearsToSubtract);
    }

    public boolean validateDate(int day, int month, int year) {
        if ((day <= 0 || (month <= 0 || month > 12) || year < 0) || day > DAYS_PER_MONTH[month - 1]) {
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
