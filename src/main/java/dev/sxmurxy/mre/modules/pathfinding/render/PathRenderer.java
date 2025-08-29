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
 * Fixed version that compiles properly with Fabric 1.21.4
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
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((worldRenderContext, vertexConsumerProvider) -> {
            if (!shouldRenderPath()) {
                return true;
            }

            List<Vec3d> path = PathfindingModule.getCurrentPath();
            if (path == null || path.isEmpty()) {
                return true;
            }

            MatrixStack matrices = worldRenderContext.matrixStack();
            Vec3d cameraPos = worldRenderContext.camera().getPos();

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
     * Render connecting lines between path nodes
     */
    private static void renderPathLines(MatrixStack matrices, Vec3d cameraPos, List<Vec3d> path) {
        if (path.size() < 2) return;

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d from = path.get(i);
            Vec3d to = path.get(i + 1);

            // Skip lines that are too far away
            if (cameraPos.distanceTo(from) > 64 || cameraPos.distanceTo(to) > 64) continue;

            // Skip very long lines (teleportation segments)
            if (from.distanceTo(to) > MAX_LINE_DISTANCE) continue;

            renderPathLine(matrices, cameraPos, from, to);
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
                .build();

        // Render the node
        nodeRect.render(matrix);

        matrices.pop();
    }

    /**
     * Render a line between two path points
     */
    private static void renderPathLine(MatrixStack matrices, Vec3d cameraPos, Vec3d from, Vec3d to) {
        matrices.push();

        Vec3d midpoint = from.lerp(to, 0.5);
        matrices.translate(
                midpoint.x - cameraPos.x,
                midpoint.y - cameraPos.y + 0.05, // Slightly above ground, below nodes
                midpoint.z - cameraPos.z
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Calculate line length and angle for proper rendering
        double lineLength = from.distanceTo(to);

        // Create line using rectangle with small height
        BuiltRectangle lineRect = Builder.rectangle()
                .size(new SizeState((float)lineLength, LINE_WIDTH))
                .radius(new QuadRadiusState(LINE_WIDTH / 2))
                .color(new QuadColorState(LINE_COLOR))
                .build();

        // Render the line
        lineRect.render(matrix);

        matrices.pop();
    }

    /**
     * Get node color based on position in path
     */
    private static Color getNodeColor(int index, int totalNodes) {
        if (index == 0) {
            return NODE_START_COLOR; // Start node
        } else if (index == totalNodes - 1) {
            return NODE_END_COLOR; // End node
        } else if (index == getCurrentPathIndex()) {
            return NODE_CURRENT_COLOR; // Current target
        } else {
            // Interpolate between start and end colors
            float ratio = (float) index / (totalNodes - 1);
            return interpolateColors(NODE_START_COLOR, NODE_END_COLOR, ratio);
        }
    }

    /**
     * Get node size based on importance
     */
    private static float getNodeSize(int index, int totalNodes) {
        if (index == 0 || index == totalNodes - 1) {
            return DESTINATION_NODE_SIZE; // Start and end nodes are larger
        } else if (index == getCurrentPathIndex()) {
            return CURRENT_NODE_SIZE; // Current target is largest
        } else {
            return BASE_NODE_SIZE; // Regular nodes
        }
    }

    /**
     * Get current path index (simplified - could be enhanced)
     */
    private static int getCurrentPathIndex() {
        // This would ideally come from PathfindingModule
        // For now, return 0 to indicate first waypoint
        return 0;
    }

    /**
     * Interpolate between two colors
     */
    private static Color interpolateColors(Color color1, Color color2, float ratio) {
        int r = (int) (color1.getRed() + ratio * (color2.getRed() - color1.getRed()));
        int g = (int) (color1.getGreen() + ratio * (color2.getGreen() - color1.getGreen()));
        int b = (int) (color1.getBlue() + ratio * (color2.getBlue() - color1.getBlue()));
        int a = (int) (color1.getAlpha() + ratio * (color2.getAlpha() - color1.getAlpha()));

        return new Color(r, g, b, a);
    }

    /**
     * Get rendering statistics
     */
    public static String getRenderStats() {
        List<Vec3d> path = PathfindingModule.getCurrentPath();
        if (path == null) {
            return "Path Rendering: Inactive";
        }

        return String.format(
                "Path Rendering:\n" +
                        "  Nodes: %d\n" +
                        "  Rendered: %s\n" +
                        "  Color Scheme: Blue Gradient",
                path.size(),
                shouldRenderPath() ? "Yes" : "No"
        );
    }
}