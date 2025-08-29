package dev.sxmurxy.mre.client.pathfinding;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.client.pathfinding.*;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.List;

/**
 * Path renderer for the advanced pathfinding system.
 * Renders path nodes with colors based on movement type while keeping the original drawing style.
 */
public class PathRender {

    // The custom RenderLayer for drawing translucent shapes through walls
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

    public PathRender() {
        // Register the render event when the module is created
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            onWorldRender(context.matrixStack(), context.camera());
        });
    }


    public void onWorldRender(MatrixStack matrices, Camera camera) {
        // Only render if module is enabled, rendering is enabled, and pathfinding is active

        // Get path data from the new PathfinderAPI
        List<Vec3d> smoothedPath = PathfinderAPI.getSmoothedPath();
        List<Pathfinder.PathNode> waypoints = PathfinderAPI.getSimplifiedPath();

        if (smoothedPath == null || smoothedPath.isEmpty() || waypoints == null) {
            return;
        }

        Vec3d cameraPos = camera.getPos();

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Render the waypoints as colored boxes (keeping original drawing style)
        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEffectVertexConsumers();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(HIGHLIGHT_LAYER);

        for (Pathfinder.PathNode node : waypoints) {
            float[] color = getColorForMoveType(node.move);
            drawFilledBox(matrices, vertexConsumer, node.pos, color[0], color[1], color[2], 0.3f);
        }

        vertexConsumers.draw(); // Draw all the buffered boxes at once

        matrices.pop();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Get colors based on movement type for better visual feedback.
     */
    private float[] getColorForMoveType(Pathfinder.MoveType moveType) {
        switch (moveType) {
            case JUMP -> {
                return new float[]{0.2f, 1.0f, 0.2f}; // Green for jumps
            }
            case FALL -> {
                return new float[]{1.0f, 1.0f, 0.2f}; // Yellow for falls
            }
            case SPRINT -> {
                return new float[]{0.2f, 0.6f, 1.0f}; // Blue for sprinting
            }
            case AOTV -> {
                return new float[]{1.0f, 0.2f, 1.0f}; // Magenta for AOTV teleports
            }
            case ETHERWARP -> {
                return new float[]{0.8f, 0.2f, 1.0f}; // Purple for Etherwarp
            }
            default -> {
                return new float[]{0.2f, 0.8f, 1.0f}; // Light Blue for walking
            }
        }
    }

    /**
     * Draw filled box using the original method (keeping the exact same style).
     */
    private void drawFilledBox(MatrixStack matrices, VertexConsumer vertexConsumer, BlockPos pos, float r, float g, float b, float a) {
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        float x1 = pos.getX();
        float y1 = pos.getY();
        float z1 = pos.getZ();
        float x2 = x1 + 1; // Fixed: should be +1 for full block size
        float y2 = (float) (y1 + 0.1); // Fixed: should be +1 for full block size
        float z2 = z1 + 1; // Fixed: should be +1 for full block size

        // TOP face
        vertexConsumer.vertex(positionMatrix, x1, y2, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y2, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y2, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y2, z2).color(r, g, b, a);

        // BOTTOM face
        vertexConsumer.vertex(positionMatrix, x1, y1, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y1, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y1, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y1, z1).color(r, g, b, a);

        // FRONT face (Z+)
        vertexConsumer.vertex(positionMatrix, x1, y1, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y2, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y2, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y1, z2).color(r, g, b, a);

        // BACK face (Z-)
        vertexConsumer.vertex(positionMatrix, x2, y1, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y2, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y2, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y1, z1).color(r, g, b, a);

        // LEFT face (X-)
        vertexConsumer.vertex(positionMatrix, x1, y1, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y2, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y2, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x1, y1, z2).color(r, g, b, a);

        // RIGHT face (X+)
        vertexConsumer.vertex(positionMatrix, x2, y1, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y2, z2).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y2, z1).color(r, g, b, a);
        vertexConsumer.vertex(positionMatrix, x2, y1, z1).color(r, g, b, a);
    }
}