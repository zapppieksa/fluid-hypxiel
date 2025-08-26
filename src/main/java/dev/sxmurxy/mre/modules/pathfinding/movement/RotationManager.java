package dev.sxmurxy.mre.modules.pathfinding.movement;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Handles humanized rotation with realistic looking patterns and imperfections
 *
 * Features:
 * - Smooth rotation with acceleration/deceleration
 * - Look-ahead behavior for natural pathfinding
 * - Random noise and imperfections
 * - Natural head movement patterns
 * - Realistic rotation speeds and timing
 */
public class RotationManager {

    private final PathfinderConfig config;
    private final Random random = new Random();

    // Current rotation state
    private float targetYaw = 0f;
    private float targetPitch = 0f;
    private float currentYaw = 0f;
    private float currentPitch = 0f;

    // Rotation timing
    private long lastRotationTime = 0;
    private long nextRotationUpdate = 0;

    // Humanization state
    private float rotationNoiseYaw = 0f;
    private float rotationNoisePitch = 0f;
    private long lastNoiseUpdate = 0;

    // Look behavior
    private Vec3d lastLookTarget = Vec3d.ZERO;
    private long lookAheadStartTime = 0;
    private boolean isLookingAhead = false;

    // Natural movement patterns
    private double headBobPhase = 0.0;
    private long movementStartTime = 0;
    private boolean wasMoving = false;

    public RotationManager(PathfinderConfig config) {
        this.config = config;
        this.movementStartTime = System.currentTimeMillis();
    }

    /**
     * Update player rotation with humanized movement patterns
     */
    public void updateRotation(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget, BlockPos finalDestination) {
        if (player == null) return;

        long currentTime = System.currentTimeMillis();

        // Check if we should update rotation (humanized timing)
        if (currentTime < nextRotationUpdate) {
            return;
        }

        // Calculate where to look
        Vec3d lookTarget = calculateLookTarget(player, currentTarget, nextTarget, finalDestination);

        // Update target rotation
        calculateTargetRotation(player, lookTarget);

        // Apply rotation with humanization
        applyHumanizedRotation(player, currentTime);

        // Schedule next update
        scheduleNextRotationUpdate();

        // Update state
        lastRotationTime = currentTime;
        lastLookTarget = lookTarget;
    }

    /**
     * Calculate optimal look target based on path and context
     */
    private Vec3d calculateLookTarget(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget, BlockPos finalDestination) {
        Vec3d playerPos = player.getPos();
        Vec3d baseLookTarget = currentTarget;

        // 1. Look-ahead system: look at next target when close to current
        if (nextTarget != null && !nextTarget.equals(currentTarget)) {
            double distanceToCurrent = playerPos.distanceTo(currentTarget);
            double lookAheadDistance = config.ROTATION_LOOK_AHEAD_DISTANCE;

            if (distanceToCurrent < lookAheadDistance) {
                double interpolationFactor = Math.max(0, 1.0 - distanceToCurrent / lookAheadDistance);
                baseLookTarget = currentTarget.lerp(nextTarget, interpolationFactor);
            }
        }

        // 2. Occasional glances at final destination (for realism)
        if (shouldLookAtDestination(finalDestination)) {
            if (!isLookingAhead) {
                isLookingAhead = true;
                lookAheadStartTime = System.currentTimeMillis();
            }

            if (isLookingAhead && System.currentTimeMillis() - lookAheadStartTime < config.ROTATION_FINAL_DESTINATION_LOOK_DURATION) {
                baseLookTarget = Vec3d.ofCenter(finalDestination);
            } else {
                isLookingAhead = false;
            }
        }

        // 3. Apply humanization variations
        return addLookVariations(baseLookTarget, playerPos);
    }

    /**
     * Determine if should look at final destination
     */
    private boolean shouldLookAtDestination(BlockPos destination) {
        if (destination == null) return false;

        return !isLookingAhead && random.nextDouble() < config.ROTATION_FINAL_DESTINATION_LOOK_CHANCE;
    }

    /**
     * Add humanization variations to look target
     */
    private Vec3d addLookVariations(Vec3d target, Vec3d playerPos) {
        double noiseIntensity = config.ROTATION_NOISE_INTENSITY;

        // Base random noise
        double xNoise = (random.nextGaussian() - 0.5) * noiseIntensity;
        double yNoise = (random.nextGaussian() - 0.5) * noiseIntensity * 0.3; // Less Y variation
        double zNoise = (random.nextGaussian() - 0.5) * noiseIntensity;

        // Distance-based scaling (more variation when farther)
        double distance = playerPos.distanceTo(target);
        double distanceScale = Math.min(1.0, distance / 10.0);

        xNoise *= distanceScale;
        yNoise *= distanceScale;
        zNoise *= distanceScale;

        // Add slight head bob when moving
        Vec3d headBob = getHeadBobOffset();

        return target.add(xNoise + headBob.x, yNoise + headBob.y, zNoise + headBob.z);
    }

    /**
     * Get head bob offset for natural movement
     */
    private Vec3d getHeadBobOffset() {
        long currentTime = System.currentTimeMillis();
        double timeSinceStart = (currentTime - movementStartTime) / 1000.0;

        // Create subtle head bobbing motion
        headBobPhase = timeSinceStart * 3.0; // Slightly faster than walking

        double bobIntensity = 0.02;
        double yBob = Math.sin(headBobPhase) * bobIntensity;
        double xBob = Math.sin(headBobPhase * 0.5) * bobIntensity * 0.3;

        return new Vec3d(xBob, yBob, 0);
    }

    /**
     * Calculate target yaw and pitch angles
     */
    private void calculateTargetRotation(ClientPlayerEntity player, Vec3d lookTarget) {
        Vec3d playerPos = player.getPos();
        Vec3d direction = lookTarget.subtract(playerPos);

        // Calculate yaw (horizontal rotation)
        targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // Calculate pitch (vertical rotation)
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance));

        // Clamp pitch to valid range
        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f);

        // Normalize yaw to [-180, 180]
        targetYaw = MathHelper.wrapDegrees(targetYaw);
    }

    /**
     * Apply rotation with humanized movement patterns
     */
    private void applyHumanizedRotation(ClientPlayerEntity player, long currentTime) {
        currentYaw = player.getYaw();
        currentPitch = player.getPitch();

        // Calculate angle differences
        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Calculate rotation speed with humanization
        float rotationSpeed = calculateRotationSpeed(Math.abs(yawDiff), Math.abs(pitchDiff));

        // Apply smooth rotation with acceleration curves
        float newYaw = interpolateRotation(currentYaw, targetYaw, yawDiff, rotationSpeed);
        float newPitch = interpolateRotation(currentPitch, targetPitch, pitchDiff, rotationSpeed * 0.8f);

        // Add rotation noise for imperfection
        updateRotationNoise(currentTime);
        newYaw += rotationNoiseYaw;
        newPitch += rotationNoisePitch;

        // Apply constraints
        newPitch = MathHelper.clamp(newPitch, -90f, 90f);
        newYaw = MathHelper.wrapDegrees(newYaw);

        // Set player rotation
        player.setYaw(newYaw);
        player.setPitch(newPitch);
        player.headYaw = newYaw;
        player.bodyYaw = newYaw;
    }

    /**
     * Calculate rotation speed based on angle difference and context
     */
    private float calculateRotationSpeed(float yawDiff, float pitchDiff) {
        float baseSpeed = config.ROTATION_BASE_SPEED;

        // Larger angles = faster rotation (up to a point)
        float totalDiff = yawDiff + pitchDiff;
        float speedMultiplier = 1.0f + (totalDiff / 180.0f) * config.ROTATION_ANGLE_SPEED_MULTIPLIER;
        speedMultiplier = Math.min(speedMultiplier, 3.0f); // Cap maximum speed

        // Add random variation
        float randomFactor = 0.8f + random.nextFloat() * 0.4f; // 0.8 to 1.2

        // Slower rotation when looking ahead (more careful)
        if (isLookingAhead) {
            randomFactor *= 0.7f;
        }

        return baseSpeed * speedMultiplier * randomFactor;
    }

    /**
     * Interpolate rotation with smooth acceleration/deceleration
     */
    private float interpolateRotation(float current, float target, float diff, float speed) {
        float absDiff = Math.abs(diff);

        if (absDiff < 0.1f) {
            return target; // Close enough, snap to target
        }

        // Smooth S-curve for natural acceleration/deceleration
        float progress = 1.0f - (absDiff / 180.0f);
        float smoothingFactor = (float)(1.0 - Math.cos(progress * Math.PI)) / 2.0f;

        // Apply speed with smoothing
        float adjustedSpeed = speed * (0.3f + 0.7f * smoothingFactor);

        // Limit maximum step size
        float maxStep = Math.min(adjustedSpeed, absDiff);

        // Apply rotation step
        if (diff > 0) {
            return current + maxStep;
        } else {
            return current - maxStep;
        }
    }

    /**
     * Update rotation noise for natural imperfection
     */
    private void updateRotationNoise(long currentTime) {
        // Update noise periodically
        if (currentTime - lastNoiseUpdate > config.ROTATION_NOISE_UPDATE_INTERVAL) {
            rotationNoiseYaw = (float) ((random.nextGaussian() - 0.5) * config.ROTATION_NOISE_AMPLITUDE);
            rotationNoisePitch = (float) ((random.nextGaussian() - 0.5) * config.ROTATION_NOISE_AMPLITUDE * 0.5);
            lastNoiseUpdate = currentTime;
        }

        // Add subtle oscillation for natural head movement
        double oscillationTime = currentTime * 0.001; // Slow oscillation
        float oscillationYaw = (float) (Math.sin(oscillationTime) * 0.3);
        float oscillationPitch = (float) (Math.cos(oscillationTime * 0.7) * 0.15);

        rotationNoiseYaw += oscillationYaw;
        rotationNoisePitch += oscillationPitch;
    }

    /**
     * Schedule next rotation update with humanized timing
     */
    private void scheduleNextRotationUpdate() {
        int baseDelay = config.ROTATION_BASE_DELAY_MS;
        int variation = (int) (random.nextGaussian() * config.ROTATION_DELAY_VARIATION_MS);
        int totalDelay = Math.max(config.ROTATION_MIN_DELAY_MS, baseDelay + variation);

        // Faster updates when making large rotations
        float currentAngleDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - currentYaw)) +
                Math.abs(targetPitch - currentPitch);
        if (currentAngleDiff > 45) {
            totalDelay = (int) (totalDelay * 0.6); // 40% faster for large rotations
        }

        nextRotationUpdate = System.currentTimeMillis() + totalDelay;
    }

    /**
     * Instantly snap rotation to target (for emergency situations)
     */
    public void snapToTarget(ClientPlayerEntity player, Vec3d target) {
        if (player == null || target == null) return;

        calculateTargetRotation(player, target);
        player.setYaw(targetYaw);
        player.setPitch(targetPitch);
        player.headYaw = targetYaw;
        player.bodyYaw = targetYaw;
    }

    /**
     * Check if currently looking at target within acceptable accuracy
     */
    public boolean isLookingAtTarget(ClientPlayerEntity player, Vec3d target) {
        if (player == null || target == null) return false;

        calculateTargetRotation(player, target);

        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - player.getYaw()));
        float pitchDiff = Math.abs(targetPitch - player.getPitch());

        return yawDiff < config.ROTATION_ACCURACY_THRESHOLD &&
                pitchDiff < config.ROTATION_ACCURACY_THRESHOLD;
    }

    /**
     * Add idle head movement for natural behavior when not pathfinding
     */
    public void addIdleHeadMovement(ClientPlayerEntity player) {
        if (player == null) return;

        if (random.nextDouble() < config.ROTATION_IDLE_MOVEMENT_CHANCE) {
            float yawNoise = (float) ((random.nextGaussian() - 0.5) * 8.0);
            float pitchNoise = (float) ((random.nextGaussian() - 0.5) * 3.0);

            float newYaw = MathHelper.wrapDegrees(player.getYaw() + yawNoise);
            float newPitch = MathHelper.clamp(player.getPitch() + pitchNoise, -90f, 90f);

            player.setYaw(newYaw);
            player.setPitch(newPitch);
            player.headYaw = newYaw;
        }
    }

    /**
     * Simulate natural "looking around" behavior
     */
    public void simulateLookingAround(ClientPlayerEntity player) {
        if (player == null) return;

        long currentTime = System.currentTimeMillis();

        // Periodically look around
        if (random.nextDouble() < 0.01) { // 1% chance per update
            float randomYaw = player.getYaw() + (random.nextFloat() - 0.5f) * 60f; // ±30 degrees
            float randomPitch = (random.nextFloat() - 0.5f) * 30f; // ±15 degrees

            Vec3d lookAroundTarget = player.getPos().add(
                    Math.sin(Math.toRadians(randomYaw)) * 10,
                    Math.tan(Math.toRadians(randomPitch)) * 10,
                    Math.cos(Math.toRadians(randomYaw)) * 10
            );

            calculateTargetRotation(player, lookAroundTarget);
        }
    }

    /**
     * Get rotation statistics for debugging
     */
    public RotationStats getStats() {
        return new RotationStats(
                targetYaw,
                targetPitch,
                currentYaw,
                currentPitch,
                rotationNoiseYaw,
                rotationNoisePitch,
                isLookingAhead,
                System.currentTimeMillis() - lastRotationTime
        );
    }

    /**
     * Rotation statistics record for debugging
     */
    public record RotationStats(
            float targetYaw,
            float targetPitch,
            float currentYaw,
            float currentPitch,
            float noiseYaw,
            float noisePitch,
            boolean isLookingAhead,
            long timeSinceLastUpdate
    ) {
        public float getYawDifference() {
            return Math.abs(MathHelper.wrapDegrees(targetYaw - currentYaw));
        }

        public float getPitchDifference() {
            return Math.abs(targetPitch - currentPitch);
        }

        public boolean isAccurate(float threshold) {
            return getYawDifference() < threshold && getPitchDifference() < threshold;
        }

        public String getAccuracyString() {
            return String.format("Yaw: %.1f°, Pitch: %.1f°", getYawDifference(), getPitchDifference());
        }
    }
}