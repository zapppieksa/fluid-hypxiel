package dev.sxmurxy.mre.modules.settings.impl;

import dev.sxmurxy.mre.UnnsenseClient;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.settings.ISetting;

public class NumberSetting implements ISetting<Double> {
    public double value, min, max;
    public boolean onlyint;
    public String name;
    public Module m;

    public NumberSetting(String name, Module m, double value, double min, double max, boolean onlyint) {
        this.name = name;
        this.m = m;
        this.value = value;
        this.min = min;
        this.max = max;
        this.onlyint = onlyint;
        UnnsenseClient.getInstance().settingManager.settings.add(this);
    }

    public NumberSetting(String pathSmoothness, double v, double value, double min, double max) {
    }

    @Override
    public Double get() {
        return value;
    }

    @Override
    public void set(Double value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Module getModule() {
        return m;
    }

    public void setValue(double max) {
    }

    public Object getValue() {
    }
}