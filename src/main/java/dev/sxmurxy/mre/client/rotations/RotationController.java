package dev.sxmurxy.mre.client.rotations;

import dev.sxmurxy.mre.client.pathfinding.Pathfinder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.List;
import java.util.Random;

/**
 * An advanced rotation controller implementing the "Gaze and Glance" algorithm
 * with a fine-tuned PID controller for exceptionally smooth and purposeful camera movement.
 */
public class RotationController {

    private final PlayerEntity player;
    private final Random random = new Random();

    // Fine-tuned PID constants for a "softer," more human feel
    private final double Kp = 0.035; // Proportional
    private final double Ki = 0.004; // Integral
    private final double Kd = 0.015; // Derivative

    private double yawErrorSum = 0;
    private double pitchErrorSum = 0;
    private double lastYawError = 0;
    private double lastPitchError = 0;

    private long lastGlanceTime = 0;
    private boolean isGlancing = false;

    public RotationController(PlayerEntity player) {
        this.player = player;
    }

    public void tick(List<Vec3d> path, int pathIndex, List<Pathfinder.MoveType> moveTypes) {
        if (path == null || path.isEmpty() || pathIndex >= path.size()) return;

        // --- Saccadic Gaze Logic ---
        // Gaze target is now velocity-dependent: look further when moving faster.
        int lookAhead = (int) (10 + player.getVelocity().length() * 5);
        Vec3d gazeTarget = path.get(Math.min(pathIndex + lookAhead, path.size() - 1));
        Vec3d aimTarget = gazeTarget;

        // --- Purposeful Glance Logic ---
        // Trigger a glance only if a JUMP or FALL is imminent.
        boolean complexMoveAhead = false;
        for (int i = pathIndex; i < Math.min(pathIndex + 5, moveTypes.size()); i++) {
            if (moveTypes.get(i) == Pathfinder.MoveType.JUMP || moveTypes.get(i) == Pathfinder.MoveType.FALL) {
                complexMoveAhead = true;
                break;
            }
        }

        if (complexMoveAhead && System.currentTimeMillis() - lastGlanceTime > 1500) {
            isGlancing = true;
            lastGlanceTime = System.currentTimeMillis();
        }

        if (isGlancing && System.currentTimeMillis() - lastGlanceTime < 250) { // Glance for 250ms
            aimTarget = path.get(pathIndex); // Look at immediate node to "check footing"
        } else {
            isGlancing = false;
        }

        applyPIDRotations(aimTarget);
    }

    public void tickPrecise(Vec3d target) {
        applyPIDRotations(target);
    }

    private void applyPIDRotations(Vec3d target) {
        // --- Calculate Target Angles ---
        double deltaX = target.x - player.getX();
        double deltaY = target.y - player.getEyeY();
        double deltaZ = target.z - player.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float targetYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(deltaY, horizontalDistance) * (180.0 / Math.PI)));

        // --- PID Calculation ---
        double yawError = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        yawErrorSum += yawError;
        yawErrorSum = MathHelper.clamp(yawErrorSum, -100, 100); // Prevent integral windup
        double yawDerivative = yawError - lastYawError;
        float yawOutput = (float) (Kp * yawError + Ki * yawErrorSum + Kd * yawDerivative);
        lastYawError = yawError;

        double pitchError = targetPitch - player.getPitch();
        pitchErrorSum += pitchError;
        pitchErrorSum = MathHelper.clamp(pitchErrorSum, -100, 100);
        double pitchDerivative = pitchError - lastPitchError;
        float pitchOutput = (float) (Kp * pitchError + Ki * pitchErrorSum + Kd * pitchDerivative);
        lastPitchError = pitchError;

        // --- Apply New Rotations ---
        player.setYaw(player.getYaw() + yawOutput);
        player.setPitch(MathHelper.clamp(player.getPitch() + pitchOutput, -90.0F, 90.0F));
    }

    public void stop() {
        // Reset PID state
        this.yawErrorSum = 0;
        this.pitchErrorSum = 0;
        this.lastYawError = 0;
        this.lastPitchError = 0;
    }
}

