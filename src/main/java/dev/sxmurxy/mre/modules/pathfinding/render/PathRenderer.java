package dev.sxmurxy.mre.modules.pathfinding.render;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.modules.pathfinding.PathfindingModule;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.Color;
import java.util.List;

/**
 * Renders pathfinding visualization using blue semi-transparent nodes
 * Integrates with the mod's existing render system for consistent styling
 *
 * Features:
 * - Blue gradient coloring from start to destination
 * - Semi-transparent nodes for see-through effect
 * - Dynamic node sizing based on importance
 * - Smooth connecting lines between waypoints
 * - Performance optimized rendering
 */
public class PathRenderer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Color scheme - Blue theme with transparency
    private static final Color NODE_START_COLOR = new Color(30, 144, 255, 140);    // DodgerBlue - start
    private static final Color NODE_END_COLOR = new Color(173, 216, 230, 120);     // LightBlue - end
    private static final Color NODE_CURRENT_COLOR = new Color(0, 191, 255, 180);   // DeepSkyBlue - current target
    private static final Color LINE_COLOR = new Color(0, 255, 255, 90);            // Cyan - connections

    // Node size configuration
    private static final float BASE_NODE_SIZE = 0.15f;
    private static final float CURRENT_NODE_SIZE = 0.25f;
    private static final float DESTINATION_NODE_SIZE = 0.2f;

    // Line rendering configuration
    private static final float LINE_WIDTH = 0.08f;
    private static final double MAX_LINE_DISTANCE = 8.0; // Don't render lines longer than this

    /**
     * Initialize the path renderer
     */
    public static void initialize() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context) -> {
            if (!shouldRenderPath()) {
                return true;
            }

            List<Vec3d> path = PathfindingModule.getCurrentPath();
            if (path == null || path.isEmpty()) {
                return true;
            }

            MatrixStack matrices = context.matrixStack();
            Vec3d cameraPos = context.camera().getPos();

            // Set up rendering state
            setupRenderState();

            try {
                // Render path components
                renderPathNodes(matrices, cameraPos, path);
                renderPathLines(matrices, cameraPos, path);
            } finally {
                // Always restore render state
                restoreRenderState();
            }

            return true;
        });
    }

    /**
     * Check if path should be rendered
     */
    private static boolean shouldRenderPath() {
        return PathfindingModule.isPathing() &&
                mc.player != null &&
                mc.world != null &&
                PathfindingModule.getInstance() != null;
    }

    /**
     * Set up OpenGL render state for path rendering
     */
    private static void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
    }

    /**
     * Restore OpenGL render state after path rendering
     */
    private static void restoreRenderState() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Render individual path nodes using the mod's render system
     */
    private static void renderPathNodes(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
        for (int i = 0; i < path.size(); i++) {
            Vec3d nodePos = path.get(i);

            // Skip nodes that are too far away for performance
            if (cameraPos.distanceTo(nodePos) > 64) continue;

            // Determine node properties
            Color nodeColor = getNodeColor(i, path.size());
            float nodeSize = getNodeSize(i, path.size());

            // Render the node
            renderSingleNode(matrices, cameraPos, nodePos, nodeColor, nodeSize);
        }
    }

    /**
     * Render a single path node
     */
    private static void renderSingleNode(MatrixStack matrices, Vec3d cameraPos, Vec3d nodePos, Color color, float size) {
        matrices.push();

        // Translate to world position relative to camera
        matrices.translate(
                nodePos.x - cameraPos.x,
                nodePos.y - cameraPos.y + 0.1, // Slightly above ground
                nodePos.z - cameraPos.z
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Create circular node using the Builder system
        BuiltRectangle nodeRect = Builder.rectangle()
                .size(new SizeState(size, size))
                .radius(new QuadRadiusState(size / 2)) // Circular
                .color(new QuadColorState(color))
                .smoothness(1.0f)
                .build();

        // Render at origin (already translated)
        nodeRect.render(matrix, -size / 2, -size / 2, 0);

        matrices.pop();
    }

    /**
     * Render connecting lines between path nodes
     */
    private static void renderPathLines(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
        if (path.size() < 2) return;

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d from = path.get(i);
            Vec3d to = path.get(i + 1);

            // Skip very long lines for performance and clarity
            if (from.distanceTo(to) > MAX_LINE_DISTANCE) continue;

            // Skip lines that are too far from camera
            Vec3d midpoint = from.lerp(to, 0.5);
            if (cameraPos.distanceTo(midpoint) > 64) continue;

            renderPathLine(matrices, cameraPos, from, to);
        }
    }

    /**
     * Render a single line segment between two points
     */
    private static void renderPathLine(MatrixStack matrices, Vec3d cameraPos, Vec3d from, Vec3d to) {
        matrices.push();

        // Translate relative to camera
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Calculate line properties
        Vec3d direction = to.subtract(from).normalize();
        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x).multiply(LINE_WIDTH / 2);

        // Set up vertex buffer
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float r = LINE_COLOR.getRed() / 255.0f;
        float g = LINE_COLOR.getGreen() / 255.0f;
        float b = LINE_COLOR.getBlue() / 255.0f;
        float a = LINE_COLOR.getAlpha() / 255.0f;

        // Render line as quad strip
        Vec3d adjustedFrom = from.add(0, 0.05, 0); // Slightly above ground
        Vec3d adjustedTo = to.add(0, 0.05, 0);

        // Line start vertices
        buffer.vertex(matrix,
                        (float)(adjustedFrom.x + perpendicular.x),
                        (float)adjustedFrom.y,
                        (float)(adjustedFrom.z + perpendicular.z))
                .color(r, g, b, a);

        buffer.vertex(matrix,
                        (float)(adjustedFrom.x - perpendicular.x),
                        (float)adjustedFrom.y,
                        (float)(adjustedFrom.z - perpendicular.z))
                .color(r, g, b, a);

        // Line end vertices
        buffer.vertex(matrix,
                        (float)(adjustedTo.x + perpendicular.x),
                        (float)adjustedTo.y,
                        (float)(adjustedTo.z + perpendicular.z))
                .color(r, g, b, a);

        buffer.vertex(matrix,
                        (float)(adjustedTo.x - perpendicular.x),
                        (float)adjustedTo.y,
                        (float)(adjustedTo.z - perpendicular.z))
                .color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    /**
     * Get node color based on position in path (gradient from start to end)
     */
    private static Color getNodeColor(int index, int totalNodes) {
        if (totalNodes <= 1) return NODE_CURRENT_COLOR;

        // Current target (first node) gets special color
        if (index == 0) return NODE_CURRENT_COLOR;

        // Destination (last node) gets special color
        if (index == totalNodes - 1) return NODE_END_COLOR;

        // Interpolate between start and end colors for middle nodes
        float progress = (float) index / (totalNodes - 1);

        int r = (int) (NODE_START_COLOR.getRed() +
                (NODE_END_COLOR.getRed() - NODE_START_COLOR.getRed()) * progress);
        int g = (int) (NODE_START_COLOR.getGreen() +
                (NODE_END_COLOR.getGreen() - NODE_START_COLOR.getGreen()) * progress);
        int b = (int) (NODE_START_COLOR.getBlue() +
                (NODE_END_COLOR.getBlue() - NODE_START_COLOR.getBlue()) * progress);
        int a = (int) (NODE_START_COLOR.getAlpha() +
                (NODE_END_COLOR.getAlpha() - NODE_START_COLOR.getAlpha()) * progress);

        return new Color(r, g, b, a);
    }

    /**
     * Get node size based on importance and position in path
     */
    private static float getNodeSize(int index, int totalNodes) {
        // Current target (first node) is largest
        if (index == 0) return CURRENT_NODE_SIZE;

        // Destination (last node) is larger
        if (index == totalNodes - 1) return DESTINATION_NODE_SIZE;

        // Regular waypoints
        return BASE_NODE_SIZE;
    }

    /**
     * Render 3D node alternative for enhanced visualization
     */
    private static void render3DNode(MatrixStack matrices, Vec3d cameraPos, Vec3d nodePos, Color color, float size) {
        matrices.push();

        // Translate to position
        matrices.translate(
                nodePos.x - cameraPos.x,
                nodePos.y - cameraPos.y,
                nodePos.z - cameraPos.z
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Set up rendering
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        float half = size / 2;

        // Render cube faces (simplified - just top and front for visibility)
        // Top face
        buffer.vertex(matrix, -half, half, -half).color(r, g, b, a);
        buffer.vertex(matrix, half, half, -half).color(r, g, b, a);
        buffer.vertex(matrix, half, half, half).color(r, g, b, a);
        buffer.vertex(matrix, -half, half, half).color(r, g, b, a);

        // Front face
        buffer.vertex(matrix, -half, -half, half).color(r, g, b, a * 0.8f);
        buffer.vertex(matrix, half, -half, half).color(r, g, b, a * 0.8f);
        buffer.vertex(matrix, half, half, half).color(r, g, b, a * 0.8f);
        buffer.vertex(matrix, -half, half, half).color(r, g, b, a * 0.8f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    /**
     * Render enhanced path with additional visual effects
     */
    public static void renderEnhancedPath(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
        if (path == null || path.isEmpty()) return;

        setupRenderState();

        try {
            // Render path trail effect
            renderPathTrail(matrices, cameraPos, path);

            // Render nodes with pulsing effect
            renderPulsingNodes(matrices, cameraPos, path);

            // Render direction indicators
            renderDirectionIndicators(matrices, cameraPos, path);

        } finally {
            restoreRenderState();
        }
    }

    /**
     * Render trail effect behind the path
     */
    private static void renderPathTrail(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
        if (path.size() < 2) return;

        long time = System.currentTimeMillis();
        double trailPhase = (time % 2000) / 2000.0; // 2 second cycle

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d from = path.get(i);
            Vec3d to = path.get(i + 1);

            // Calculate trail opacity based on phase
            double nodePhase = (double) i / path.size();
            double phaseOffset = (trailPhase + nodePhase) % 1.0;
            float alpha = (float) (Math.sin(phaseOffset * Math.PI * 2) * 0.3 + 0.4);

            Color trailColor = new Color(LINE_COLOR.getRed(), LINE_COLOR.getGreen(),
                    LINE_COLOR.getBlue(), (int)(alpha * 255));

            renderPathLineWithColor(matrices, cameraPos, from, to, trailColor);
        }
    }

    /**
     * Render nodes with pulsing animation
     */
    private static void renderPulsingNodes(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
        long time = System.currentTimeMillis();
        double pulsePhase = (time % 1500) / 1500.0; // 1.5 second pulse

        for (int i = 0; i < path.size(); i++) {
            Vec3d nodePos = path.get(i);

            if (cameraPos.distanceTo(nodePos) > 64) continue;

            // Calculate pulsing size
            float baseSize = getNodeSize(i, path.size());
            float pulseFactor = (float) (Math.sin(pulsePhase * Math.PI * 2) * 0.2 + 1.0);
            float pulsingSize = baseSize * pulseFactor;

            // Calculate pulsing color
            Color baseColor = getNodeColor(i, path.size());
            int pulseAlpha = (int) (baseColor.getAlpha() * (0.8 + 0.2 * pulseFactor));
            Color pulsingColor = new Color(baseColor.getRed(), baseColor.getGreen(),
                    baseColor.getBlue(), pulseAlpha);

            renderSingleNode(matrices, cameraPos, nodePos, pulsingColor, pulsingSize);
        }
    }

    /**
     * Render direction indicators along the path
     */
    private static void renderDirectionIndicators(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
        if (path.size() < 2) return;

        for (int i = 0; i < path.size() - 1; i += 3) { // Every 3rd segment
            Vec3d from = path.get(i);
            Vec3d to = path.get(Math.min(i + 1, path.size() - 1));

            Vec3d midpoint = from.lerp(to, 0.5);
            if (cameraPos.distanceTo(midpoint) > 32) continue;

            Vec3d direction = to.subtract(from).normalize();
            renderDirectionArrow(matrices, cameraPos, midpoint, direction);
        }
    }

    /**
     * Render a small arrow indicating path direction
     */
    private static void renderDirectionArrow(MatrixStack matrices, Vec3d cameraPos, Vec3d position, Vec3d direction) {
        matrices.push();

        matrices.translate(
                position.x - cameraPos.x,
                position.y - cameraPos.y + 0.2,
                position.z - cameraPos.z
        );

        // Create small arrow shape (simplified triangle)
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x).multiply(0.1);
        Vec3d tip = direction.multiply(0.15);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Color arrowColor = new Color(255, 255, 0, 150); // Yellow arrows
        float r = arrowColor.getRed() / 255.0f;
        float g = arrowColor.getGreen() / 255.0f;
        float b = arrowColor.getBlue() / 255.0f;
        float a = arrowColor.getAlpha() / 255.0f;

        // Arrow triangle
        buffer.vertex(matrix, (float)tip.x, (float)tip.y, (float)tip.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)-perpendicular.x, (float)-perpendicular.y, (float)-perpendicular.z).color(r, g, b, a);
        buffer.vertex(matrix, (float)perpendicular.x, (float)perpendicular.y, (float)perpendicular.z).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    /**
     * Render path line with custom color
     */
    private static void renderPathLineWithColor(MatrixStack matrices, Vec3d cameraPos, Vec3d from, Vec3d to, Color color) {
        matrices.push();

        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Vec3d direction = to.subtract(from).normalize();
        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x).multiply(LINE_WIDTH / 2);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        Vec3d adjustedFrom = from.add(0, 0.05, 0);
        Vec3d adjustedTo = to.add(0, 0.05, 0);

        buffer.vertex(matrix, (float)(adjustedFrom.x + perpendicular.x), (float)adjustedFrom.y, (float)(adjustedFrom.z + perpendicular.z)).color(r, g, b, a);
        buffer.vertex(matrix, (float)(adjustedFrom.x - perpendicular.x), (float)adjustedFrom.y, (float)(adjustedFrom.z - perpendicular.z)).color(r, g, b, a);
        buffer.vertex(matrix, (float)(adjustedTo.x + perpendicular.x), (float)adjustedTo.y, (float)(adjustedTo.z + perpendicular.z)).color(r, g, b, a);
        buffer.vertex(matrix, (float)(adjustedTo.x - perpendicular.x), (float)adjustedTo.y, (float)(adjustedTo.z - perpendicular.z)).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    /**
     * Get render statistics for debugging
     */
    public static RenderStats getRenderStats(List<Vec3d> path, Vec3d cameraPos) {
        if (path == null) return new RenderStats(0, 0, 0, 0);

        int totalNodes = path.size();
        int visibleNodes = 0;
        int totalLines = Math.max(0, path.size() - 1);
        int visibleLines = 0;

        for (int i = 0; i < path.size(); i++) {
            if (cameraPos.distanceTo(path.get(i)) <= 64) {
                visibleNodes++;
            }
        }

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d midpoint = path.get(i).lerp(path.get(i + 1), 0.5);
            if (cameraPos.distanceTo(midpoint) <= 64) {
                visibleLines++;
            }
        }

        return new RenderStats(totalNodes, visibleNodes, totalLines, visibleLines);
    }

    /**
     * Rendering statistics record
     */
    public record RenderStats(
            int totalNodes,
            int visibleNodes,
            int totalLines,
            int visibleLines
    ) {
        public String getSummary() {
            return String.format("Nodes: %d/%d, Lines: %d/%d",
                    visibleNodes, totalNodes, visibleLines, totalLines);
        }

        public double getNodeVisibilityRatio() {
            return totalNodes > 0 ? (double) visibleNodes / totalNodes : 0;
        }

        public double getLineVisibilityRatio() {
            return totalLines > 0 ? (double) visibleLines / totalLines : 0;
        }
    }
}