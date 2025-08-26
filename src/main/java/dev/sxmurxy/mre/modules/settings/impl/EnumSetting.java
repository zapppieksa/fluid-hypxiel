package dev.sxmurxy.mre.modules.settings.impl;

import dev.sxmurxy.mre.UnnsenseClient;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.settings.ISetting;

import java.util.Arrays;
import java.util.List;

public class EnumSetting implements ISetting<String> {
    public String value; // Changed to private for encapsulation
    public final String name;
    public final List<String> modes;
    public final Module module;
    public boolean expanded = false; // Unused in current dropdown implementation, retained for compatibility

    public EnumSetting(String name, Module module, String defaultValue, String... modes) {
        this.name = name;
        this.module = module;
        this.modes = Arrays.asList(modes);
        // Validate defaultValue
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default value '" + defaultValue + "' is not in modes: " + this.modes);
        }
        this.value = defaultValue;
        UnnsenseClient.getInstance().settingManager.settings.add(this);
    }

    public boolean is(String s) {
        return value.equalsIgnoreCase(s);
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public void set(String value) {
        if (modes.contains(value)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Value '" + value + "' is not a valid mode for setting '" + name + "'");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Module getModule() {
        return module;
    }

    public void next() {
        int currentIndex = modes.indexOf(value);
        int nextIndex = (currentIndex + 1) % modes.size();
        value = modes.get(nextIndex);
    }

    public String[] getOptions() {
        return modes.toArray(new String[0]);
    }
}