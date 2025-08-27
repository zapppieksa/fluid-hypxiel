package dev.sxmurxy.mre.modules.render;

import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.ModuleManager;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltBlur;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Arraylist extends Module {

    private static final Supplier<MsdfFont> NIGA_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("atlas").data("atlas").build());
    private static final float MODULE_HEIGHT = 15f; // Fixed height of 15px for each module entry
    private static final float MODULE_SPACING = -1.2f; // Spacing between module entries
    private static final float PADDING_X = 5f; // Horizontal padding for background
    private static final float TEXT_SIZE = 8f; // Font size for module names
    private static final float RIGHT_OFFSET = -0.2f; // Offset from the right edge of the screen

    public Arraylist() {
        super("Arraylist", "Displays enabled modules", ModuleCategory.RENDER);
        HudRenderCallback.EVENT.register(this::renderTargetHUD);
    }

    @Override
    public void onUpdate() {
        if (mc == null || mc.player == null || mc.world == null || !mc.player.isAlive()) {
            return;
        }
    }

    private void renderTargetHUD(DrawContext context, RenderTickCounter tickCounter) {
        if (!this.isToggled()) return;
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        // Get list of toggled modules
        List<Module> toggledModules = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module.isToggled() && module != this) { // Exclude Arraylist itself
                toggledModules.add(module);
            }
        }

        // Sort modules by name length (longest first)
        toggledModules.sort((a, b) -> {
            float widthA = NIGA_FONT.get().getWidth(a.getName().toLowerCase(), TEXT_SIZE);
            float widthB = NIGA_FONT.get().getWidth(b.getName().toLowerCase(), TEXT_SIZE);
            return Float.compare(widthB, widthA); // Descending order
        });

        if (toggledModules.isEmpty()) return; // Don't render if no modules are toggled

        // Get screen width for right-alignment
        float screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();

        // Render each module with its own background sized to its name length
        float currentY = -1f; // Start at top

        for (Module module : toggledModules) {
            // Calculate width for this module's name
            float textWidth = NIGA_FONT.get().getWidth(module.getName().toLowerCase(), TEXT_SIZE);
            float moduleWidth = textWidth + PADDING_X * 2; // Add padding on both sides

            // Calculate startX for right-alignment
            float startX = screenWidth - moduleWidth - RIGHT_OFFSET;

            // Render blur for this module
            BuiltBlur blur = Builder.blur()
                    .size(new SizeState(moduleWidth, MODULE_HEIGHT))
                    .radius(new QuadRadiusState(0f, 3f, 0f, 0f))
                    .blurRadius(15f)
                    .smoothness(1f)
                    .color(new QuadColorState(Color.WHITE))
                    .build();
            blur.render(matrix, startX, currentY);

            // Render background rectangle for this module
            BuiltRectangle rectangle = Builder.rectangle()
                    .size(new SizeState(moduleWidth, MODULE_HEIGHT))
                    .color(new QuadColorState(new Color(36, 36, 36, 100)))
                    .radius(new QuadRadiusState(0f, 3f, 0f, 0f))
                    .smoothness(1.0f)
                    .build();
            rectangle.render(matrix, startX, currentY);

            // Render module name with solid color, centered horizontally
            BuiltText text = Builder.text()
                    .font(NIGA_FONT.get())
                    .text(module.getName().toLowerCase())
                    .color(new Color(255, 255, 255, 255))
                    .size(TEXT_SIZE)
                    .thickness(0.01f)
                    .build();
            text.render(matrix, startX + (moduleWidth - textWidth) / 2, currentY + (MODULE_HEIGHT - TEXT_SIZE) / 2 - 1); // Center text vertically and horizontally

            currentY += MODULE_HEIGHT + MODULE_SPACING;
        }
    }

    @Override
    public void onDisable() {
        System.out.println("Arraylist disabled");
    }

    @Override
    public void onEnable() {
        System.out.println("Arraylist enabled");
    }
}