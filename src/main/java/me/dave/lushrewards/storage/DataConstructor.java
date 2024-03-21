package me.dave.lushrewards.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataConstructor {

    /**
     * Data to be loaded from a database
     */
    public static class Loadable {
        private static final Pattern LIST_PATTERN = Pattern.compile("([^\"]+)");

        public static final Function<String, String> STRING = (obj) -> obj;
        public static final Function<Integer, Integer> INTEGER = (obj) -> obj;
        public static final Function<Boolean, Boolean> BOOLEAN = (obj) -> obj;
        public static final Function<Double, Double> DOUBLE = (obj) -> obj;
        public static final Function<Long, Long> LONG = (obj) -> obj;
        public static final Function<String, List<String>> LIST_STRING = (listRaw) -> {
            if (listRaw == null) {
                return Collections.emptyList();
            }

            Matcher matcher = LIST_PATTERN.matcher(listRaw);

            ArrayList<String> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(matcher.group());
            }

            return matches;
        };
        public static final Function<String, LocalDate> DATE = (dateRaw) -> dateRaw != null && !dateRaw.isEmpty() ? LocalDate.parse(dateRaw, DateTimeFormatter.ofPattern("dd-MM-yyyy")) : null;
    }

    /**
     * Data to be saved to a database (Types must be translated to an instance of String, Integer, Boolean, Double or Long)
     */
    public static class Savable {
        public static final Function<String, String> STRING = (obj) -> obj;
        public static final Function<Integer, Integer> INTEGER = (obj) -> obj;
        public static final Function<Boolean, Boolean> BOOLEAN = (obj) -> obj;
        public static final Function<Double, Double> DOUBLE = (obj) -> obj;
        public static final Function<Long, Long> LONG = (obj) -> obj;
        public static final Function<List<String>, String> LIST_STRING = (list) -> '"' + String.join("\",\"", list != null ? list : new ArrayList<>()) + '"';
        public static final Function<LocalDate, String> DATE = (date) -> date != null ? date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
    }
}
