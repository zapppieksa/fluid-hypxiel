package dev.sxmurxy.mre.modules.pathfinding.movement;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Handles humanized rotation with realistic looking patterns and imperfections
 * Uses only PathfinderConfig constants for all settings (no GUI dependency)
 *
 * Features:
 * - Smooth rotation with acceleration/deceleration using config
 * - Look-ahead behavior for natural pathfinding
 * - Random noise and imperfections from config constants
 * - Natural head movement patterns
 * - Realistic rotation speeds and timing from config
 * - Occasional glances at final destination
 * - Idle head movements for realism
 */
public class RotationManager {

    private final PathfinderConfig config;
    private final Random random = new Random();

    // Current rotation state
    private float targetYaw = 0f;
    private float targetPitch = 0f;
    private float currentYaw = 0f;
    private float currentPitch = 0f;

    // Rotation timing (using config constants)
    private long lastRotationTime = 0;
    private long nextRotationUpdate = 0;

    // Humanization state (using config constants)
    private float rotationNoiseYaw = 0f;
    private float rotationNoisePitch = 0f;
    private long lastNoiseUpdate = 0;

    // Look behavior (using config constants)
    private Vec3d lastLookTarget = Vec3d.ZERO;
    private long lookAheadStartTime = 0;
    private boolean isLookingAhead = false;
    private long finalDestinationLookStart = 0;
    private boolean lookingAtFinalDestination = false;

    // Natural movement patterns
    private double headBobPhase = 0.0;
    private long movementStartTime = 0;
    private boolean wasMoving = false;

    // Rotation smoothing
    private float yawVelocity = 0f;
    private float pitchVelocity = 0f;
    private boolean isAcceleratingYaw = false;
    private boolean isAcceleratingPitch = false;

    public RotationManager(PathfinderConfig config) {
        this.config = config;
        this.movementStartTime = System.currentTimeMillis();
    }

    /**
     * Update player rotation with humanized movement patterns using config
     */
    public void updateRotation(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget, BlockPos finalDestination) {
        if (player == null) return;

        long currentTime = System.currentTimeMillis();

        // Check if we should update rotation (humanized timing using config)
        if (currentTime < nextRotationUpdate) {
            return;
        }

        // Calculate where to look
        Vec3d lookTarget = calculateLookTarget(player, currentTarget, nextTarget, finalDestination, currentTime);

        // Update target rotation
        calculateTargetRotation(player, lookTarget);

        // Apply rotation with humanization
        applyHumanizedRotation(player, currentTime);

        // Schedule next update using config constants
        scheduleNextRotationUpdate();

        // Update state
        lastRotationTime = currentTime;
        lastLookTarget = lookTarget;
    }

    /**
     * Calculate optimal look target based on path and context
     */
    private Vec3d calculateLookTarget(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget, BlockPos finalDestination, long currentTime) {
        Vec3d playerPos = player.getPos();
        Vec3d baseLookTarget = currentTarget;

        // 1. Look-ahead behavior using config distance
        if (nextTarget != null && !nextTarget.equals(currentTarget)) {
            double distanceToCurrentTarget = playerPos.distanceTo(currentTarget);

            if (distanceToCurrentTarget < PathfinderConfig.ROTATION_LOOK_AHEAD_DISTANCE) {
                isLookingAhead = true;
                if (lookAheadStartTime == 0) {
                    lookAheadStartTime = currentTime;
                }
                baseLookTarget = nextTarget;
            } else {
                isLookingAhead = false;
                lookAheadStartTime = 0;
            }
        }

        // 2. Occasional glances at final destination using config chance
        if (finalDestination != null &&
                random.nextDouble() < PathfinderConfig.ROTATION_FINAL_DESTINATION_LOOK_CHANCE) {

            if (!lookingAtFinalDestination) {
                lookingAtFinalDestination = true;
                finalDestinationLookStart = currentTime;
            }
        }

        // Handle final destination looking duration
        if (lookingAtFinalDestination) {
            if (currentTime - finalDestinationLookStart < PathfinderConfig.ROTATION_FINAL_DESTINATION_LOOK_DURATION) {
                baseLookTarget = Vec3d.ofCenter(finalDestination);
            } else {
                lookingAtFinalDestination = false;
                finalDestinationLookStart = 0;
            }
        }

        // 3. Add randomization using config constants
        baseLookTarget = addLookTargetRandomization(baseLookTarget, currentTime);

        // 4. Apply natural head bobbing
        baseLookTarget = addNaturalHeadMovement(baseLookTarget, playerPos, currentTime);

        return baseLookTarget;
    }

    /**
     * Add look target randomization using config constants
     */
    private Vec3d addLookTargetRandomization(Vec3d target, long currentTime) {
        double intensity = PathfinderConfig.ROTATION_NOISE_INTENSITY;

        // Update noise periodically
        if (currentTime - lastNoiseUpdate > PathfinderConfig.ROTATION_NOISE_UPDATE_INTERVAL) {
            rotationNoiseYaw = (float)(random.nextGaussian() * intensity);
            rotationNoisePitch = (float)(random.nextGaussian() * intensity * 0.3); // Less vertical noise
            lastNoiseUpdate = currentTime;
        }

        return target.add(rotationNoiseYaw, rotationNoisePitch * 0.5, rotationNoiseYaw * 0.5);
    }

    /**
     * Add natural head movement and bobbing
     */
    private Vec3d addNaturalHeadMovement(Vec3d target, Vec3d playerPos, long currentTime) {
        boolean isMoving = playerPos.distanceTo(lastLookTarget) > 0.1;

        if (isMoving != wasMoving) {
            // Movement state changed, adjust head bob phase
            headBobPhase = 0.0;
        }

        if (isMoving) {
            // Add subtle head bobbing while moving
            double timeFactor = currentTime * 0.002; // Slower than movement wiggle
            double bobIntensity = 0.05;

            double bobX = Math.sin(timeFactor) * bobIntensity;
            double bobY = Math.cos(timeFactor * 1.5) * bobIntensity * 0.3;

            target = target.add(bobX, bobY, bobX * 0.5);
        } else {
            // Idle head movements using config chance
            if (random.nextDouble() < PathfinderConfig.ROTATION_IDLE_MOVEMENT_CHANCE) {
                double idleX = (random.nextGaussian() * 0.3);
                double idleY = (random.nextGaussian() * 0.15);
                target = target.add(idleX, idleY, idleX * 0.3);
            }
        }

        wasMoving = isMoving;
        return target;
    }

    /**
     * Calculate target rotation from look position
     */
    private void calculateTargetRotation(ClientPlayerEntity player, Vec3d lookTarget) {
        Vec3d playerPos = player.getEyePos();
        Vec3d direction = lookTarget.subtract(playerPos).normalize();

        // Calculate yaw (horizontal rotation)
        targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // Calculate pitch (vertical rotation)
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        targetPitch = (float) Math.toDegrees(Math.atan2(-direction.y, horizontalDistance));

        // Clamp pitch to valid range
        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f);
    }

    /**
     * Apply humanized rotation with acceleration and noise
     */
    private void applyHumanizedRotation(ClientPlayerEntity player, long currentTime) {
        float playerYaw = player.getYaw();
        float playerPitch = player.getPitch();

        // Calculate angle differences
        float yawDiff = MathHelper.wrapDegrees(targetYaw - playerYaw);
        float pitchDiff = targetPitch - playerPitch;

        // Calculate rotation speeds based on config and angle size
        float yawSpeed = calculateRotationSpeed(Math.abs(yawDiff), true);
        float pitchSpeed = calculateRotationSpeed(Math.abs(pitchDiff), false);

        // Apply acceleration/deceleration
        yawSpeed = applyRotationAcceleration(yawSpeed, yawDiff, true);
        pitchSpeed = applyRotationAcceleration(pitchSpeed, pitchDiff, false);

        // Add noise using config constants
        yawSpeed = addRotationNoise(yawSpeed, currentTime, true);
        pitchSpeed = addRotationNoise(pitchSpeed, currentTime, false);

        // Calculate new rotation values
        float newYaw = playerYaw;
        float newPitch = playerPitch;

        // Apply yaw rotation
        if (Math.abs(yawDiff) > PathfinderConfig.ROTATION_ACCURACY_THRESHOLD) {
            float yawStep = Math.signum(yawDiff) * yawSpeed;
            if (Math.abs(yawStep) > Math.abs(yawDiff)) {
                yawStep = yawDiff; // Don't overshoot
            }
            newYaw += yawStep;
        }

        // Apply pitch rotation
        if (Math.abs(pitchDiff) > PathfinderConfig.ROTATION_ACCURACY_THRESHOLD) {
            float pitchStep = Math.signum(pitchDiff) * pitchSpeed;
            if (Math.abs(pitchStep) > Math.abs(pitchDiff)) {
                pitchStep = pitchDiff; // Don't overshoot
            }
            newPitch += pitchStep;
        }

        // Set new rotation
        player.setYaw(newYaw);
        player.setPitch(MathHelper.clamp(newPitch, -90f, 90f));
    }

    /**
     * Calculate rotation speed based on config and angle size
     */
    private float calculateRotationSpeed(float angleDiff, boolean isYaw) {
        float baseSpeed = PathfinderConfig.ROTATION_BASE_SPEED;

        // Larger angles rotate faster (up to a point)
        if (angleDiff > 30f) {
            baseSpeed *= PathfinderConfig.ROTATION_ANGLE_SPEED_MULTIPLIER;
        }

        // Pitch rotations are typically slower
        if (!isYaw) {
            baseSpeed *= 0.7f;
        }

        return baseSpeed;
    }

    /**
     * Apply acceleration/deceleration to rotation
     */
    private float applyRotationAcceleration(float speed, float angleDiff, boolean isYaw) {
        float currentVelocity = isYaw ? yawVelocity : pitchVelocity;
        boolean isAccelerating = isYaw ? isAcceleratingYaw : isAcceleratingPitch;

        // Acceleration when starting or large angle
        if (Math.abs(angleDiff) > 45f || currentVelocity < speed * 0.5f) {
            isAccelerating = true;
            currentVelocity = Math.min(currentVelocity + speed * 0.3f, speed);
        } else if (Math.abs(angleDiff) < 10f) {
            // Deceleration when close to target
            isAccelerating = false;
            currentVelocity = Math.max(currentVelocity * 0.8f, speed * 0.2f);
        }

        // Update state
        if (isYaw) {
            yawVelocity = currentVelocity;
            isAcceleratingYaw = isAccelerating;
        } else {
            pitchVelocity = currentVelocity;
            isAcceleratingPitch = isAccelerating;
        }

        return currentVelocity;
    }

    /**
     * Add rotation noise using config constants
     */
    private float addRotationNoise(float speed, long currentTime, boolean isYaw) {
        // Update noise periodically
        if (currentTime - lastNoiseUpdate > PathfinderConfig.ROTATION_NOISE_UPDATE_INTERVAL / 2) {
            float amplitude = PathfinderConfig.ROTATION_NOISE_AMPLITUDE;
            float noise = (float)(random.nextGaussian() * amplitude * 0.1f);
            speed += noise;
        }

        return Math.max(0.1f, speed); // Minimum speed
    }

    /**
     * Schedule next rotation update using config constants
     */
    private void scheduleNextRotationUpdate() {
        int baseDelay = PathfinderConfig.ROTATION_BASE_DELAY_MS;
        int variation = PathfinderConfig.ROTATION_DELAY_VARIATION_MS;
        int minDelay = PathfinderConfig.ROTATION_MIN_DELAY_MS;

        int delay = baseDelay + (int)(random.nextGaussian() * variation);
        delay = Math.max(minDelay, delay);

        nextRotationUpdate = System.currentTimeMillis() + delay;
    }

    /**
     * Snap rotation directly to target (for non-humanized movement)
     */
    public void snapToTarget(ClientPlayerEntity player, Vec3d target) {
        if (player == null) return;

        Vec3d playerPos = player.getEyePos();
        Vec3d direction = target.subtract(playerPos).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float pitch = (float) Math.toDegrees(Math.atan2(-direction.y, horizontalDistance));

        player.setYaw(yaw);
        player.setPitch(MathHelper.clamp(pitch, -90f, 90f));
    }

    /**
     * Get current rotation state for debugging
     */
    public String getRotationState() {
        return String.format(
                "Rotation State: target=(%.1f,%.1f), looking=%s, finalDest=%s, noise=(%.2f,%.2f)",
                targetYaw, targetPitch, isLookingAhead, lookingAtFinalDestination,
                rotationNoiseYaw, rotationNoisePitch
        );
    }

    /**
     * Check if currently performing look-ahead behavior
     */
    public boolean isLookingAhead() {
        return isLookingAhead;
    }

    /**
     * Check if currently looking at final destination
     */
    public boolean isLookingAtFinalDestination() {
        return lookingAtFinalDestination;
    }

    /**
     * Get rotation statistics
     */
    public String getStatistics() {
        long uptime = System.currentTimeMillis() - movementStartTime;
        return String.format(
                "Rotation Statistics:\n" +
                        "  Uptime: %d seconds\n" +
                        "  Look-ahead active: %s\n" +
                        "  Final destination glances: %s\n" +
                        "  Current velocities: yaw=%.2f, pitch=%.2f\n" +
                        "  Noise levels: yaw=%.2f, pitch=%.2f",
                uptime / 1000, isLookingAhead, lookingAtFinalDestination,
                yawVelocity, pitchVelocity, rotationNoiseYaw, rotationNoisePitch
        );
    }

    /**
     * Reset rotation state
     */
    public void reset() {
        yawVelocity = 0f;
        pitchVelocity = 0f;
        isAcceleratingYaw = false;
        isAcceleratingPitch = false;
        rotationNoiseYaw = 0f;
        rotationNoisePitch = 0f;
        isLookingAhead = false;
        lookingAtFinalDestination = false;
        lookAheadStartTime = 0;
        finalDestinationLookStart = 0;
        lastNoiseUpdate = 0;
    }

    /**
     * Emergency rotation reset
     */
    public void emergencyReset() {
        reset();
        nextRotationUpdate = 0; // Allow immediate update
        lastRotationTime = 0;
    }
}