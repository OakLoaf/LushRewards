package me.dave.activityrewarder.utils;

import org.bukkit.Location;

import java.util.Objects;

public class SimpleLocation {
    private final double x;
    private final double y;
    private final double z;

    public SimpleLocation(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleLocation that = (SimpleLocation) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Double.compare(that.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public static SimpleLocation from(Location location) {
        return new SimpleLocation(location.getX(), location.getY(), location.getZ());
    }
}
