package dev.sxmurxy.mre.modules.pathfinding.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;

/**
 * Comprehensive configuration for the pathfinding system
 * Contains all settings for pathfinding behavior, humanization, and performance
 */
public class PathfinderConfig {

    // ==================== CORE PATHFINDING SETTINGS ====================

    /** Maximum distance player can fall without taking damage */
    public static final int MAX_FALL_DISTANCE = 4;

    /** How smooth the path should be (0.0 = angular, 1.0 = very smooth) */
    public static final double PATH_SMOOTHNESS = 0.3;

    /** Ticks before recalculating path when stuck */
    public static final int RECALCULATE_STUCK_TICKS = 100;

    /** Maximum time allowed for path calculation in milliseconds */
    public static final long PATH_CALCULATION_TIMEOUT_MS = 5000;

    /** Maximum number of nodes to keep in cache */
    public static final int MAX_CACHED_NODES = 8000;

    /** Maximum search distance for pathfinding */
    public static final double MAX_SEARCH_DISTANCE = 1000.0;

    // ==================== MOVEMENT HUMANIZATION ====================

    /** Base random movement variation intensity */
    public static final double MOVEMENT_VARIATION_BASE = 0.15;

    /** Strength of path prediction (looking ahead) */
    public static final double MOVEMENT_PREDICTION_STRENGTH = 0.1;

    /** Intensity of side-to-side wiggle movement */
    public static final double MOVEMENT_WIGGLE_INTENSITY = 0.08;

    /** Frequency of wiggle movement */
    public static final double MOVEMENT_WIGGLE_FREQUENCY = 0.003;

    /** Threshold for forward movement activation */
    public static final double MOVEMENT_FORWARD_THRESHOLD = 0.15;

    /** Threshold for strafe movement activation */
    public static final double MOVEMENT_STRAFE_THRESHOLD = 0.1;

    // Movement timing settings
    /** Base delay between movement updates in milliseconds */
    public static final int MOVEMENT_BASE_DELAY_MS = 50;

    /** Random variation in movement delay */
    public static final int MOVEMENT_DELAY_VARIATION_MS = 30;

    /** Minimum movement delay */
    public static final int MOVEMENT_MIN_DELAY_MS = 20;

    /** Maximum movement delay for safety */
    public static final int MOVEMENT_MAX_DELAY_MS = 150;

    // ==================== ROTATION HUMANIZATION ====================

    /** Base rotation speed per update */
    public static final float ROTATION_BASE_SPEED = 2.5f;

    /** Speed multiplier for large angle rotations */
    public static final float ROTATION_ANGLE_SPEED_MULTIPLIER = 1.5f;

    /** Distance to start looking at next waypoint */
    public static final double ROTATION_LOOK_AHEAD_DISTANCE = 2.5;

    // Rotation timing settings
    /** Base delay between rotation updates */
    public static final int ROTATION_BASE_DELAY_MS = 40;

    /** Random variation in rotation delay */
    public static final int ROTATION_DELAY_VARIATION_MS = 25;

    /** Minimum rotation delay */
    public static final int ROTATION_MIN_DELAY_MS = 15;

    // Rotation noise and imperfection
    /** Intensity of look target randomization */
    public static final double ROTATION_NOISE_INTENSITY = 0.2;

    /** Amplitude of rotation noise */
    public static final float ROTATION_NOISE_AMPLITUDE = 0.8f;

    /** How often to update rotation noise */
    public static final long ROTATION_NOISE_UPDATE_INTERVAL = 150;

    /** Acceptable rotation accuracy in degrees */
    public static final float ROTATION_ACCURACY_THRESHOLD = 5.0f;

    // Look behavior settings
    /** Chance to glance at final destination */
    public static final double ROTATION_FINAL_DESTINATION_LOOK_CHANCE = 0.02;

    /** Duration of looking at final destination */
    public static final long ROTATION_FINAL_DESTINATION_LOOK_DURATION = 3000;

    /** Chance for random idle head movement */
    public static final double ROTATION_IDLE_MOVEMENT_CHANCE = 0.001;

    // ==================== SPECIAL MOVEMENT SETTINGS ====================

    /** Probability of jumping when jump is needed */
    public static final double JUMP_CHANCE_PER_TICK = 0.7;

    /** Distance threshold to start sneaking for precision */
    public static final double SNEAK_DISTANCE_THRESHOLD = 0.5;

    /** Minimum distance to allow sprinting */
    public static final double SPRINT_DISTANCE_THRESHOLD = 3.0;

    /** Probability of starting sprint when appropriate */
    public static final double SPRINT_CHANCE_PER_TICK = 0.8;

    /** Probability of stopping sprint for humanization */
    public static final double SPRINT_STOP_CHANCE_PER_TICK = 0.05;

    // ==================== STUCK DETECTION SETTINGS ====================

    /** Minimum movement distance to not be considered stuck */
    public static final double STUCK_MOVEMENT_THRESHOLD = 0.1;

    /** Ticks before player is considered stuck */
    public static final int STUCK_DETECTION_TICKS = 60;

    /** Maximum time to spend trying to unstick before giving up */
    public static final int MAX_UNSTICK_TIME_MS = 5000;

    // ==================== 3D NAVIGATION SETTINGS ====================

    /** Maximum horizontal distance for parkour jumps */
    public static final double MAX_PARKOUR_DISTANCE = 5.0;

    /** Minimum gap size to attempt parkour */
    public static final double MIN_PARKOUR_GAP = 2.0;

    /** Maximum height difference for jump movements */
    public static final int MAX_JUMP_HEIGHT = 1;

    /** Maximum height for climbing movements */
    public static final int MAX_CLIMB_HEIGHT = 3;

    /** Cost multiplier for dangerous movements */
    public static final double DANGER_COST_MULTIPLIER = 3.0;

    /** Cost multiplier for parkour movements */
    public static final double PARKOUR_COST_MULTIPLIER = 2.5;

    /** Cost multiplier for climbing movements */
    public static final double CLIMB_COST_MULTIPLIER = 1.8;

    // ==================== PERFORMANCE SETTINGS ====================

    /** Maximum iterations for A* algorithm */
    public static final int MAX_ASTAR_ITERATIONS = 15000;

    /** Interval for cache cleanup during pathfinding */
    public static final int CACHE_CLEANUP_INTERVAL = 2000;

    /** Maximum memory usage for node cache (MB) */
    public static final int MAX_CACHE_MEMORY_MB = 64;

    /** Chunk loading radius for pathfinding */
    public static final double CHUNK_LOADING_RADIUS = 64.0;

    // ==================== DYNAMIC METHODS ====================

    /**
     * Get reach distance based on player state and movement speed
     */
    public double getReachDistance(ClientPlayerEntity player) {
        if (player == null) return 1.2;

        double baseReach = 1.2;

        // Adjust for player velocity
        double velocity = player.getVelocity().length();
        double velocityBonus = Math.min(0.5, velocity * 0.3);

        // Adjust for sprinting
        if (player.isSprinting()) {
            baseReach += 0.3;
        }

        // Adjust for jumping
        if (!player.isOnGround()) {
            baseReach += 0.2;
        }

        return baseReach + velocityBonus;
    }

    /**
     * Get safe Y level based on world type and position
     */
    public int getSafeYLevel(BlockPos pos) {
        int currentY = pos.getY();

        // Overworld
        if (currentY > -60) {
            return Math.max(5, currentY - MAX_FALL_DISTANCE);
        }

        // Nether/caves
        return Math.max(-55, currentY - MAX_FALL_DISTANCE);
    }

    /**
     * Check if position is in safely loaded area
     */
    public boolean isPositionSafelyLoaded(BlockPos pos) {
        // Check world boundaries
        if (Math.abs(pos.getX()) > 29999999 || Math.abs(pos.getZ()) > 29999999) {
            return false;
        }

        // Check Y boundaries
        if (pos.getY() < -64 || pos.getY() > 320) {
            return false;
        }

        return true;
    }

    /**
     * Get environment cost multiplier based on location and conditions
     */
    public double getEnvironmentCostMultiplier(BlockPos pos) {
        double multiplier = 1.0;

        // Height penalties
        if (pos.getY() < 10) {
            multiplier *= 1.3; // Dangerous near void
        } else if (pos.getY() > 200) {
            multiplier *= 1.1; // Slightly more expensive at height
        }

        // Nether penalty
        if (pos.getY() < 0 && pos.getY() > -64) {
            multiplier *= 1.2; // Nether is more dangerous
        }

        return multiplier;
    }

    /**
     * Get maximum search distance based on performance settings and distance
     */
    public double getMaxSearchDistance(BlockPos start, BlockPos goal) {
        double directDistance = Math.sqrt(start.getSquaredDistance(goal));

        // Adjust max search based on direct distance
        if (directDistance < 100) {
            return MAX_SEARCH_DISTANCE;
        } else if (directDistance < 500) {
            return MAX_SEARCH_DISTANCE * 1.5; // Allow longer search for medium distances
        } else {
            return MAX_SEARCH_DISTANCE * 2.0; // Maximum search for very long distances
        }
    }

    /**
     * Get node expansion limit based on search complexity
     */
    public int getNodeExpansionLimit(BlockPos start, BlockPos goal) {
        double distance = Math.sqrt(start.getSquaredDistance(goal));

        if (distance < 50) {
            return 5000; // High detail for short paths
        } else if (distance < 200) {
            return 8000; // Medium detail for medium paths
        } else {
            return MAX_CACHED_NODES; // Maximum for long paths
        }
    }

    /**
     * Check if path should be recalculated based on time and conditions
     */
    public boolean shouldRecalculatePath(long lastCalculationTime) {
        long timeSinceCalc = System.currentTimeMillis() - lastCalculationTime;

        // Recalculate after 30 seconds for optimization
        if (timeSinceCalc > 30000) {
            return true;
        }

        // More frequent recalculation if player has speed effects
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.hasStatusEffect(StatusEffects.SPEED)) {
            return timeSinceCalc > 15000; // 15 seconds with speed effect
        }

        return false;
    }

    /**
     * Get humanization intensity based on settings and context
     */
    public double getHumanizationIntensity() {
        return 0.7; // Balanced humanization (0.0 = robotic, 1.0 = very human)
    }

    /**
     * Get performance priority (affects quality vs speed tradeoff)
     */
    public double getPerformancePriority() {
        return 0.3; // Slightly favor quality over pure performance
    }

    /**
     * Check if parkour movements should be allowed based on context
     */
    public boolean shouldAllowParkour(BlockPos currentPos, BlockPos targetPos) {
        // Don't attempt parkour if too far
        double distance = Math.sqrt(currentPos.getSquaredDistance(targetPos));
        if (distance > 64) return false;

        // Don't attempt parkour at dangerous heights
        if (currentPos.getY() < 10) return false;

        return true;
    }

    /**
     * Check if climbing should be allowed
     */
    public boolean shouldAllowClimbing(BlockPos pos) {
        // Allow climbing unless at dangerous heights
        return pos.getY() > 5;
    }

    /**
     * Get movement delay based on current context
     */
    public int getContextualMovementDelay() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return MOVEMENT_BASE_DELAY_MS;

        int delay = MOVEMENT_BASE_DELAY_MS;

        // Faster updates when sprinting
        if (player.isSprinting()) {
            delay = (int) (delay * 0.7);
        }

        // Slower updates when sneaking (more precise)
        if (player.isSneaking()) {
            delay = (int) (delay * 1.5);
        }

        // Faster updates with speed effect
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            delay = (int) (delay * 0.8);
        }

        return Math.max(MOVEMENT_MIN_DELAY_MS, Math.min(MOVEMENT_MAX_DELAY_MS, delay));
    }

    /**
     * Get rotation speed based on context
     */
    public float getContextualRotationSpeed() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return ROTATION_BASE_SPEED;

        float speed = ROTATION_BASE_SPEED;

        // Faster rotation when moving fast
        if (player.getVelocity().length() > 0.3) {
            speed *= 1.2f;
        }

        // Slower rotation when sneaking
        if (player.isSneaking()) {
            speed *= 0.7f;
        }

        return speed;
    }

    /**
     * Check if should use aggressive pathfinding (less humanization, more direct)
     */
    public boolean shouldUseAggressivePathfinding(double distanceToGoal) {
        // Use aggressive pathfinding for very long distances
        return distanceToGoal > 500;
    }

    /**
     * Get jump timing based on context (some jumps need precise timing)
     */
    public int getJumpTiming() {
        // Return delay in milliseconds before executing jump
        return 50 + (int)(Math.random() * 30); // 50-80ms delay
    }

    /**
     * Get configuration summary for debugging
     */
    public String getConfigSummary() {
        return String.format("""
            Pathfinder Configuration:
            - Max Fall: %d blocks
            - Path Smoothness: %.1f
            - Humanization: %.1f
            - Performance Priority: %.1f
            - Max Search Distance: %.0f blocks
            - Movement Delay: %d-%dms
            - Rotation Speed: %.1f
            """,
                MAX_FALL_DISTANCE,
                PATH_SMOOTHNESS,
                getHumanizationIntensity(),
                getPerformancePriority(),
                MAX_SEARCH_DISTANCE,
                MOVEMENT_MIN_DELAY_MS, MOVEMENT_MAX_DELAY_MS,
                ROTATION_BASE_SPEED
        );
    }
}