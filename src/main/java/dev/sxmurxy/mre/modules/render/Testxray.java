package dev.sxmurxy.mre.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.settings.impl.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Testxray extends Module {
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


    public Testxray() {
        super("Test xray", "Highlights a block at (X, Y, Z) in 3D, visible through walls.", ModuleCategory.RENDER);
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

    private final List<BlockPos> diamondOres = new ArrayList<>();
    private int tickCounter = 0;
    private static final int SCAN_INTERVAL = 20; // co 20 ticków = 1 sekunda

    public void onWorldRender(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!isToggled() || mc.world == null || mc.player == null) return;

        // Odśwież cache co SCAN_INTERVAL ticków
        tickCounter++;
        if (tickCounter >= SCAN_INTERVAL) {
            tickCounter = 0;
            diamondOres.clear();
            BlockPos playerPos = mc.player.getBlockPos();
            int radius = 16;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (mc.world.getBlockState(pos).isOf(Blocks.DIAMOND_ORE)
                                || mc.world.getBlockState(pos).isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                            diamondOres.add(pos);
                        }
                    }
                }
            }
        }

        // Render
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEffectVertexConsumers();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(HIGHLIGHT_LAYER);
        MatrixStack.Entry matrixEntry = matrices.peek();
        Matrix4f positionMatrix = matrixEntry.getPositionMatrix();

        float red = 0f, green = 1f, blue = 1f, alpha = 0.2f;


        for (BlockPos pos : diamondOres) {
            float x = (float) pos.getX() - (float) camera.getPos().x;
            float y = (float) pos.getY() - (float) camera.getPos().y;
            float z = (float) pos.getZ() - (float) camera.getPos().z;
            float x2 = x + 1, y2 = y + 1, z2 = z + 1;

            // Rysowanie tylko krawędzi
            drawBoxLines(vertexConsumer, positionMatrix, x, y, z, x2, y2, z2, red, green, blue, alpha);
        }

        vertexConsumers.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // Metoda rysująca tylko krawędzie sześcianu
    private void drawBoxLines(VertexConsumer vertexConsumer, Matrix4f mat,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        // Dolna kwadratowa podstawa
        vertexConsumer.vertex(mat, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(mat, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(mat, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0);

        // BOTTOM
        vertexConsumer.vertex(mat, x1, y1, z2).color(r, g, b, a).normal(0, -1, 0);
        vertexConsumer.vertex(mat, x2, y1, z2).color(r, g, b, a).normal(0, -1, 0);
        vertexConsumer.vertex(mat, x2, y1, z1).color(r, g, b, a).normal(0, -1, 0);
        vertexConsumer.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(0, -1, 0);

        // FRONT (z2)
        vertexConsumer.vertex(mat, x1, y1, z2).color(r, g, b, a).normal(0, 0, 1);
        vertexConsumer.vertex(mat, x2, y1, z2).color(r, g, b, a).normal(0, 0, 1);
        vertexConsumer.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(0, 0, 1);
        vertexConsumer.vertex(mat, x1, y2, z2).color(r, g, b, a).normal(0, 0, 1);

        // BACK (z1)
        vertexConsumer.vertex(mat, x2, y1, z1).color(r, g, b, a).normal(0, 0, -1);
        vertexConsumer.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(0, 0, -1);
        vertexConsumer.vertex(mat, x1, y2, z1).color(r, g, b, a).normal(0, 0, -1);
        vertexConsumer.vertex(mat, x2, y2, z1).color(r, g, b, a).normal(0, 0, -1);

        // LEFT (x1)
        vertexConsumer.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(-1, 0, 0);
        vertexConsumer.vertex(mat, x1, y1, z2).color(r, g, b, a).normal(-1, 0, 0);
        vertexConsumer.vertex(mat, x1, y2, z2).color(r, g, b, a).normal(-1, 0, 0);
        vertexConsumer.vertex(mat, x1, y2, z1).color(r, g, b, a).normal(-1, 0, 0);

        // RIGHT (x2)
        vertexConsumer.vertex(mat, x2, y1, z2).color(r, g, b, a).normal(1, 0, 0);
        vertexConsumer.vertex(mat, x2, y1, z1).color(r, g, b, a).normal(1, 0, 0);
        vertexConsumer.vertex(mat, x2, y2, z1).color(r, g, b, a).normal(1, 0, 0);
        vertexConsumer.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(1, 0, 0);


    }



}