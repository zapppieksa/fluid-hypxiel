package dev.sxmurxy.mre.modules.settings.impl;


import dev.sxmurxy.mre.UnnsenseClient;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.settings.ISetting;

public class TextSetting implements ISetting<String> {
    private String name;
    private Module module;
    public String value;

    public TextSetting(String name, Module module, String value) {
        this.name = name;
        this.module = module;
        this.value = value;
        UnnsenseClient.getInstance().settingManager.settings.add(this);
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public void set(String value) {
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
