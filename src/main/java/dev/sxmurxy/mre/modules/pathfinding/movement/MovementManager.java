package dev.sxmurxy.mre.modules.pathfinding.movement;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Handles humanized movement with realistic timing, variations, and imperfections
 *
 * Features:
 * - Random movement variations and wiggles
 * - Variable speed and timing delays
 * - Realistic acceleration and deceleration
 * - Stuck detection and recovery
 * - Natural movement patterns that look human
 */
public class MovementManager {

    private final PathfinderConfig config;
    private final Random random = new Random();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Movement state tracking
    private Vec3d lastTargetPos = Vec3d.ZERO;
    private Vec3d lastPlayerPos = Vec3d.ZERO;
    private long lastMovementTime = 0;
    private int stuckTicks = 0;
    private boolean isUnsticking = false;

    // Humanization state
    private long nextMovementDelay = 0;
    private Vec3d currentVariation = Vec3d.ZERO;
    private double speedMultiplier = 1.0;
    private boolean wasMoving = false;

    // Movement rhythm (for natural walking patterns)
    private long movementStartTime = 0;
    private double movementPhase = 0.0;
    private boolean isAccelerating = true;

    public MovementManager(PathfinderConfig config) {
        this.config = config;
        this.movementStartTime = System.currentTimeMillis();
    }

    /**
     * Update player movement towards target with humanization
     */
    public void updateMovement(ClientPlayerEntity player, Vec3d targetNode, Vec3d nextNode) {
        if (player == null || targetNode == null) return;

        long currentTime = System.currentTimeMillis();

        // Apply movement delay for humanization
        if (currentTime < nextMovementDelay) {
            return; // Wait for delay to pass
        }

        Vec3d playerPos = player.getPos();

        // Update stuck detection
        updateStuckDetection(player);

        // Calculate target with humanization variations
        Vec3d adjustedTarget = applyMovementVariations(targetNode, nextNode, playerPos);

        // Apply movement keys and actions
        applyMovement(player, adjustedTarget, playerPos);

        // Set next movement delay
        scheduleNextMovementUpdate();

        // Update state
        lastTargetPos = targetNode;
        lastMovementTime = currentTime;
    }

    /**
     * Apply various humanization variations to the target position
     */
    private Vec3d applyMovementVariations(Vec3d target, Vec3d nextNode, Vec3d playerPos) {
        Vec3d adjustedTarget = target;

        // 1. Base random variation
        adjustedTarget = addRandomVariation(adjustedTarget);

        // 2. Path prediction (look slightly ahead)
        adjustedTarget = addPathPrediction(adjustedTarget, nextNode);

        // 3. Wiggle movement (side-to-side)
        adjustedTarget = addWiggleMovement(adjustedTarget, playerPos);

        // 4. Momentum-based adjustment
        adjustedTarget = addMomentumAdjustment(adjustedTarget, playerPos);

        return adjustedTarget;
    }

    /**
     * Add basic random variation to movement
     */
    private Vec3d addRandomVariation(Vec3d target) {
        double intensity = config.MOVEMENT_VARIATION_BASE;
        double xVar = (random.nextGaussian() - 0.5) * intensity;
        double zVar = (random.nextGaussian() - 0.5) * intensity;

        return target.add(xVar, 0, zVar);
    }

    /**
     * Add path prediction (slight movement toward next waypoint)
     */
    private Vec3d addPathPrediction(Vec3d target, Vec3d nextNode) {
        if (nextNode == null || nextNode.equals(target)) {
            return target;
        }

        Vec3d pathDirection = nextNode.subtract(target).normalize();
        double strength = config.MOVEMENT_PREDICTION_STRENGTH * random.nextDouble();

        return target.add(pathDirection.multiply(strength));
    }

    /**
     * Add wiggle movement for natural walking pattern
     */
    private Vec3d addWiggleMovement(Vec3d target, Vec3d playerPos) {
        double time = System.currentTimeMillis() * config.MOVEMENT_WIGGLE_FREQUENCY;
        double wiggleIntensity = config.MOVEMENT_WIGGLE_INTENSITY;

        // Create periodic side-to-side movement
        double wiggle = Math.sin(time) * wiggleIntensity;

        // Calculate perpendicular direction for wiggle
        Vec3d direction = target.subtract(playerPos);
        if (direction.length() > 0.1) {
            direction = direction.normalize();
            Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x);
            return target.add(perpendicular.multiply(wiggle));
        }

        return target;
    }

    /**
     * Add momentum-based movement adjustment
     */
    private Vec3d addMomentumAdjustment(Vec3d target, Vec3d playerPos) {
        Vec3d velocity = mc.player.getVelocity();

        // If moving fast, slightly overshoot target
        if (velocity.length() > 0.2) {
            Vec3d direction = target.subtract(playerPos).normalize();
            double overshoot = velocity.length() * 0.3;
            return target.add(direction.multiply(overshoot));
        }

        return target;
    }

    /**
     * Apply actual movement keys and actions
     */
    private void applyMovement(ClientPlayerEntity player, Vec3d target, Vec3d playerPos) {
        // Reset all movement keys first
        resetMovementKeys();

        Vec3d direction = target.subtract(playerPos);
        double distance = direction.length();

        if (distance < 0.1) {
            return; // Too close, no movement needed
        }

        direction = direction.normalize();

        // Calculate movement relative to player's facing direction
        Vec3d facingDirection = Vec3d.fromPolar(0, player.getYaw());
        double forward = direction.dotProduct(facingDirection);
        double strafe = direction.dotProduct(new Vec3d(-facingDirection.z, 0, facingDirection.x));

        // Apply movement with humanized thresholds
        applyDirectionalMovement(forward, strafe);

        // Handle special movements
        handleSpecialMovements(player, target, distance);

        // Handle sprinting with humanization
        handleSprinting(player, forward, distance);

        // Update movement rhythm
        updateMovementRhythm();
    }

    /**
     * Apply directional movement keys with humanized thresholds
     */
    private void applyDirectionalMovement(double forward, double strafe) {
        // Apply humanized thresholds with slight randomization
        double forwardThreshold = config.MOVEMENT_FORWARD_THRESHOLD +
                (random.nextGaussian() - 0.5) * 0.05;
        double strafeThreshold = config.MOVEMENT_STRAFE_THRESHOLD +
                (random.nextGaussian() - 0.5) * 0.03;

        // Forward/backward movement
        if (forward > forwardThreshold) {
            mc.options.forwardKey.setPressed(true);
        } else if (forward < -forwardThreshold) {
            mc.options.backKey.setPressed(true);
        }

        // Left/right movement
        if (strafe > strafeThreshold) {
            mc.options.rightKey.setPressed(true);
        } else if (strafe < -strafeThreshold) {
            mc.options.leftKey.setPressed(true);
        }
    }

    /**
     * Handle special movements like jumping, sneaking, climbing
     */
    private void handleSpecialMovements(ClientPlayerEntity player, Vec3d target, double distance) {
        BlockPos targetBlock = BlockPos.ofFloored(target);
        BlockPos playerBlock = player.getBlockPos();

        // Jumping logic with humanized timing
        if (shouldJump(player, targetBlock, playerBlock)) {
            if (random.nextDouble() < config.JUMP_CHANCE_PER_TICK) {
                mc.options.jumpKey.setPressed(true);
            }
        }

        // Sneaking for precise movement
        if (shouldSneak(distance)) {
            mc.options.sneakKey.setPressed(true);
        }
    }

    /**
     * Determine if player should jump
     */
    private boolean shouldJump(ClientPlayerEntity player, BlockPos target, BlockPos playerPos) {
        // Jump if target is higher
        if (target.getY() > playerPos.getY()) {
            return true;
        }

        // Jump over obstacles
        Vec3d direction = Vec3d.ofCenter(target).subtract(player.getPos()).normalize();
        BlockPos frontBlock = playerPos.offset(player.getHorizontalFacing());

        if (!mc.world.getBlockState(frontBlock).isAir()) {
            return true;
        }

        // Random jumping for humanization (very rare)
        return random.nextDouble() < 0.001;
    }

    /**
     * Determine if player should sneak
     */
    private boolean shouldSneak(double distance) {
        return distance < config.SNEAK_DISTANCE_THRESHOLD;
    }

    /**
     * Handle sprinting with humanization
     */
    private void handleSprinting(ClientPlayerEntity player, double forward, double distance) {
        boolean shouldSprint = forward > 0.7 && distance > config.SPRINT_DISTANCE_THRESHOLD;

        if (shouldSprint) {
            // Humanized sprinting - not always instant
            if (random.nextDouble() < config.SPRINT_CHANCE_PER_TICK) {
                player.setSprinting(true);
            }
        } else {
            // Occasionally stop sprinting for realism
            if (player.isSprinting() && random.nextDouble() < config.SPRINT_STOP_CHANCE_PER_TICK) {
                player.setSprinting(false);
            }
        }
    }

    /**
     * Update movement rhythm for natural walking patterns
     */
    private void updateMovementRhythm() {
        long currentTime = System.currentTimeMillis();
        double timeSinceStart = (currentTime - movementStartTime) / 1000.0;

        // Create natural movement rhythm (slight speed variations)
        movementPhase = (timeSinceStart * 2.0) % (Math.PI * 2);
        double rhythmFactor = 0.9 + 0.1 * Math.sin(movementPhase);

        speedMultiplier = rhythmFactor;

        // Update acceleration/deceleration state
        if (wasMoving != isCurrentlyMoving()) {
            isAccelerating = !wasMoving;
            wasMoving = isCurrentlyMoving();
        }
    }

    /**
     * Check if player is currently moving
     */
    private boolean isCurrentlyMoving() {
        return mc.options.forwardKey.isPressed() ||
                mc.options.backKey.isPressed() ||
                mc.options.leftKey.isPressed() ||
                mc.options.rightKey.isPressed();
    }

    /**
     * Schedule next movement update with humanized delay
     */
    private void scheduleNextMovementUpdate() {
        int baseDelay = config.MOVEMENT_BASE_DELAY_MS;
        int variation = (int)(random.nextGaussian() * config.MOVEMENT_DELAY_VARIATION_MS);
        int totalDelay = Math.max(config.MOVEMENT_MIN_DELAY_MS, baseDelay + variation);

        // Adjust delay based on movement state
        if (isAccelerating) {
            totalDelay = (int)(totalDelay * 0.8); // Faster updates when starting
        } else if (mc.player != null && mc.player.isSprinting()) {
            totalDelay = (int)(totalDelay * 0.7); // Faster updates when sprinting
        }

        nextMovementDelay = System.currentTimeMillis() + totalDelay;
    }

    /**
     * Update stuck detection system
     */
    private void updateStuckDetection(ClientPlayerEntity player) {
        Vec3d currentPos = player.getPos();
        double movementThreshold = config.STUCK_MOVEMENT_THRESHOLD;

        if (lastPlayerPos.squaredDistanceTo(currentPos) < movementThreshold * movementThreshold) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            isUnsticking = false;
        }

        lastPlayerPos = currentPos;
    }

    /**
     * Check if player is stuck
     */
    public boolean isStuck() {
        return stuckTicks > config.STUCK_DETECTION_TICKS;
    }

    /**
     * Get number of ticks player has been stuck
     */
    public int getStuckTicks() {
        return stuckTicks;
    }

    /**
     * Try to unstick the player with random movements
     */
    public void tryToUnstick(ClientPlayerEntity player) {
        if (isUnsticking) return;

        isUnsticking = true;
        resetMovementKeys();

        // Try different unstick strategies
        int strategy = random.nextInt(8);
        switch (strategy) {
            case 0 -> mc.options.forwardKey.setPressed(true);
            case 1 -> mc.options.backKey.setPressed(true);
            case 2 -> mc.options.leftKey.setPressed(true);
            case 3 -> mc.options.rightKey.setPressed(true);
            case 4 -> {
                // Jump while moving
                mc.options.jumpKey.setPressed(true);
                mc.options.forwardKey.setPressed(true);
            }
            case 5 -> {
                // Strafe jump
                mc.options.jumpKey.setPressed(true);
                mc.options.rightKey.setPressed(true);
            }
            case 6 -> {
                // Back up and jump
                mc.options.backKey.setPressed(true);
                mc.options.jumpKey.setPressed(true);
            }
            case 7 -> {
                // Sprint jump forward
                mc.options.forwardKey.setPressed(true);
                mc.options.jumpKey.setPressed(true);
                if (player != null) player.setSprinting(true);
            }
        }

        // Add delay before next unstick attempt
        nextMovementDelay = System.currentTimeMillis() + 150 + random.nextInt(200);
    }

    /**
     * Reset stuck detector
     */
    public void resetStuckDetector() {
        stuckTicks = 0;
        isUnsticking = false;
        if (mc.player != null) {
            lastPlayerPos = mc.player.getPos();
        }
    }

    /**
     * Reset all movement keys to unpressed state
     */
    public static void resetMovementKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            setKeyPressed(mc.options.forwardKey, false);
            setKeyPressed(mc.options.backKey, false);
            setKeyPressed(mc.options.leftKey, false);
            setKeyPressed(mc.options.rightKey, false);
            setKeyPressed(mc.options.jumpKey, false);
            setKeyPressed(mc.options.sneakKey, false);
        }
    }

    /**
     * Set key binding pressed state
     */
    private static void setKeyPressed(KeyBinding key, boolean pressed) {
        if (key != null) {
            key.setPressed(pressed);
        }
    }

    /**
     * Get current movement statistics for debugging
     */
    public MovementStats getStats() {
        return new MovementStats(
                stuckTicks,
                isUnsticking,
                speedMultiplier,
                isCurrentlyMoving(),
                System.currentTimeMillis() - lastMovementTime
        );
    }

    /**
     * Movement statistics for debugging and monitoring
     */
    public record MovementStats(
            int stuckTicks,
            boolean isUnsticking,
            double speedMultiplier,
            boolean isMoving,
            long timeSinceLastMovement
    ) {
        public boolean isHealthy() {
            return stuckTicks < 30 && !isUnsticking && timeSinceLastMovement < 1000;
        }

        public String getStatusString() {
            if (isUnsticking) return "§eUnsticking";
            if (stuckTicks > 60) return "§cStuck (" + (stuckTicks / 20) + "s)";
            if (isMoving) return "§aMoving";
            return "§7Idle";
        }
    }
}