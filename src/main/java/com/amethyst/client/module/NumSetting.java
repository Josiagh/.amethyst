package com.amethyst.client.module;

public class NumSetting {
    public final String name;
    public double value, min, max;
    public NumSetting(String name, double value, double min, double max) {
        this.name = name; this.value = value; this.min = min; this.max = max;
    }
}
