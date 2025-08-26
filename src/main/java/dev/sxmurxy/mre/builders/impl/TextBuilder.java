package dev.sxmurxy.mre.builders.impl;

import java.awt.Color;

import dev.sxmurxy.mre.builders.AbstractBuilder;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltText;

public final class TextBuilder extends AbstractBuilder<BuiltText> {

    private MsdfFont font;
    private String text;
    private float size;
    private float thickness;
    private int color;
    private float smoothness;
    private float spacing;
    private int outlineColor;
    private float outlineThickness;

    public TextBuilder font(MsdfFont font) {
        this.font = font;
        return this;
    }

    public TextBuilder text(String text) {
        this.text = text;
        return this;
    }

    public TextBuilder size(float size) {
        this.size = size;
        return this;
    }

    public TextBuilder thickness(float thickness) {
        this.thickness = thickness;
        return this;
    }

    public TextBuilder color(Color color) {
        this.color = color.getRGB(); // Preserves RGBA from java.awt.Color
        return this;
    }

    public TextBuilder color(int r, int g, int b, int a) {
        this.color = (a << 24) | (r << 16) | (g << 8) | b; // Construct RGBA int
        return this;
    }

    public TextBuilder color(int color) {
        this.color = color; // Accepts raw RGBA int
        return this;
    }

    public TextBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public TextBuilder spacing(float spacing) {
        this.spacing = spacing;
        return this;
    }

    public TextBuilder outline(Color color, float thickness) {
        this.outlineColor = color.getRGB(); // Preserves RGBA from java.awt.Color
        this.outlineThickness = thickness;
        return this;
    }

    public TextBuilder outline(int r, int g, int b, int a, float thickness) {
        this.outlineColor = (a << 24) | (r << 16) | (g << 8) | b; // Construct RGBA int
        this.outlineThickness = thickness;
        return this;
    }

    public TextBuilder outline(int color, float thickness) {
        this.outlineColor = color; // Accepts raw RGBA int
        this.outlineThickness = thickness;
        return this;
    }

    @Override
    protected BuiltText _build() {
        return new BuiltText(
                this.font,
                this.text,
                this.size,
                this.thickness,
                this.color,
                this.smoothness,
                this.spacing,
                this.outlineColor,
                this.outlineThickness
        );
    }

    @Override
    protected void reset() {
        this.font = null;
        this.text = "";
        this.size = 0.0f;
        this.thickness = 0.05f;
        this.color = 0xFFFFFFFF; // Default to opaque white
        this.smoothness = 0.5f;
        this.spacing = 0.0f;
        this.outlineColor = 0x00000000; // Default to transparent black
        this.outlineThickness = 0.0f;
    }

}