package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.exceptions.SimpleDateParseException;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDate implements Cloneable {
    /**
     * Default date format "dd-mm-yyyy".
     */
    private static final Pattern DATE_FORMAT = Pattern.compile("([0-9]{1,2})-([0-9]{1,2})-([0-9]{4,})");
    /**
     * Maximum number of days in each month.
     */
    private static final int[] DAYS_PER_MONTH = new int[]{ 31, 29 /* 28 on all years except leap years */, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    /**
     * The number of days in a 400-year cycle.
     */
    private static final int DAYS_PER_CYCLE = 146097;
    /**
     * The number of days from year zero to year 1970.
     * There are five 400 year cycles from year zero to 2000.
     * There are 7 leap years from 1970 to 2000.
     */
    private static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

    private int day;
    private int month;
    private int year;

    public SimpleDate(int day, int month, int year) {
        if ((day <= 0 || (month <= 0 || month > 12) || year < 0) || day > DAYS_PER_MONTH[month - 1]) {
            throw new SimpleDateParseException(day + "/" + month + "/" + year + " is not a valid date");
        } else if (day == 29 && month == 2 && !isLeapYear()) {
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

    public boolean isLeapYear() {
        return year % 4 == 0;
    }

    public boolean isBefore(SimpleDate date) {
        return !isAfter(date);
    }

    public boolean isAfter(SimpleDate date) {
        if (date.year > this.year) {
            return true;
        } else if (date.year == this.year) {
            if (date.month > this.month) {
                return true;
            } else if (date.month == this.month) {
                return date.day > this.day;
            }
        }

        return false;
    }

    public SimpleDate addDays(int daysToAdd) {
        if (daysToAdd < 0) {
            return minusDays(-daysToAdd);
        }

        daysToAdd += this.day;
        int months = 0;

        for (int monthIndex = this.month - 1; daysToAdd > DAYS_PER_MONTH[monthIndex] || (daysToAdd > 28 && monthIndex == 1 && isLeapYear()); monthIndex++) {
            months++;
            daysToAdd -= DAYS_PER_MONTH[monthIndex];
        }
        addMonths(months);

        if (!validateDate(daysToAdd, month, year)) throw new SimpleDateParseException(daysToAdd + "/" + month + "/" + year + " is not a valid date");

        this.day = daysToAdd;

        return this;
    }

    public SimpleDate minusDays(int daysToSubtract) {
        daysToSubtract = daysToSubtract < 0 ? -daysToSubtract : daysToSubtract;
        int months = 0;

        if (daysToSubtract > this.day) {
            minusMonths(1);
            daysToSubtract -= this.day;
        }

        for (int monthIndex = this.month - 1; daysToSubtract > DAYS_PER_MONTH[monthIndex] || (daysToSubtract > 28 && monthIndex == 1 && isLeapYear()); monthIndex--) {
            months++;
            daysToSubtract -= DAYS_PER_MONTH[monthIndex];
        }

        minusMonths(months);
        int days = DAYS_PER_MONTH[month] - daysToSubtract;
        if (days == 0) {
            minusMonths(1);
            days = DAYS_PER_MONTH[month];
        }

        if (!validateDate(days, month, year)) throw new SimpleDateParseException(days + "/" + month + "/" + year + " is not a valid date");

        this.day = days;

        return this;
    }

    public SimpleDate addMonths(int monthsToAdd) {
        if (monthsToAdd < 0) {
            return minusMonths(monthsToAdd);
        }

        int years = this.year + (int) Math.floor(monthsToAdd / (float) 12);
        int months = (monthsToAdd + this.month) % 12;
        int days = Math.min(this.day, DAYS_PER_MONTH[months - 1]);

        if (!validateDate(days, months, years)) throw new SimpleDateParseException(days + "/" + months + "/" + years + " is not a valid date");

        this.month = months;
        this.year = years;
        this.day = days;

        return this;
    }

    public SimpleDate minusMonths(int monthsToSubtract) {
        int years = this.year - (int) Math.floor(monthsToSubtract / (float) 12);
        monthsToSubtract = (this.month - (monthsToSubtract < 0 ? -monthsToSubtract : monthsToSubtract)) % 12;
        int days = Math.min(this.day, DAYS_PER_MONTH[monthsToSubtract - 1]);

        if (!validateDate(days, monthsToSubtract, years)) throw new SimpleDateParseException(days + "/" + monthsToSubtract + "/" + years + " is not a valid date");

        this.month = monthsToSubtract;
        this.year = years;
        this.day = days;

        return this;
    }

    public SimpleDate addYears(int yearsToAdd) {
        if (!validateDate(day, month, year + yearsToAdd)) throw new SimpleDateParseException(day + "/" + month + "/" + (year + yearsToAdd) + " is not a valid date");

        this.year += yearsToAdd;

        return this;
    }

    public SimpleDate minusYears(int yearsToSubtract) {
        return addYears(-yearsToSubtract);
    }

    public boolean validateDate(int day, int month, int year) {
        if ((day <= 0 || (month <= 0 || month > 12) || year < 0) || day > DAYS_PER_MONTH[month - 1]) {
            return false;
        } else if (day != 29 || month != 2 || isLeapYear()) {
            return true;
        } else {
            return false;
        }
    }

    public long toEpochDay() {
        long y = year;
        long m = month;
        long total = 0;
        total += 365 * y;
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        total += ((367 * m - 362) / 12);
        total += day - 1;
        if (m > 2) {
            total--;
            if (!isLeapYear()) {
                total--;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    public String toString(String format) {
        return format
                .replaceAll("dd", String.valueOf(day))
                .replaceAll("mm", String.valueOf(month))
                .replaceAll("yyyy", String.valueOf(year));
    }

    public static SimpleDate parse(@NotNull String string) {
        string = string.replace('/', '-');
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
