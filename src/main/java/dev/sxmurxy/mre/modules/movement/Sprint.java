package dev.sxmurxy.mre.modules.movement;

import dev.sxmurxy.mre.modules.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleManager;

public class Sprint extends Module {

    public Sprint() {
        super("Sprint", "Automatyczny sprint.", ModuleCategory.MOVEMENT);
    }


    @Override
    public void onUpdate() {
        if (mc == null || mc.player == null) {
            return;
        }

        // Additional checks for safety
        if (mc.world == null || !mc.player.isAlive()) {
            return;
        }
        if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_W)) {
            MinecraftClient.getInstance().player.setSprinting(true);
        }
    }

    @Override
    public void onDisable() {
        System.out.println("2");
    }

    @Override
    public void onEnable() {
        System.out.println("1");
    }
}
