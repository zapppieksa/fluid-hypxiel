package dev.sxmurxy.mre.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.settings.impl.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class Test extends Module {
    private final NumberSetting range = new NumberSetting("X", this, -246.0f, -1000.0f, 320.0f, true);
    private final NumberSetting range1 = new NumberSetting("Y", this, 105.0f, 0.0f, 200.0f, true);
    private final NumberSetting range2 = new NumberSetting("Z", this, -223.0f, -64.0f, 320.0f, true);

    private static final RenderLayer HIGHLIGHT_LAYER = RenderLayer.of(
            "highlight_layer",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            true,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(new RenderPhase.ShaderProgram(ShaderProgramKeys.POSITION_COLOR))
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(new RenderPhase.DepthTest("always", GL11.GL_ALWAYS))
                    .build(true)
    );


    public Test() {
        super("Test", "Highlights a block at (X, Y, Z) in 3D, visible through walls.", ModuleCategory.RENDER);
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {

            onWorldRender(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(false));

        });
    }

    @Override
    public void onUpdate() {
        // No update logic needed
    }

    @Override
    public void onDisable() {
        System.out.println("Block highlight disabled");
    }

    @Override
    public void onEnable() {
        System.out.println("Block highlight enabled");
    }

    public void onWorldRender(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!isToggled() || mc.world == null || mc.player == null) {
            return;
        }

        BlockPos targetPos = new BlockPos(range.get().intValue(), range1.get().intValue(), range2.get().intValue());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEffectVertexConsumers();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(HIGHLIGHT_LAYER);
        MatrixStack.Entry matrixEntry = matrices.peek();
        Matrix4f positionMatrix = matrixEntry.getPositionMatrix();

        float red = 1.0f, green = 0.0f, blue = 0.0f, alpha = 0.2f;

        // Kamera offset
        float x1 = targetPos.getX() - (float) camera.getPos().x;
        float y1 = targetPos.getY() - (float) camera.getPos().y;
        float z1 = targetPos.getZ() - (float) camera.getPos().z;
        float x2 = x1 + 1;
        float y2 = y1 + 1;
        float z2 = z1 + 1;

        // TOP
        vertexConsumer.vertex(positionMatrix, x1, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(positionMatrix, x2, y2, z1).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(positionMatrix, x2, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(positionMatrix, x1, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0);

        // BOTTOM
        vertexConsumer.vertex(positionMatrix, x1, y1, z2).color(red, green, blue, alpha).normal(0, -1, 0);
        vertexConsumer.vertex(positionMatrix, x2, y1, z2).color(red, green, blue, alpha).normal(0, -1, 0);
        vertexConsumer.vertex(positionMatrix, x2, y1, z1).color(red, green, blue, alpha).normal(0, -1, 0);
        vertexConsumer.vertex(positionMatrix, x1, y1, z1).color(red, green, blue, alpha).normal(0, -1, 0);

        // FRONT (z2)
        vertexConsumer.vertex(positionMatrix, x1, y1, z2).color(red, green, blue, alpha).normal(0, 0, 1);
        vertexConsumer.vertex(positionMatrix, x2, y1, z2).color(red, green, blue, alpha).normal(0, 0, 1);
        vertexConsumer.vertex(positionMatrix, x2, y2, z2).color(red, green, blue, alpha).normal(0, 0, 1);
        vertexConsumer.vertex(positionMatrix, x1, y2, z2).color(red, green, blue, alpha).normal(0, 0, 1);

        // BACK (z1)
        vertexConsumer.vertex(positionMatrix, x2, y1, z1).color(red, green, blue, alpha).normal(0, 0, -1);
        vertexConsumer.vertex(positionMatrix, x1, y1, z1).color(red, green, blue, alpha).normal(0, 0, -1);
        vertexConsumer.vertex(positionMatrix, x1, y2, z1).color(red, green, blue, alpha).normal(0, 0, -1);
        vertexConsumer.vertex(positionMatrix, x2, y2, z1).color(red, green, blue, alpha).normal(0, 0, -1);

        // LEFT (x1)
        vertexConsumer.vertex(positionMatrix, x1, y1, z1).color(red, green, blue, alpha).normal(-1, 0, 0);
        vertexConsumer.vertex(positionMatrix, x1, y1, z2).color(red, green, blue, alpha).normal(-1, 0, 0);
        vertexConsumer.vertex(positionMatrix, x1, y2, z2).color(red, green, blue, alpha).normal(-1, 0, 0);
        vertexConsumer.vertex(positionMatrix, x1, y2, z1).color(red, green, blue, alpha).normal(-1, 0, 0);

        // RIGHT (x2)
        vertexConsumer.vertex(positionMatrix, x2, y1, z2).color(red, green, blue, alpha).normal(1, 0, 0);
        vertexConsumer.vertex(positionMatrix, x2, y1, z1).color(red, green, blue, alpha).normal(1, 0, 0);
        vertexConsumer.vertex(positionMatrix, x2, y2, z1).color(red, green, blue, alpha).normal(1, 0, 0);
        vertexConsumer.vertex(positionMatrix, x2, y2, z2).color(red, green, blue, alpha).normal(1, 0, 0);

        vertexConsumers.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

}