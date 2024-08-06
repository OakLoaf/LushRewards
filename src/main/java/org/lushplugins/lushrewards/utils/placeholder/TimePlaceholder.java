package org.lushplugins.lushrewards.utils.placeholder;

import org.bukkit.entity.Player;
import org.lushplugins.lushrewards.LushRewards;

import java.util.List;
import java.util.logging.Level;

public class TimePlaceholder extends Placeholder {
    private final TimePlaceholderFunction method;

    public TimePlaceholder(String content, TimePlaceholderFunction method) {
        super(content);
        this.method = method;
    }

    @Override
    boolean matches(String string) {
        return string.startsWith(content)
            || string.startsWith(content + "_minutes")
            || string.startsWith(content + "_hours")
            || string.startsWith(content + "_seconds");
    }

    @Override
    public String parse(String[] params, Player player) {
        try {
            Integer seconds = method.apply(params, player);
            if (seconds == null) {
                return null;
            }

            List<String> paramsList = List.of(params);
            if (paramsList.contains("seconds")) {
                return String.valueOf(seconds);
            } else if (paramsList.contains("hours")) {
                return String.valueOf((int) Math.floor((seconds / 60D) / 60D));
            } else {
                return String.valueOf((int) Math.floor(seconds / 60D));
            }
        } catch(Exception e) {
            LushRewards.getInstance().log(Level.WARNING, "Caught error whilst parsing time placeholder:", e);
            return null;
        }
    }

    @Override
    public TimePlaceholder addChild(Placeholder placeholder) {
        super.addChild(placeholder);
        return this;
    }

    @FunctionalInterface
    public interface TimePlaceholderFunction {

        /**
         * @param params Parameters
         * @param player Player
         * @return Time value (in seconds)
         */
        Integer apply(String[] params, Player player) ;
    }
}
