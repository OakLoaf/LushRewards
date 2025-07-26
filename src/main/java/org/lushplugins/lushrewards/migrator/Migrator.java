package org.lushplugins.lushrewards.migrator;

public abstract class Migrator {
    private final String name;

    public Migrator(String name) {
        this.name = name;
    }

    public abstract boolean convert();

    public String getName() {
        return name;
    }
}
