package me.dave.activityrewarder.gui;

import me.dave.activityrewarder.ActivityRewarder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiFormat {
    private final String title;
    private final GuiTemplate template;

    public GuiFormat(String title, GuiTemplate template) {
        this.title = title;
        this.template = template;
    }

    public String getTitle() {
        return title;
    }

    public GuiTemplate getTemplate() {
        return template;
    }


    public static class GuiTemplate {
        private final List<String> rows;

        //   Format:
//     # - Border
//     R - Reward
//     N - Upcoming Reward
        public GuiTemplate() {
            this.rows = new ArrayList<>();
        }

        public GuiTemplate(String[] rows) {
            this(Arrays.asList(rows));
        }

        public GuiTemplate(List<String> rows) {
            if (rows.size() == 0) {
                ActivityRewarder.getInstance().getLogger().warning("Failed to load template, no lines detected.");
                this.rows = new ArrayList<>();
                return;
            }

            for (String row : rows) {
                if (row.length() != 9) {
                    ActivityRewarder.getInstance().getLogger().warning("Failed to load template, lines must be 9 characters long.");
                    this.rows = new ArrayList<>();
                    return;
                }
            }

            this.rows = rows;
        }

        public void setRow(int row, String format) {
            if (format.length() != 9) {
                ActivityRewarder.getInstance().getLogger().warning("Failed to load row format, lines must be 9 characters long.");
                return;
            }
            rows.set(row, format);
        }

        public void addRow(String format) {
            if (format.length() != 9) {
                ActivityRewarder.getInstance().getLogger().warning("Failed to load row format, lines must be 9 characters long.");
                return;
            }
            rows.add(format);
        }

        public int getRowCount() {
            return rows.size();
        }

        public char getCharAt(int slot) {
            int currRow = (int) Math.ceil((slot + 1) / 9F) - 1;
            int slotInRow = slot % 9;

            return rows.get(currRow).charAt(slotInRow);
        }

        public int countChar(char character) {
            int count = 0;

            for (String row : rows) {
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == character) {
                        count++;
                    }
                }
            }

            return count;
        }

        public static class DefaultTemplate {

            private static final String[] DEFAULT = new String[]{
                "#########",
                "RRRRRRR#N",
                "#########"
            };

            private static final String[] COMPACT = new String[]{
                "RRRRRRR N"
            };

            private static final String[] COMPACT_PROFILE = new String[]{
                "P RRRRRRR"
            };

            private static final String[] BORDERED_LARGE = new String[]{
                "#########",
                "#RRRRRRR#",
                "#RRRRRRR#",
                "#RRRRRRR#",
                "#RRRRRRR#",
                "####P####"
            };

            private static final String[] DAILY_REWARDS_PLUS = new String[]{
                "RRRRRRRRR",
                "RRRRRRRRR",
                "#RRRRRRR#",
                "##RRRRR##",
                "#########",
                "####P####"
            };

            private static final String[] NDAILY_REWARDS = new String[]{
                "#########",
                "#RRRRRRR#",
                "#########"
            };

            public static GuiTemplate valueOf(String string) {
                switch (string.toUpperCase()) {
                    case "DEFAULT" -> {
                        return new GuiTemplate(DEFAULT);
                    }
                    case "COMPACT" -> {
                        return new GuiTemplate(COMPACT);
                    }
                    case "COMPACT_PROFILE" -> {
                        return new GuiTemplate(COMPACT_PROFILE);
                    }
                    case "BORDERED_LARGE" -> {
                        return new GuiTemplate(BORDERED_LARGE);
                    }
                    case "DAILY_REWARDS_PLUS" -> {
                        return new GuiTemplate(DAILY_REWARDS_PLUS);
                    }
                    case "NDAILY_REWARDS" -> {
                        return new GuiTemplate(NDAILY_REWARDS);
                    }
                    default -> {
                        ActivityRewarder.getInstance().getLogger().warning("Invalid template type, setting to default.");
                        return new GuiTemplate(DEFAULT);
                    }
                }
            }
        }
    }
}
