package dev.sxmurxy.mre.modules.settings.impl;

import dev.sxmurxy.mre.modules.Module;

import java.awt.*;

public class ColorSetting {
    private String name;
    private Module module;
    private Color value;

    // Constructor to initialize the ColorSetting with a name, module, and default color
    public ColorSetting(String name, Module module, Color defaultColor) {
        this.name = name;
        this.module = module;
        this.value = defaultColor;
    }

    // Method to retrieve the current color value
    public Color get() {
        return value;
    }

    // Optional: Method to set a new color value if needed
    public void set(Color newColor) {
        this.value = newColor;
    }
}