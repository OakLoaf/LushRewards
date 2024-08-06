package org.lushplugins.lushrewards.utils.placeholder;

import org.bukkit.entity.Player;

public class SimplePlaceholder extends Placeholder {
    private final LocalPlaceholders.PlaceholderFunction method;

    public SimplePlaceholder(String content, LocalPlaceholders.PlaceholderFunction method) {
        super(content);
        this.method = method;
    }

    @Override
    boolean matches(String string) {
        return string.startsWith(content);
    }

    @Override
    public String parse(String[] params, Player player) {
        try {
            return method.apply(params, player);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public SimplePlaceholder addChild(Placeholder placeholder) {
        super.addChild(placeholder);
        return this;
    }
}
