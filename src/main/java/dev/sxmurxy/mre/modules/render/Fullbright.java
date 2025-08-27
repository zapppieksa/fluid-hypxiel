package dev.sxmurxy.mre.modules.render;

import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.settings.impl.EnumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

import java.lang.reflect.Field;

public class Fullbright extends Module {
    public EnumSetting mode = new EnumSetting("Mode", this, "Night Vision", "Night Vision", "Gamma");
    private double previousGamma = -1;

    public Fullbright() {
        super("FullBright", "Widzenie w ciemnosciach.", ModuleCategory.RENDER);
    }

    @Override
    public void onEnable() {
        if (mode.is("Gamma")) {
            SimpleOption<Double> gamma = MinecraftClient.getInstance().options.getGamma();
            previousGamma = gamma.getValue();

            try {
                Field valueField = SimpleOption.class.getDeclaredField("value");
                valueField.setAccessible(true);
                valueField.set(gamma, 1000.0); // jeb gammazzzzzzzzzzzzzzzz
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUpdate() {
        if (MinecraftClient.getInstance().player == null) return;

        if (mode.is("Night Vision")) {
            MinecraftClient.getInstance().player.addStatusEffect(
                    new net.minecraft.entity.effect.StatusEffectInstance(
                            net.minecraft.entity.effect.StatusEffects.NIGHT_VISION, 400, 0, false, false
                    )
            );
        }
    }

    @Override
    public void onDisable() {
        if (mode.is("Night Vision") && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
        }

        if (mode.is("Gamma") && previousGamma != -1) {
            MinecraftClient.getInstance().options.getGamma().setValue(previousGamma);
        }
    }
}
