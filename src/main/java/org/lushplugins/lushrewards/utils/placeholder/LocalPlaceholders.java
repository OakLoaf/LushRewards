package org.lushplugins.lushrewards.utils.placeholder;

import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

// TODO: Migrate to PlaceholderHandler
public class LocalPlaceholders {
    private final ConcurrentHashMap<String, Placeholder> placeholders = new ConcurrentHashMap<>();

    public LocalPlaceholders() {}

    public String parsePlaceholder(String params, Player player) {
        String[] paramsArr = params.split("_");

        Placeholder currentPlaceholder = null;
        String currParams = params;
        for (int i = 0; i < paramsArr.length; i++) {
            boolean found = false;

            for (Placeholder subPlaceholder : currentPlaceholder != null ? currentPlaceholder.getChildren() : placeholders.values()) {
                if (subPlaceholder.matches(currParams)) {
                    currentPlaceholder = subPlaceholder;
                    currParams = currParams.replace(subPlaceholder.getContent() + "_", "");

                    found = true;
                    break;
                }
            }

            if (!found) {
                break;
            }
        }

        if (currentPlaceholder != null) {
            try {
                return currentPlaceholder.parse(paramsArr, player);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public void registerPlaceholder(Placeholder placeholder) {
        placeholders.put(placeholder.getContent(), placeholder);
    }

    public void unregisterPlaceholder(String content) {
        placeholders.remove(content);
    }

    @FunctionalInterface
    public interface PlaceholderFunction {
        String apply(String[] params, Player player) ;
    }
}
