package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class RotationController {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private float targetYaw = 0;
    private float targetPitch = 0;
    private float currentYaw = 0;
    private float currentPitch = 0;

    // Humanization parameters
    private static final float MAX_ROTATION_SPEED = 12.0f; // degrees per tick
    private static final float MIN_ROTATION_SPEED = 3.0f;
    private final Random random = ThreadLocalRandom.current();

    public void updateRotation(Vec3d direction) {
        if (mc.player == null) return;

        // Calculate target angles
        float newTargetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float newTargetPitch = (float) Math.toDegrees(-Math.asin(direction.y));

        // Normalize angles
        newTargetYaw = normalizeAngle(newTargetYaw);
        newTargetPitch = Math.max(-90, Math.min(90, newTargetPitch));

        // Update targets
        targetYaw = newTargetYaw;
        targetPitch = newTargetPitch;

        // Get current angles
        currentYaw = normalizeAngle(mc.player.getYaw());
        currentPitch = mc.player.getPitch();

        // Calculate smooth rotation
        applyRotationStep();
    }

    public void setPitch(float pitch) {
        targetPitch = Math.max(-90, Math.min(90, pitch));
    }

    private void applyRotationStep() {
        // Calculate angle differences
        float yawDiff = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Calculate rotation speeds (humanized)
        float yawSpeed = getHumanizedRotationSpeed(Math.abs(yawDiff));
        float pitchSpeed = getHumanizedRotationSpeed(Math.abs(pitchDiff)) * 0.8f; // Pitch is usually slower

        // Apply rotation step
        if (Math.abs(yawDiff) > 0.5f) {
            float yawStep = Math.min(yawSpeed, Math.abs(yawDiff)) * Math.signum(yawDiff);
            currentYaw = normalizeAngle(currentYaw + yawStep);
            mc.player.setYaw(currentYaw);
        }

        if (Math.abs(pitchDiff) > 0.5f) {
            float pitchStep = Math.min(pitchSpeed, Math.abs(pitchDiff)) * Math.signum(pitchDiff);
            currentPitch = Math.max(-90, Math.min(90, currentPitch + pitchStep));
            mc.player.setPitch(currentPitch);
        }
    }

    private float getHumanizedRotationSpeed(float angleDifference) {
        // Base speed varies with angle difference
        float baseSpeed = MIN_ROTATION_SPEED + (MAX_ROTATION_SPEED - MIN_ROTATION_SPEED) *
                Math.min(1.0f, angleDifference / 90.0f);

        // Add random variation
        float variation = 1.0f + (float)(random.nextGaussian() * 0.2f);
        variation = Math.max(0.5f, Math.min(1.5f, variation));

        return baseSpeed * variation;
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360.0f;
        if (angle > 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
}