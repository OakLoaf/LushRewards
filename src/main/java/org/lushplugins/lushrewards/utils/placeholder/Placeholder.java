package org.lushplugins.lushrewards.utils.placeholder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public abstract class Placeholder {
    protected final String content;
    private Collection<Placeholder> children;

    public Placeholder(String content) {
        this.content = content;
    }

    abstract boolean matches(String string);

    abstract String parse(String[] params, Player player);

    public String getContent() {
        return content;
    }

    @NotNull
    public Collection<Placeholder> getChildren() {
        return children != null ? children : Collections.emptyList();
    }

    public Placeholder addChild(Placeholder placeholder) {
        if (children == null) {
            children = new ArrayList<>();
        }

        children.add(placeholder);
        return this;
    }
}
