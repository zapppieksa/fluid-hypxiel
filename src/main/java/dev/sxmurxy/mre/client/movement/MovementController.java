package dev.sxmurxy.mre.client.movement;

import dev.sxmurxy.mre.client.pathfinding.Pathfinder;
import dev.sxmurxy.mre.client.rotations.RotationController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;
import java.util.Random;

/**
 * Advanced movement controller with intelligent key usage and humanized execution.
 * Implements optimal movement patterns using all available input keys (W, S, A, D, Space, Sprint, Sneak).
 * Handles AOTV teleportation and Etherwarp execution with proper humanization.
 */
public class MovementController {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    // Key bindings
    private final KeyBinding forwardKey;
    private final KeyBinding backKey;
    private final KeyBinding leftKey;
    private final KeyBinding rightKey;
    private final KeyBinding jumpKey;
    private final KeyBinding sprintKey;
    private final KeyBinding sneakKey;

    // Movement state
    private List<Pathfinder.PathNode> currentPath;
    private int pathIndex;
    private boolean isExecuting;

    // AOTV state - Random forward teleporting while walking
    private boolean aotvEnabled = true;
    private long lastAotvTime = 0;
    private static final long AOTV_COOLDOWN = 100; // ms
    private static final double AOTV_CHANCE = 0.3; // 30% chance per check

    // Etherwarp state - Stop, shift, aim, teleport
    private boolean etherwarpEnabled = true;
    private boolean isEtherwarping = false;
    private Vec3d etherwarpTarget;
    private long etherwarpStartTime = 0;

    // Movement optimization
    private Vec3d lastPlayerPos;
    private long stuckStartTime = 0;
    private static final long STUCK_THRESHOLD = 1000; // ms

    // Humanization
    private long lastMovementChange = 0;
    private static final long MIN_MOVEMENT_CHANGE_DELAY = 150; // ms

    public MovementController() {
        this.forwardKey = mc.options.forwardKey;
        this.backKey = mc.options.backKey;
        this.leftKey = mc.options.leftKey;
        this.rightKey = mc.options.rightKey;
        this.jumpKey = mc.options.jumpKey;
        this.sprintKey = mc.options.sprintKey;
        this.sneakKey = mc.options.sneakKey;

        this.lastPlayerPos = Vec3d.ZERO;
    }

    /**
     * Execute movement along the provided path with intelligent key usage.
     */
    public void executePath(List<Pathfinder.PathNode> path) {
        this.currentPath = path;
        this.pathIndex = 0;
        this.isExecuting = true;
        this.lastPlayerPos = mc.player != null ? mc.player.getPos() : Vec3d.ZERO;
        this.stuckStartTime = 0;
    }

    /**
     * Main movement tick - called every client tick.
     */
    public void tick() {
        if (!isExecuting || currentPath == null || mc.player == null) {
            releaseAllKeys();
            return;
        }

        // Check path completion
        if (pathIndex >= currentPath.size()) {
            completeExecution();
            return;
        }

        // Update stuck detection
        updateStuckDetection();

        // Handle etherwarp state
        if (isEtherwarping) {
            handleEtherwarpExecution();
            return;
        }

        Pathfinder.PathNode currentTarget = currentPath.get(pathIndex);

        // Check if we should advance to next waypoint
        if (shouldAdvanceWaypoint(currentTarget)) {
            pathIndex++;
            if (pathIndex >= currentPath.size()) {
                completeExecution();
                return;
            }
            currentTarget = currentPath.get(pathIndex);
        }

        // Execute movement based on target and move type
        executeMovementToTarget(currentTarget);
    }

    /**
     * Execute movement to target with optimal key combinations.
     */
    private void executeMovementToTarget(Pathfinder.PathNode target) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.position;
        double distance = playerPos.distanceTo(targetPos);

        // Handle special movement types
        switch (target.move) {
            case AOTV -> {
                executeAotv(targetPos, distance);
                return;
            }
            case ETHERWARP -> {
                initializeEtherwarp(targetPos);
                return;
            }
            case JUMP -> {
                executeJumpMovement(targetPos);
                return;
            }
            case FALL -> {
                executeFallMovement(targetPos);
                return;
            }
        }

        // Regular ground movement with optimal key usage
        executeGroundMovement(targetPos, distance, target.move == Pathfinder.MoveType.SPRINT);
    }

    /**
     * Execute ground movement with full key optimization (W, S, A, D, Sprint).
     */
    private void executeGroundMovement(Vec3d targetPos, double distance, boolean shouldSprint) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d direction = targetPos.subtract(playerPos).normalize();

        // Calculate player facing direction
        Vec3d facingDir = Vec3d.fromPolar(0, mc.player.getYaw());
        Vec3d rightDir = new Vec3d(-facingDir.z, 0, facingDir.x);

        // Calculate dot products for movement direction
        double forwardDot = direction.dotProduct(facingDir);
        double rightDot = direction.dotProduct(rightDir);

        // Determine optimal key combination
        boolean useForward = forwardDot > 0.2;
        boolean useBackward = forwardDot < -0.2;
        boolean useLeft = rightDot < -0.1;
        boolean useRight = rightDot > 0.1;

        // Apply keys with humanization
        setKey(forwardKey, useForward);
        setKey(backKey, useBackward);
        setKey(leftKey, useLeft);
        setKey(rightKey, useRight);

        // Sprint logic
        boolean sprint = shouldSprint && distance > 2.0 && useForward && !useBackward;
        setKey(sprintKey, sprint);

        // Rotate towards target
        RotationController.rotate(targetPos, RotationController.RotationType.MOVEMENT, false);
    }

    /**
     * AOTV execution - Random forward teleporting while walking (not cursor-based).
     */
    private void executeAotv(Vec3d targetPos, double distance) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastAotvTime < AOTV_COOLDOWN) {
            // Continue walking while AOTV is on cooldown
            setKey(forwardKey, true);
            setKey(sprintKey, true);
            return;
        }

        // Check if should teleport (random chance + distance check)
        if (distance > 5.0 && random.nextDouble() < AOTV_CHANCE) {
            // Calculate forward teleport position (8 blocks forward in facing direction)
            Vec3d facingDir = Vec3d.fromPolar(0, mc.player.getYaw());
            Vec3d teleportPos = mc.player.getPos().add(facingDir.multiply(8.0));

            // Check if teleport would bring us closer to target
            double currentDistance = mc.player.getPos().distanceTo(targetPos);
            double teleportDistance = teleportPos.distanceTo(targetPos);

            if (teleportDistance < currentDistance && canTeleportTo(teleportPos)) {
                // Execute AOTV teleport
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                lastAotvTime = currentTime;

                // Brief pause after teleport for humanization
                new Thread(() -> {
                    try {
                        Thread.sleep(50 + random.nextInt(100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                return;
            }
        }

        // Continue walking if not teleporting
        executeGroundMovement(targetPos, distance, true);
    }

    /**
     * Initialize etherwarp sequence: stop -> shift -> aim -> teleport.
     */
    private void initializeEtherwarp(Vec3d targetPos) {
        if (isEtherwarping) return;

        // Stop all movement
        releaseMovementKeys();

        // Start etherwarp sequence
        isEtherwarping = true;
        etherwarpTarget = targetPos;
        etherwarpStartTime = System.currentTimeMillis();

        // Start shifting
        setKey(sneakKey, true);
    }

    /**
     * Handle etherwarp execution sequence.
     */
    private void handleEtherwarpExecution() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - etherwarpStartTime;

        if (elapsedTime < 200) {
            // Phase 1: Shifting and aiming (200ms)
            setKey(sneakKey, true);
            RotationController.rotate(etherwarpTarget, RotationController.RotationType.ETHERWARP, false);

        } else if (elapsedTime < 400) {
            // Phase 2: Fine-tuning aim (200ms)
            if (RotationController.isRotationComplete(etherwarpTarget, 1.0f)) {
                // Execute etherwarp
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                completeEtherwarp();
            }

        } else {
            // Timeout - complete etherwarp anyway
            completeEtherwarp();
        }
    }

    /**
     * Complete etherwarp sequence.
     */
    private void completeEtherwarp() {
        isEtherwarping = false;
        etherwarpTarget = null;
        setKey(sneakKey, false);

        // Brief pause for humanization
        new Thread(() -> {
            try {
                Thread.sleep(100 + random.nextInt(200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Execute jump movement with proper timing and key combinations.
     */
    private void executeJumpMovement(Vec3d targetPos) {
        // Continue forward movement
        executeGroundMovement(targetPos, mc.player.getPos().distanceTo(targetPos), false);

        // Jump if on ground
        if (mc.player.isOnGround()) {
            setKey(jumpKey, true);

            // Release jump after one tick for vanilla behavior
            new Thread(() -> {
                try {
                    Thread.sleep(50); // One tick
                    setKey(jumpKey, false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Execute fall movement (mainly just forward movement).
     */
    private void executeFallMovement(Vec3d targetPos) {
        executeGroundMovement(targetPos, mc.player.getPos().distanceTo(targetPos), true);
    }

    /**
     * Humanized key setting with delay prevention.
     */
    private void setKey(KeyBinding key, boolean pressed) {
        long currentTime = System.currentTimeMillis();

        // Prevent rapid key changes for humanization
        if (currentTime - lastMovementChange < MIN_MOVEMENT_CHANGE_DELAY) {
            return;
        }

        if (key.isPressed() != pressed) {
            key.setPressed(pressed);
            lastMovementChange = currentTime;
        }
    }

    // Helper methods

    private boolean shouldAdvanceWaypoint(Pathfinder.PathNode target) {
        if (mc.player == null) return false;

        double distance = mc.player.getPos().distanceTo(target.position);
        double threshold = mc.player.getVelocity().length() * 2.0 + 1.2;

        return distance < threshold;
    }

    private boolean canTeleportTo(Vec3d pos) {
        // Basic check - would need more sophisticated collision detection in full implementation
        return pos.y > 0 && pos.y < 256;
    }

    private void updateStuckDetection() {
        if (mc.player == null) return;

        Vec3d currentPos = mc.player.getPos();
        double movement = currentPos.distanceTo(lastPlayerPos);

        if (movement < 0.1) {
            if (stuckStartTime == 0) {
                stuckStartTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - stuckStartTime > STUCK_THRESHOLD) {
                handleStuckRecovery();
            }
        } else {
            stuckStartTime = 0;
        }

        lastPlayerPos = currentPos;
    }

    private void handleStuckRecovery() {
        // Simple stuck recovery - jump and try different direction
        setKey(jumpKey, true);
        setKey(leftKey, random.nextBoolean());
        setKey(rightKey, !leftKey.isPressed());

        new Thread(() -> {
            try {
                Thread.sleep(500);
                releaseAllKeys();
                stuckStartTime = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void releaseMovementKeys() {
        setKey(forwardKey, false);
        setKey(backKey, false);
        setKey(leftKey, false);
        setKey(rightKey, false);
    }

    private void releaseAllKeys() {
        releaseMovementKeys();
        setKey(jumpKey, false);
        setKey(sprintKey, false);
        setKey(sneakKey, false);
    }

    private void completeExecution() {
        releaseAllKeys();
        isExecuting = false;
        currentPath = null;
        pathIndex = 0;
        isEtherwarping = false;
    }

    // Getters and setters
    public boolean isExecuting() { return isExecuting; }
    public int getCurrentPathIndex() { return pathIndex; }
    public int getPathLength() { return currentPath != null ? currentPath.size() : 0; }

    public void setAotvEnabled(boolean enabled) { this.aotvEnabled = enabled; }
    public void setEtherwarpEnabled(boolean enabled) { this.etherwarpEnabled = enabled; }
    public boolean isAotvEnabled() { return aotvEnabled; }
    public boolean isEtherwarpEnabled() { return etherwarpEnabled; }
}