package dev.sxmurxy.mre.modules.pathfinding.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.modules.pathfinding.PathfinderAPI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.List;

/**
 * Main class to initialize the path rendering system.
 * It hooks into the world render event and uses the PathBuilder to construct
 * and render the visual components of the path.
 */
public class PathRenderer {

    public static void initialize() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register((context) -> {
            if (PathfinderAPI.isPathing()) {
                List<Vec3d> path = PathfinderAPI.getCurrentPath();
                if (path != null && !path.isEmpty()) {

                    // Use the new builder to create and render the path line
                    PathBuilder.line()
                            .color(new Color(0, 255, 255, 150))
                            .build()
                            .render(context.matrixStack(), context.camera(), path);

                    // Use the builder to create and render a box for each node
                    BuiltPathNodeBox nodeBox = PathBuilder.nodeBox()
                            .color(new Color(255, 0, 0, 200))
                            .size(0.1)
                            .build();

                    for (Vec3d point : path) {
                        nodeBox.render(context.matrixStack(), context.camera(), point);
                    }
                }
            }
        });
    }
}

/**
 * A builder class for creating path visualization components,
 * mirroring the builder pattern used in the client's UI system.
 */
class PathBuilder {
    public static PathLineBuilder line() {
        return new PathLineBuilder();
    }

    public static PathNodeBoxBuilder nodeBox() {
        return new PathNodeBoxBuilder();
    }
}

/**
 * Builder for the line connecting path nodes.
 */
class PathLineBuilder {
    private Color color = Color.WHITE;

    public PathLineBuilder color(Color color) {
        this.color = color;
        return this;
    }

    public BuiltPathLine build() {
        return new BuiltPathLine(color);
    }
}

/**
 * Builder for the boxes that represent individual path nodes.
 */
class PathNodeBoxBuilder {
    private Color color = Color.WHITE;
    private double size = 0.1;

    public PathNodeBoxBuilder color(Color color) {
        this.color = color;
        return this;
    }

    public PathNodeBoxBuilder size(double size) {
        this.size = size;
        return this;
    }

    public BuiltPathNodeBox build() {
        return new BuiltPathNodeBox(color, size);
    }
}

/**
 * A renderable component for the path's connecting line.
 * @param color The color of the line.
 */
record BuiltPathLine(Color color) {
    public void render(MatrixStack matrixStack, Camera camera, List<Vec3d> path) {
        setupRenderState();

        Tessellator tessellator = Tessellator.getInstance();
        Vec3d cameraPos = camera.getPos();

        matrixStack.push();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();


        matrixStack.pop();
        restoreRenderState();
    }

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
    }

    private void restoreRenderState() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}

/**
 * A renderable component for the box marking a single path node.
 * @param color The color of the box.
 * @param size The size of the box.
 */
record BuiltPathNodeBox(Color color, double size) {
    public void render(MatrixStack matrixStack, Camera camera, Vec3d nodePosition) {
        setupRenderState();

        Vec3d cameraPos = camera.getPos();

        matrixStack.push();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Box box = new Box(nodePosition, nodePosition).expand(size);

        // Use the modern method for drawing debug shapes
        matrixStack.pop();
        restoreRenderState();
    }

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
    }

    private void restoreRenderState() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
