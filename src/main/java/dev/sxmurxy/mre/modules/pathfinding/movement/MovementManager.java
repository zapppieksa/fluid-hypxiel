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
 * Uses only PathfinderConfig constants for all settings (no GUI dependency)
 *
 * Features:
 * - Random movement variations and wiggles using config constants
 * - Variable speed and timing delays from config
 * - Realistic acceleration and deceleration patterns
 * - Stuck detection and recovery mechanisms
 * - Natural movement patterns that look human
 * - Sprint management for parkour movements
 * - Sneak precision for tight spaces
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

    // Humanization state (using config constants)
    private long nextMovementDelay = 0;
    private Vec3d currentVariation = Vec3d.ZERO;
    private double speedMultiplier = 1.0;
    private boolean wasMoving = false;

    // Movement rhythm and timing
    private long movementStartTime = 0;
    private double movementPhase = 0.0;
    private boolean isAccelerating = true;

    // Special movement state
    private boolean shouldSprint = false;
    private boolean shouldSneak = false;
    private int jumpCooldown = 0;

    public MovementManager(PathfinderConfig config) {
        this.config = config;
        this.movementStartTime = System.currentTimeMillis();
    }

    /**
     * Update player movement towards target with humanization based on config
     */
    public void updateMovement(ClientPlayerEntity player, Vec3d targetNode, Vec3d nextNode) {
        if (player == null || targetNode == null) return;

        long currentTime = System.currentTimeMillis();

        // Apply movement delay for humanization (using config constants)
        if (currentTime < nextMovementDelay) {
            return; // Wait for delay to pass
        }

        Vec3d playerPos = player.getPos();

        // Update stuck detection
        updateStuckDetection(player);

        // Calculate target with humanization variations
        Vec3d adjustedTarget = applyMovementVariations(targetNode, nextNode, playerPos);

        // Determine special movement needs
        updateSpecialMovementState(playerPos, adjustedTarget, nextNode);

        // Apply movement keys and actions
        applyMovement(player, adjustedTarget, playerPos);

        // Set next movement delay (using config constants)
        scheduleNextMovementUpdate();

        // Update state
        lastTargetPos = targetNode;
        lastMovementTime = currentTime;

        // Update jump cooldown
        if (jumpCooldown > 0) jumpCooldown--;
    }

    /**
     * Apply various humanization variations to the target position
     */
    private Vec3d applyMovementVariations(Vec3d target, Vec3d nextNode, Vec3d playerPos) {
        Vec3d adjustedTarget = target;

        // 1. Base random variation (using config constants)
        adjustedTarget = addRandomVariation(adjustedTarget);

        // 2. Path prediction and look-ahead
        adjustedTarget = addPathPrediction(adjustedTarget, nextNode);

        // 3. Wiggle movement for natural human-like path
        adjustedTarget = addWiggleMovement(adjustedTarget, playerPos);

        // 4. Speed-based adjustments
        adjustedTarget = adjustSpeedVariations(adjustedTarget, playerPos);

        return adjustedTarget;
    }

    /**
     * Add random variation using config constants
     */
    private Vec3d addRandomVariation(Vec3d target) {
        double intensity = PathfinderConfig.MOVEMENT_VARIATION_BASE;

        double randomX = (random.nextGaussian() * intensity);
        double randomZ = (random.nextGaussian() * intensity);

        return target.add(randomX, 0, randomZ);
    }

    /**
     * Add path prediction for smooth movement
     */
    private Vec3d addPathPrediction(Vec3d target, Vec3d nextNode) {
        if (nextNode == null || nextNode.equals(target)) return target;

        double strength = PathfinderConfig.MOVEMENT_PREDICTION_STRENGTH;
        Vec3d prediction = nextNode.subtract(target).multiply(strength);

        return target.add(prediction);
    }

    /**
     * Add wiggle movement for natural human-like pathing
     */
    private Vec3d addWiggleMovement(Vec3d target, Vec3d playerPos) {
        long currentTime = System.currentTimeMillis();
        double timeFactor = currentTime * PathfinderConfig.MOVEMENT_WIGGLE_FREQUENCY;

        double wiggleX = Math.sin(timeFactor) * PathfinderConfig.MOVEMENT_WIGGLE_INTENSITY;
        double wiggleZ = Math.cos(timeFactor * 1.3) * PathfinderConfig.MOVEMENT_WIGGLE_INTENSITY;

        return target.add(wiggleX, 0, wiggleZ);
    }

    /**
     * Adjust target based on speed variations
     */
    private Vec3d adjustSpeedVariations(Vec3d target, Vec3d playerPos) {
        double distance = playerPos.distanceTo(target);

        // Slow down for precision near target
        if (distance < PathfinderConfig.SNEAK_DISTANCE_THRESHOLD) {
            shouldSneak = true;
            Vec3d direction = target.subtract(playerPos).normalize();
            return playerPos.add(direction.multiply(0.3));
        } else {
            shouldSneak = false;
        }

        return target;
    }

    /**
     * Update special movement state (sprint, sneak, jump)
     */
    private void updateSpecialMovementState(Vec3d playerPos, Vec3d target, Vec3d nextNode) {
        double distanceToTarget = playerPos.distanceTo(target);

        // Sprint management using config constants
        if (distanceToTarget > PathfinderConfig.SPRINT_DISTANCE_THRESHOLD &&
                !shouldSneak &&
                random.nextDouble() < PathfinderConfig.SPRINT_CHANCE_PER_TICK) {
            shouldSprint = true;
        } else if (shouldSprint &&
                random.nextDouble() < PathfinderConfig.SPRINT_STOP_CHANCE_PER_TICK) {
            shouldSprint = false;
        }

        // Override sprint for precision movements
        if (shouldSneak) {
            shouldSprint = false;
        }
    }

    /**
     * Apply movement keys and special actions
     */
    private void applyMovement(ClientPlayerEntity player, Vec3d target, Vec3d playerPos) {
        // Reset all movement keys first
        resetMovementKeys();

        // Calculate movement direction
        Vec3d direction = target.subtract(playerPos);
        if (direction.length() < 0.1) return; // Too close to move

        direction = direction.normalize();

        // Get player's facing direction for relative movement
        Vec3d facing = Vec3d.fromPolar(0, player.getYaw());
        Vec3d right = new Vec3d(-facing.z, 0, facing.x);

        // Calculate forward/backward and strafe components
        double forward = direction.dotProduct(facing);
        double strafe = direction.dotProduct(right);

        // Apply movement thresholds from config
        if (forward > PathfinderConfig.MOVEMENT_FORWARD_THRESHOLD) {
            mc.options.forwardKey.setPressed(true);
        } else if (forward < -PathfinderConfig.MOVEMENT_FORWARD_THRESHOLD) {
            mc.options.backKey.setPressed(true);
        }

        if (strafe > PathfinderConfig.MOVEMENT_STRAFE_THRESHOLD) {
            mc.options.rightKey.setPressed(true);
        } else if (strafe < -PathfinderConfig.MOVEMENT_STRAFE_THRESHOLD) {
            mc.options.leftKey.setPressed(true);
        }

        // Handle vertical movement
        handleVerticalMovement(player, target, playerPos);

        // Apply special movement states
        mc.options.sprintKey.setPressed(shouldSprint);
        mc.options.sneakKey.setPressed(shouldSneak);
    }

    /**
     * Handle jumping and vertical movement
     */
    private void handleVerticalMovement(ClientPlayerEntity player, Vec3d target, Vec3d playerPos) {
        double heightDiff = target.y - playerPos.y;

        // Jump if target is higher and we need to jump
        if (heightDiff > 0.5 && jumpCooldown == 0) {
            if (random.nextDouble() < PathfinderConfig.JUMP_CHANCE_PER_TICK) {
                mc.options.jumpKey.setPressed(true);
                jumpCooldown = 10; // Prevent spam jumping
            }
        }

        // Handle obstacles that require jumping
        if (isObstacleInFront(player, target) && jumpCooldown == 0) {
            if (random.nextDouble() < PathfinderConfig.JUMP_CHANCE_PER_TICK * 2) {
                mc.options.jumpKey.setPressed(true);
                jumpCooldown = 15;
            }
        }
    }

    /**
     * Check if there's an obstacle requiring jumping
     */
    private boolean isObstacleInFront(ClientPlayerEntity player, Vec3d target) {
        Vec3d direction = target.subtract(player.getPos()).normalize();
        Vec3d checkPos = player.getPos().add(direction.multiply(1.5));
        BlockPos blockPos = BlockPos.ofFloored(checkPos);

        return mc.world != null &&
                !mc.world.getBlockState(blockPos).isAir() &&
                mc.world.getBlockState(blockPos).isSolidBlock(mc.world, blockPos);
    }

    /**
     * Schedule next movement update using config constants
     */
    private void scheduleNextMovementUpdate() {
        int baseDelay = PathfinderConfig.MOVEMENT_BASE_DELAY_MS;
        int variation = PathfinderConfig.MOVEMENT_DELAY_VARIATION_MS;
        int minDelay = PathfinderConfig.MOVEMENT_MIN_DELAY_MS;
        int maxDelay = PathfinderConfig.MOVEMENT_MAX_DELAY_MS;

        int delay = baseDelay + (int)(random.nextGaussian() * variation);
        delay = Math.max(minDelay, Math.min(maxDelay, delay));

        nextMovementDelay = System.currentTimeMillis() + delay;
    }

    /**
     * Update stuck detection using config timeouts
     */
    private void updateStuckDetection(ClientPlayerEntity player) {
        Vec3d currentPlayerPos = player.getPos();

        if (currentPlayerPos.distanceTo(lastPlayerPos) < 0.1) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            isUnsticking = false;
        }

        lastPlayerPos = currentPlayerPos;
    }

    /**
     * Try to unstick player with random movements
     */
    public void tryToUnstick(ClientPlayerEntity player) {
        if (!isUnsticking) {
            isUnsticking = true;

            // Try random movement to unstick
            resetMovementKeys();

            // Random directional movement
            switch (random.nextInt(6)) {
                case 0 -> mc.options.forwardKey.setPressed(true);
                case 1 -> mc.options.backKey.setPressed(true);
                case 2 -> mc.options.leftKey.setPressed(true);
                case 3 -> mc.options.rightKey.setPressed(true);
                case 4 -> {
                    mc.options.jumpKey.setPressed(true);
                    mc.options.forwardKey.setPressed(true);
                }
                case 5 -> {
                    mc.options.sneakKey.setPressed(true);
                    mc.options.forwardKey.setPressed(true);
                }
            }
        }
    }

    /**
     * Reset stuck detector
     */
    public void resetStuckDetector() {
        stuckTicks = 0;
        isUnsticking = false;
    }

    /**
     * Check if player is stuck using config threshold
     */
    public boolean isStuck() {
        return stuckTicks > PathfinderConfig.RECALCULATE_STUCK_TICKS / 2; // Half threshold for early detection
    }

    /**
     * Get stuck tick count
     */
    public int getStuckTicks() {
        return stuckTicks;
    }

    /**
     * Reset all movement keys to prevent conflicts
     */
    public static void resetMovementKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
        }
    }

    /**
     * Get current movement state for debugging
     */
    public String getMovementState() {
        return String.format(
                "Movement State: stuck=%d, sprint=%s, sneak=%s, unsticking=%s, jumpCD=%d",
                stuckTicks, shouldSprint, shouldSneak, isUnsticking, jumpCooldown
        );
    }

    /**
     * Check if currently performing special movement
     */
    public boolean isPerformingSpecialMovement() {
        return shouldSprint || shouldSneak || isUnsticking;
    }

    /**
     * Get movement statistics
     */
    public String getStatistics() {
        long uptime = System.currentTimeMillis() - movementStartTime;
        return String.format(
                "Movement Statistics:\n" +
                        "  Uptime: %d seconds\n" +
                        "  Stuck incidents: %d ticks\n" +
                        "  Current state: %s\n" +
                        "  Special movement: %s",
                uptime / 1000, stuckTicks,
                isStuck() ? "Stuck" : "Moving",
                isPerformingSpecialMovement() ? "Active" : "Normal"
        );
    }

    /**
     * Emergency stop - reset everything
     */
    public void emergencyStop() {
        resetMovementKeys();
        resetStuckDetector();
        shouldSprint = false;
        shouldSneak = false;
        isUnsticking = false;
        jumpCooldown = 0;
    }
}