package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class MovementHumanizer {
    private final Random random = ThreadLocalRandom.current();
    private final Queue<Vec3d> recentDeviations = new ArrayDeque<>();
    private long lastDeviationTime = 0;
    private double currentSpeedMultiplier = 1.0;
    private long lastSpeedChange = 0;

    // Humanization constants
    private static final double MAX_DEVIATION_DISTANCE = 0.8;
    private static final double BASE_DEVIATION_CHANCE = 0.15;
    private static final long DEVIATION_COOLDOWN_MS = 2000;
    private static final double SPEED_VARIATION_RANGE = 0.3;
    private static final long SPEED_CHANGE_INTERVAL_MS = 3000;

    // Safe constructor - no validation here
    public MovementHumanizer() {
        // Initialize with safe defaults
        // All validation happens in the methods that actually use the player
        System.out.println("[MovementHumanizer] Created successfully");
    }

    public Vec3d getHumanizedTarget(Vec3d originalTarget, Vec3d playerPos, Vec3d playerVelocity) {
        // Safety check - return original if we can't humanize safely
        if (originalTarget == null || playerPos == null) {
            return originalTarget != null ? originalTarget : Vec3d.ZERO;
        }

        // Additional safety check for MinecraftClient
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return originalTarget; // Can't humanize without player reference
        }

        Vec3d target = originalTarget;

        try {
            // Apply random path deviations
            if (shouldApplyDeviation()) {
                target = applyPathDeviation(target, playerPos);
            }

            // Apply micro-adjustments based on "attention"
            target = applyAttentionVariation(target, playerPos);
        } catch (Exception e) {
            System.err.println("[MovementHumanizer] Error in getHumanizedTarget: " + e.getMessage());
            return originalTarget; // Return original on any error
        }

        return target;
    }

    private boolean shouldApplyDeviation() {
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDeviationTime < DEVIATION_COOLDOWN_MS) {
                return false;
            }

            return random.nextDouble() < BASE_DEVIATION_CHANCE;
        } catch (Exception e) {
            return false; // Don't deviate if there's any issue
        }
    }

    private Vec3d applyPathDeviation(Vec3d target, Vec3d playerPos) {
        try {
            lastDeviationTime = System.currentTimeMillis();

            // Create random deviation perpendicular to movement direction
            Vec3d direction = target.subtract(playerPos);
            if (direction.lengthSquared() < 0.001) {
                return target; // Too close to apply meaningful deviation
            }

            direction = direction.normalize();
            Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x);

            double deviationAmount = (random.nextDouble() - 0.5) * MAX_DEVIATION_DISTANCE;
            Vec3d deviation = perpendicular.multiply(deviationAmount);

            // Store for future reference
            recentDeviations.offer(deviation);
            if (recentDeviations.size() > 5) {
                recentDeviations.poll();
            }

            return target.add(deviation);
        } catch (Exception e) {
            System.err.println("[MovementHumanizer] Error in applyPathDeviation: " + e.getMessage());
            return target;
        }
    }

    private Vec3d applyAttentionVariation(Vec3d target, Vec3d playerPos) {
        try {
            // Simulate human attention lapses with small position variations
            double attentionNoise = (random.nextGaussian() * 0.1);
            Vec3d noise = new Vec3d(
                    random.nextGaussian() * attentionNoise,
                    0,
                    random.nextGaussian() * attentionNoise
            );

            return target.add(noise);
        } catch (Exception e) {
            System.err.println("[MovementHumanizer] Error in applyAttentionVariation: " + e.getMessage());
            return target;
        }
    }

    public double getHumanizedSpeed() {
        try {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastSpeedChange > SPEED_CHANGE_INTERVAL_MS) {
                // Change speed periodically
                currentSpeedMultiplier = 1.0 + (random.nextGaussian() * SPEED_VARIATION_RANGE);
                currentSpeedMultiplier = Math.max(0.5, Math.min(1.5, currentSpeedMultiplier));
                lastSpeedChange = currentTime;
            }

            return currentSpeedMultiplier;
        } catch (Exception e) {
            System.err.println("[MovementHumanizer] Error in getHumanizedSpeed: " + e.getMessage());
            return 1.0; // Return default speed on error
        }
    }

    public boolean shouldPauseMovement() {
        try {
            // Occasionally pause as humans do
            return random.nextDouble() < 0.005; // 0.5% chance per tick
        } catch (Exception e) {
            return false; // Don't pause if there's an issue
        }
    }

    public int getRandomPauseDuration() {
        try {
            return random.nextInt(10, 40); // 0.5 to 2 seconds
        } catch (Exception e) {
            return 20; // Default pause duration
        }
    }

    public Vec3d getImperfectRotation(Vec3d perfectDirection) {
        if (perfectDirection == null) {
            return Vec3d.ZERO;
        }

        try {
            // Add slight rotation imperfections
            double yawError = random.nextGaussian() * 2.0; // ±2 degrees standard deviation
            double pitchError = random.nextGaussian() * 1.0; // ±1 degree for pitch

            // Convert to radians and apply rotation
            double yawRad = Math.toRadians(yawError);
            double pitchRad = Math.toRadians(pitchError);

            // Apply yaw rotation
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            Vec3d rotatedDirection = new Vec3d(
                    perfectDirection.x * cos - perfectDirection.z * sin,
                    perfectDirection.y,
                    perfectDirection.x * sin + perfectDirection.z * cos
            );

            // Apply pitch rotation (simplified)
            double pitchCos = Math.cos(pitchRad);
            double pitchSin = Math.sin(pitchRad);

            Vec3d result = new Vec3d(
                    rotatedDirection.x * pitchCos,
                    rotatedDirection.y * pitchCos + rotatedDirection.length() * pitchSin,
                    rotatedDirection.z * pitchCos
            );

            // Ensure we return a normalized vector
            double length = result.length();
            if (length < 0.001) {
                return perfectDirection; // Return original if result is too small
            }

            return result.normalize();
        } catch (Exception e) {
            System.err.println("[MovementHumanizer] Error in getImperfectRotation: " + e.getMessage());
            return perfectDirection; // Return original direction on error
        }
    }

    // Utility method to safely get player information without throwing exceptions
    private boolean isPlayerAvailable() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            return mc != null && mc.player != null && mc.world != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Method to reset humanization state if needed
    public void reset() {
        try {
            recentDeviations.clear();
            lastDeviationTime = 0;
            currentSpeedMultiplier = 1.0;
            lastSpeedChange = 0;
            System.out.println("[MovementHumanizer] State reset successfully");
        } catch (Exception e) {
            System.err.println("[MovementHumanizer] Error resetting state: " + e.getMessage());
        }
    }

    // Debug method to check internal state
    public String getDebugInfo() {
        try {
            return String.format("MovementHumanizer[speed=%.2f, deviations=%d, lastDeviation=%dms ago]",
                    currentSpeedMultiplier,
                    recentDeviations.size(),
                    System.currentTimeMillis() - lastDeviationTime);
        } catch (Exception e) {
            return "MovementHumanizer[error getting debug info]";
        }
    }
}