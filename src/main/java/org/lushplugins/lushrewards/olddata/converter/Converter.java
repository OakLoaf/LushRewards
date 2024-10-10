package org.lushplugins.lushrewards.olddata.converter;

public abstract class Converter {
    private final String name;

    public Converter(String name) {
        this.name = name;
    }

    public abstract boolean convert();

    public String getName() {
        return name;
    }
}
