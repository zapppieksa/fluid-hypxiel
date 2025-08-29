package dev.sxmurxy.mre.modules;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

@Getter
public class Module {

    public static MinecraftClient mc = MinecraftClient.getInstance();

    public ModuleCategory category;
    @Setter
    public boolean toggled = false;

    public String name;
    public String desc;
    protected String description;
    protected int keyCode;
    public Module(String name, String desc, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyCode = keyCode;
        this.toggled = false;
    }

    private int key = -1;
    private boolean keyPressed = false;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean wasKeyPressed() {
        return keyPressed;
    }

    public void setKeyPressed(boolean pressed) {
        this.keyPressed = pressed;
    }

    public void onEnable() {


    }
    protected void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }
    public void onDisable() {


    }
    public int getKeyCode() {
        return keyCode;
    }

    public boolean isToggled() {
        return toggled;
    }

    // Setters
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }
    public void onTick() {
        // Override in subclasses
    }
    public void onUpdate() {}

    public void toggle() {
        toggled = !toggled;

        if (toggled) {
            onEnable();
        } else {
            onDisable();
        }
    }
}