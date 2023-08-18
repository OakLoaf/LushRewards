package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.exceptions.SimpleDateParseException;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDate {
    private static final Pattern DATE_FORMAT = Pattern.compile("([0-9]{1,2})/([0-9]{1,2})/([0-9]{4,})");
    private int day;
    private int month;
    private int year;

    private SimpleDate(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    // TODO: Needs to respect ends of months (and leap years)
    public void addDays(int days) {
        boolean negative = days < 0;

        int years = (int) Math.floor(days / 365.0);
        this.year += years;

        if (!negative) {
            this.day += days % 365;
        } else {
            this.day -= days % 365;
        }
    }

    // TODO: Needs to respect ends of years
    public void addMonths(int months) {
        this.month += months;
    }

    public void addYears(int years) {
        this.year += years;
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
            return new SimpleDate(Integer.parseInt(matcher.group(0)), Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        } else {
            throw new SimpleDateParseException("Invalid date format found");
        }
    }

    public static SimpleDate from(int day, int month, int year) {
        if ((day < 0 || day > 31) || (month < 0 || month > 12) || (year < 0)) {
            throw new SimpleDateParseException("Invalid date");
        } else {
            return new SimpleDate(day, month, year);
        }
    }

    public static SimpleDate now() {
        LocalDate localDate = LocalDate.now();
        return new SimpleDate(localDate.getDayOfMonth(), localDate.getMonthValue(), localDate.getYear());
    }
}
