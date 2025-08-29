package dev.sxmurxy.mre.client.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Random;

/**
 * Advanced humanized rotation system with context-aware speed algorithms.
 * Implements mathematical models for natural human-like camera movement.
 *
 * Key Algorithm: Bigger rotations = faster, smaller rotations = smooth and precise
 */
public class RotationController {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    // Rotation speed constants (degrees per tick) - tuned for different contexts
    private static final float GENERAL_ROTATION_SPEED_MIN = 2.0f;
    private static final float GENERAL_ROTATION_SPEED_MAX = 15.0f;
    private static final float MOVEMENT_ROTATION_SPEED_MIN = 4.0f;
    private static final float MOVEMENT_ROTATION_SPEED_MAX = 20.0f;
    private static final float ETHERWARP_ROTATION_SPEED_MIN = 1.0f;
    private static final float ETHERWARP_ROTATION_SPEED_MAX = 8.0f;

    // Humanization parameters
    private static final float SMOOTHING_FACTOR = 0.85f;
    private static final float OVERSHOOT_FACTOR = 0.15f;
    private static final float MICRO_CORRECTION_THRESHOLD = 2.0f;

    // Current rotation state
    private static float currentYawVelocity = 0.0f;
    private static float currentPitchVelocity = 0.0f;
    private static long lastRotationTime = 0;
    private static RotationType lastRotationType = RotationType.GENERAL;

    public enum RotationType {
        GENERAL,      // Smooth, natural rotations
        MOVEMENT,     // Faster rotations for pathfinding
        ETHERWARP,    // Precise, slower rotations for etherwarp
        AOTV         // Quick rotations for AOTV
    }

    /**
     * Main rotation method that handles all rotation types with humanized algorithms.
     */
    public static void rotate(Vec3d target, RotationType type, boolean instant) {
        if (mc.player == null) return;

        float[] targetRotation = calculateTargetRotation(target);
        float targetYaw = targetRotation[0];
        float targetPitch = targetRotation[1];

        if (instant) {
            setRotation(targetYaw, targetPitch);
            return;
        }

        RotationStep step = calculateHumanizedRotation(targetYaw, targetPitch, type);
        applyRotationStep(step);
    }

    /**
     * Advanced algorithm for calculating humanized rotation with context-aware speeds.
     * Uses sigmoid function for speed curve: larger angles = faster, smaller angles = smoother.
     *
     * Mathematical model: speed = base + (max-base) * sigmoid(angle/threshold) * randomFactor
     */
    private static RotationStep calculateHumanizedRotation(float targetYaw, float targetPitch, RotationType type) {
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = MathHelper.wrapDegrees(targetPitch - currentPitch);

        // Calculate total angular distance
        float totalAngle = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        // Dynamic speed calculation using sigmoid function
        float baseSpeed = calculateDynamicSpeed(totalAngle, type);

        // Apply humanization factors
        HumanizationFactors factors = calculateHumanizationFactors(totalAngle, type);

        // Calculate final rotation deltas with exponential approach for smoothness
        float yawDelta = calculateAxisDelta(yawDiff, baseSpeed, factors.yawMultiplier);
        float pitchDelta = calculateAxisDelta(pitchDiff, baseSpeed, factors.pitchMultiplier);

        return new RotationStep(yawDelta, pitchDelta, factors);
    }

    /**
     * Dynamic speed calculation using sigmoid curve.
     * Large angles get exponentially faster speed, small angles get smooth precision.
     */
    private static float calculateDynamicSpeed(float angle, RotationType type) {
        float minSpeed, maxSpeed, threshold;

        switch (type) {
            case MOVEMENT -> {
                minSpeed = MOVEMENT_ROTATION_SPEED_MIN;
                maxSpeed = MOVEMENT_ROTATION_SPEED_MAX;
                threshold = 45.0f; // Faster ramp-up for movement
            }
            case ETHERWARP -> {
                minSpeed = ETHERWARP_ROTATION_SPEED_MIN;
                maxSpeed = ETHERWARP_ROTATION_SPEED_MAX;
                threshold = 30.0f; // More precise control
            }
            case AOTV -> {
                minSpeed = GENERAL_ROTATION_SPEED_MIN;
                maxSpeed = GENERAL_ROTATION_SPEED_MAX;
                threshold = 60.0f; // Quick but controlled
            }
            default -> {
                minSpeed = GENERAL_ROTATION_SPEED_MIN;
                maxSpeed = GENERAL_ROTATION_SPEED_MAX;
                threshold = 90.0f; // Balanced curve
            }
        }

        // Sigmoid function: 1 / (1 + e^(-x + offset))
        // This creates smooth acceleration for larger angles
        float sigmoidInput = (angle - threshold/2) / (threshold/4);
        float sigmoidOutput = (float) (1.0 / (1.0 + Math.exp(-sigmoidInput)));

        // Add human variation (±20%)
        float randomFactor = 0.8f + (random.nextFloat() * 0.4f);

        return minSpeed + (maxSpeed - minSpeed) * sigmoidOutput * randomFactor;
    }

    /**
     * Calculate axis-specific rotation delta with exponential decay for smooth approach.
     */
    private static float calculateAxisDelta(float angleDiff, float baseSpeed, float multiplier) {
        if (Math.abs(angleDiff) < 0.1f) return 0.0f;

        float absAngle = Math.abs(angleDiff);
        float direction = Math.signum(angleDiff);

        // Exponential decay for smoother approach to target
        float decayFactor = (float) Math.exp(-absAngle / 180.0f);
        float smoothingMultiplier = 1.0f - (decayFactor * SMOOTHING_FACTOR);

        float delta = baseSpeed * multiplier * smoothingMultiplier;

        // Micro-corrections for small angles (human precision simulation)
        if (absAngle < MICRO_CORRECTION_THRESHOLD) {
            delta *= 0.2f + (random.nextFloat() * 0.3f); // 20-50% of normal speed
        }

        // Prevent overshoot but allow slight human error
        if (delta > absAngle) {
            float overshoot = 1.0f + (random.nextFloat() * OVERSHOOT_FACTOR - OVERSHOOT_FACTOR/2);
            delta = absAngle * overshoot;
        }

        return direction * Math.max(0.05f, delta);
    }

    /**
     * Calculate humanization factors based on rotation context and momentum.
     */
    private static HumanizationFactors calculateHumanizationFactors(float angle, RotationType type) {
        long currentTime = System.currentTimeMillis();
        float timeDelta = (currentTime - lastRotationTime) / 50.0f; // Convert to ticks

        // Base multipliers (humans are naturally slower at pitch)
        float yawMultiplier = 1.0f;
        float pitchMultiplier = 0.75f; // Vertical adjustment is typically slower

        // Context-specific adjustments
        switch (type) {
            case ETHERWARP -> {
                yawMultiplier *= 0.6f;  // Very precise for etherwarp
                pitchMultiplier *= 0.5f;
            }
            case MOVEMENT -> {
                yawMultiplier *= 1.3f;  // Faster for movement
                pitchMultiplier *= 1.2f;
            }
            case AOTV -> {
                yawMultiplier *= 1.15f; // Slightly faster for AOTV
                pitchMultiplier *= 1.0f;
            }
        }

        // Momentum-based adjustment (continuation of previous rotation type)
        if (lastRotationType == type && timeDelta < 3.0f) {
            float momentum = Math.min(1.5f, 2.0f - timeDelta / 2.0f);
            yawMultiplier *= momentum;
            pitchMultiplier *= momentum;
        }

        // Human inconsistency (±20% variation)
        yawMultiplier *= 0.8f + (random.nextFloat() * 0.4f);
        pitchMultiplier *= 0.8f + (random.nextFloat() * 0.4f);

        lastRotationTime = currentTime;
        lastRotationType = type;

        return new HumanizationFactors(yawMultiplier, pitchMultiplier, angle > 30.0f);
    }

    /**
     * Apply rotation step with velocity smoothing and natural deceleration.
     */
    private static void applyRotationStep(RotationStep step) {
        // Exponential moving average for velocity smoothing
        float smoothingFactor = step.factors.isLargeRotation ? 0.6f : 0.8f;

        currentYawVelocity = currentYawVelocity * smoothingFactor + step.yawDelta * (1 - smoothingFactor);
        currentPitchVelocity = currentPitchVelocity * smoothingFactor + step.pitchDelta * (1 - smoothingFactor);

        // Natural deceleration for large rotations
        if (step.factors.isLargeRotation) {
            currentYawVelocity *= 0.92f;
            currentPitchVelocity *= 0.92f;
        }

        // Apply final rotation
        float newYaw = mc.player.getYaw() + currentYawVelocity;
        float newPitch = MathHelper.clamp(mc.player.getPitch() + currentPitchVelocity, -90.0f, 90.0f);

        setRotation(newYaw, newPitch);
    }

    /**
     * Calculate target rotation angles to look at a specific position.
     */
    private static float[] calculateTargetRotation(Vec3d target) {
        if (mc.player == null) return new float[]{0, 0};

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = target.subtract(eyePos);

        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-Math.atan2(direction.y, horizontalDistance) * 180.0 / Math.PI);

        return new float[]{yaw, pitch};
    }

    /**
     * Set player rotation with proper normalization.
     */
    private static void setRotation(float yaw, float pitch) {
        if (mc.player == null) return;

        mc.player.setYaw(MathHelper.wrapDegrees(yaw));
        mc.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    /**
     * Check if rotation is close enough to target (within threshold).
     */
    public static boolean isRotationComplete(Vec3d target, float threshold) {
        if (mc.player == null) return false;

        float[] targetRotation = calculateTargetRotation(target);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetRotation[0] - mc.player.getYaw()));
        float pitchDiff = Math.abs(MathHelper.wrapDegrees(targetRotation[1] - mc.player.getPitch()));

        return yawDiff < threshold && pitchDiff < threshold;
    }

    /**
     * Calculate rotation distance to target in degrees.
     */
    public static float getRotationDistance(Vec3d target) {
        if (mc.player == null) return 0;

        float[] targetRotation = calculateTargetRotation(target);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetRotation[0] - mc.player.getYaw()));
        float pitchDiff = Math.abs(MathHelper.wrapDegrees(targetRotation[1] - mc.player.getPitch()));

        return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    /**
     * Reset rotation velocities for clean state.
     */
    public static void resetVelocities() {
        currentYawVelocity = 0.0f;
        currentPitchVelocity = 0.0f;
    }

    /**
     * Get current rotation velocity for debugging.
     */
    public static Vec3d getCurrentVelocity() {
        return new Vec3d(currentYawVelocity, currentPitchVelocity, 0);
    }

    // Helper classes
    private record RotationStep(float yawDelta, float pitchDelta, HumanizationFactors factors) {}

    private record HumanizationFactors(float yawMultiplier, float pitchMultiplier, boolean isLargeRotation) {}
}