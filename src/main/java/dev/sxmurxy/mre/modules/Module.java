package dev.sxmurxy.mre.modules;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;

@Getter
public class Module {

    public static MinecraftClient mc = MinecraftClient.getInstance();

    public ModuleCategory category;
    @Setter
    public boolean toggled = false;

    public String name;
    public String desc;

    public Module(String name, String desc, ModuleCategory category) {
        this.name = name;
        this.desc = desc;
        this.category = category;
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

    public void onDisable() {


    }

    public void onUpdate() {}

    public void toggle() {
        setToggled(!isToggled());

    }
}