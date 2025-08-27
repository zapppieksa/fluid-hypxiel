package dev.sxmurxy.mre.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class DrawUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void drawText(String text, float x, float y, int color, MatrixStack matrices) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer renderer = mc.textRenderer;

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        renderer.draw(
                text,
                x,
                y,
                color,
                true, // shadow
                matrix,
                mc.getBufferBuilders().getEntityVertexConsumers(),
                TextRenderer.TextLayerType.NORMAL,
                0, // backgroundColor (0 = brak)
                15728880 // pełne światło
        );

        matrices.pop();
    }

    public static Vec3d projectTo2D(Vec3d pos) {
        Matrix4f matrix = mc.gameRenderer.getBasicProjectionMatrix(mc.getRenderTickCounter().getTickDelta(true));
        Vector4f vec = new Vector4f((float) pos.x, (float) pos.y, (float) pos.z, 1.0f);

        vec.mul(matrix);

        if (vec.w <= 0) return null;

        vec.x /= vec.w;
        vec.y /= vec.w;

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        float screenX = (vec.x * 0.5f + 0.5f) * width;
        float screenY = (1.0f - (vec.y * 0.5f + 0.5f)) * height;

        return new Vec3d(screenX, screenY, 0);
    }
}
