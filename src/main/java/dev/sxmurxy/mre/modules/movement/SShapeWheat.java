package dev.sxmurxy.mre.modules.movement;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltBlur;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.Random;
import java.util.function.Supplier;

public class SShapeWheat extends Module {
    private boolean isFarming = false;
    private long lastActionTime = 0;
    private State currentState = State.LEFT;
    private final Random random = new Random();
    private final int baseDelay = 50; // Base delay for smoothness (ms)
    private boolean wasScreenOpen = false;
    private long screenClosedTime = 0;
    private final int screenCloseDelay = 200; // 200ms delay after closing screen
    private static final Supplier<MsdfFont> NIGA_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("atlas2").data("atlas2").build());
    private enum State {
        LEFT, FORWARD1, RIGHT, FORWARD2
    }
    private int blocksBroken = 0;
    private long bpsStartTime = 0;
    private double bps = 0.0;
    public SShapeWheat() {
        super("Wheat S-Shape", "Automatic S-shape farming on Hypixel Skyblock with collision detection.", ModuleCategory.MOVEMENT);
        // Activation key: R
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isToggled() || mc.player == null || mc.world == null) return;
            mc.player.setYaw(90);
            mc.player.setPitch(0);
        });
        HudRenderCallback.EVENT.register(this::renderfarmstats);
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

        long currentTime = System.currentTimeMillis();

        // Check if any screen is open
        boolean isScreenOpen = mc.currentScreen != null;

        if (isScreenOpen) {
            wasScreenOpen = true;
            return;
        }

        // If screen was open and just closed, record the time
        if (wasScreenOpen && !isScreenOpen) {
            wasScreenOpen = false;
            screenClosedTime = currentTime;
            return;
        }

        // Check if 200ms has passed since screen closed
        if (screenClosedTime > 0 && currentTime - screenClosedTime < screenCloseDelay) {
            return;
        }

        if (currentTime - lastActionTime < baseDelay) {
            return;
        }

        // Perform actions based on state
        switch (currentState) {
            case LEFT:
                handleLeftMovement();
                break;
            case FORWARD1:
                handleForwardMovement(State.RIGHT);
                break;
            case RIGHT:
                handleRightMovement();
                break;
            case FORWARD2:
                handleForwardMovement(State.LEFT);
                break;
        }
        if (!isToggled()) {
            releaseAllKeys();
        }
        lastActionTime = currentTime;
    }

    private void handleLeftMovement() {
        // Hold left mouse button for farming
        mc.options.attackKey.setPressed(true);

        // Check for collision to the left
        if (checkCollisionInDirection(-90)) { // -90 degrees is left
            // Collision detected - stop moving left, transition to forward
            setKeyPressed(mc.options.leftKey, false);
            setKeyPressed(mc.options.forwardKey, false);
            currentState = State.FORWARD1;
        } else {
            // No collision - move left
            setKeyPressed(mc.options.leftKey, true);
            setKeyPressed(mc.options.forwardKey, true);
        }
    }

    private void handleRightMovement() {
        // Hold left mouse button for farming
        mc.options.attackKey.setPressed(true);

        // Check for collision to the right
        if (checkCollisionInDirection(90)) { // 90 degrees is right
            // Collision detected - stop moving right, transition to forward
            setKeyPressed(mc.options.rightKey, false);
            setKeyPressed(mc.options.forwardKey, false);
            currentState = State.FORWARD2;
        } else {
            // No collision - move right
            setKeyPressed(mc.options.rightKey, true);
            setKeyPressed(mc.options.forwardKey, true);
        }
    }

    private void handleForwardMovement(State nextState) {
        // Hold left mouse button for farming
        mc.options.attackKey.setPressed(true);

        // Set forward key as pressed
        setKeyPressed(mc.options.forwardKey, true);
        releaseOtherMovementKeys(mc.options.forwardKey);

        // Check for collision in front
        if (checkCollisionInDirection(0)) { // 0 degrees is forward
            // Collision - transition to next state
            currentState = nextState;
        }
    }

    private boolean checkCollisionInDirection(float yawOffset) {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        Vec3d playerPos = mc.player.getPos();
        float playerYaw = mc.player.getYaw() + yawOffset;

        // Convert yaw to radians and calculate direction
        double radians = Math.toRadians(playerYaw);
        double x = -Math.sin(radians);
        double z = Math.cos(radians);

        // Check collision at player height
        Vec3d start = playerPos.add(0, 0.5, 0); // Check at player's mid height
        Vec3d end = start.add(x * 0.5, 0, z * 0.5); // Check 0.5 blocks ahead

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.BLOCK;
    }

    private int countWheatInDirection(float yawOffset, double distance) {
        if (mc.player == null || mc.world == null) {
            return 0;
        }

        int wheatCount = 0;
        Vec3d playerPos = mc.player.getPos();
        float playerYaw = mc.player.getYaw() + yawOffset;

        // Convert yaw to radians and calculate direction
        double radians = Math.toRadians(playerYaw);
        double x = -Math.sin(radians);
        double z = Math.cos(radians);

        // Check multiple points along the direction
        for (double d = 0.5; d <= distance; d += 0.5) {
            Vec3d checkPos = playerPos.add(x * d, 0, z * d);
            BlockPos blockPos = new BlockPos((int) checkPos.x, (int) playerPos.y, (int) checkPos.z);
            BlockState state = mc.world.getBlockState(blockPos);

            // Check if the block is wheat
            if (state.getBlock() == Blocks.WHEAT) {
                wheatCount++;
            }
        }

        return wheatCount;
    }

    private void setKeyPressed(KeyBinding keyBinding, boolean pressed) {
        if (keyBinding == null) return;
        keyBinding.setPressed(pressed);
    }

    private void setMouseButtonPressed(int button, boolean pressed) {
        if (mc.options == null) return;

        try {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                mc.options.attackKey.setPressed(pressed);
            }
        } catch (Exception e) {
            // Handle errors safely
        }
    }

    private void releaseOtherMovementKeys(KeyBinding exceptKey) {
        if (mc.options.forwardKey != exceptKey) {
            setKeyPressed(mc.options.forwardKey, false);
        }
        if (mc.options.backKey != exceptKey) {
            setKeyPressed(mc.options.backKey, false);
        }
        if (mc.options.leftKey != exceptKey) {
            setKeyPressed(mc.options.leftKey, false);
        }
        if (mc.options.rightKey != exceptKey) {
            setKeyPressed(mc.options.rightKey, false);
        }
    }

    private void releaseAllKeys() {
        setKeyPressed(mc.options.forwardKey, false);
        setKeyPressed(mc.options.backKey, false);
        setKeyPressed(mc.options.leftKey, false);
        setKeyPressed(mc.options.rightKey, false);
        setMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT, false);
        mc.options.attackKey.setPressed(false);
    }

    private void renderfarmstats(DrawContext context, RenderTickCounter tickCounter) {
        if (!this.isToggled()) return;
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        float startX = screenWidth - 150 - 10;
        BuiltBlur blur = Builder.blur()
                .size(new SizeState(150, 200))
                .radius(new QuadRadiusState(5f))
                .blurRadius(15f)
                .smoothness(1f)
                .color(new QuadColorState(Color.WHITE))
                .build();
        blur.render(matrix, startX, 10);

        // Render background rectangle for this module
        BuiltRectangle rectangle = Builder.rectangle()
                .size(new SizeState(150, 200))
                .color(new QuadColorState(new Color(36, 36, 36, 100)))
                .radius(new QuadRadiusState(5f))
                .smoothness(1.0f)
                .build();
        rectangle.render(matrix, startX, 10);

        BuiltText text = Builder.text()
                .font(NIGA_FONT.get())
                .text("Wheat (S-Shape)")
                .color(new Color(255, 255, 255, 255))
                .size(10)
                .thickness(0.01f)
                .build();
        text.render(matrix, startX + 5, 15);

        BuiltText text1 = Builder.text()
                .font(NIGA_FONT.get()) // Ensure NIGA_FONT is defined in your codebase
                .text("BPS: ")
                .color(new Color(152, 152, 152, 255))
                .size(8)
                .thickness(0.01f)
                .build();
        text1.render(matrix, startX + 7, 30);
        MinecraftClient client = MinecraftClient.getInstance();
        double dx = client.player.getX() - client.player.prevX;
        double dz = client.player.getZ() - client.player.prevZ;
        double bps1 = Math.sqrt(dx * dx + dz * dz) * 20.0;
        BuiltText text2 = Builder.text()
                .font(NIGA_FONT.get()) // Ensure NIGA_FONT is defined in your codebase
                .text(String.format("%.2f", bps1))
                .color(new Color(255, 255, 255, 255))
                .size(8)
                .thickness(0.01f)
                .build();
        text2.render(matrix, startX + 25, 30);

    }
    @Override
    public void onDisable() {
        System.out.println("2");
        releaseAllKeys();
        currentState = State.LEFT; // Reset state
    }

    @Override
    public void onEnable() {
        System.out.println("1");
        // Determine initial direction based on wheat presence
        int leftWheat = countWheatInDirection(-90, 5.0); // Check left side (5 blocks)
        int rightWheat = countWheatInDirection(90, 5.0); // Check right side (5 blocks)

        // If significantly fewer wheat blocks on the left, start with LEFT
        // If significantly fewer wheat blocks on the right, start with RIGHT
        // Threshold: difference of at least 2 wheat blocks to avoid small variations
        if (leftWheat < rightWheat - 1) {
            currentState = State.LEFT;
        } else if (rightWheat < leftWheat - 1) {
            currentState = State.RIGHT;
        } else {
            currentState = State.LEFT; // Default to left if counts are similar
        }

        lastActionTime = System.currentTimeMillis();
        wasScreenOpen = false;
        screenClosedTime = 0;
    }
}