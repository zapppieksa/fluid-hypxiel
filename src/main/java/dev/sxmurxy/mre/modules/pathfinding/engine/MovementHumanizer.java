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
    public MovementHumanizer() {
        if (MinecraftClient.getInstance().player == null) {
            throw new IllegalStateException("Player is null");
        }
    }
    public Vec3d getHumanizedTarget(Vec3d originalTarget, Vec3d playerPos, Vec3d playerVelocity) {
        Vec3d target = originalTarget;

        // Apply random path deviations
        if (shouldApplyDeviation()) {
            target = applyPathDeviation(target, playerPos);
        }

        // Apply micro-adjustments based on "attention"
        target = applyAttentionVariation(target, playerPos);

        return target;
    }

    private boolean shouldApplyDeviation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDeviationTime < DEVIATION_COOLDOWN_MS) {
            return false;
        }

        return random.nextDouble() < BASE_DEVIATION_CHANCE;
    }

    private Vec3d applyPathDeviation(Vec3d target, Vec3d playerPos) {
        lastDeviationTime = System.currentTimeMillis();

        // Create random deviation perpendicular to movement direction
        Vec3d direction = target.subtract(playerPos).normalize();
        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x);

        double deviationAmount = (random.nextDouble() - 0.5) * MAX_DEVIATION_DISTANCE;
        Vec3d deviation = perpendicular.multiply(deviationAmount);

        // Store for future reference
        recentDeviations.offer(deviation);
        if (recentDeviations.size() > 5) {
            recentDeviations.poll();
        }

        return target.add(deviation);
    }

    private Vec3d applyAttentionVariation(Vec3d target, Vec3d playerPos) {
        // Simulate human attention lapses with small position variations
        double attentionNoise = (random.nextGaussian() * 0.1);
        Vec3d noise = new Vec3d(
                random.nextGaussian() * attentionNoise,
                0,
                random.nextGaussian() * attentionNoise
        );

        return target.add(noise);
    }

    public double getHumanizedSpeed() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastSpeedChange > SPEED_CHANGE_INTERVAL_MS) {
            // Change speed periodically
            currentSpeedMultiplier = 1.0 + (random.nextGaussian() * SPEED_VARIATION_RANGE);
            currentSpeedMultiplier = Math.max(0.5, Math.min(1.5, currentSpeedMultiplier));
            lastSpeedChange = currentTime;
        }

        return currentSpeedMultiplier;
    }

    public boolean shouldPauseMovement() {
        // Occasionally pause as humans do
        return random.nextDouble() < 0.005; // 0.5% chance per tick
    }

    public int getRandomPauseDuration() {
        return random.nextInt(10, 40); // 0.5 to 2 seconds
    }

    public Vec3d getImperfectRotation(Vec3d perfectDirection) {
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

        return new Vec3d(
                rotatedDirection.x * pitchCos,
                rotatedDirection.y * pitchCos + rotatedDirection.length() * pitchSin,
                rotatedDirection.z * pitchCos
        ).normalize();
    }
}