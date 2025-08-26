package dev.sxmurxy.mre.modules.settings.impl;

import dev.sxmurxy.mre.UnnsenseClient;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.settings.ISetting;

public class BoolSetting implements ISetting<Boolean> {
    public BoolSetting(String name, Module module, boolean value) {
        this.name = name;
        this.value = value;
        this.module = module;
        UnnsenseClient.getInstance().settingManager.settings.add(this);
    }

    public String name;

    public Module module;

    public boolean value;

    @Override
    public Boolean get() {
        return value;
    }

    @Override
    public void set(Boolean value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Module getModule() {
        return module;
    }
}