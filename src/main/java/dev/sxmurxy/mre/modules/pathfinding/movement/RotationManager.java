package dev.sxmurxy.mre.modules.pathfinding.movement;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class RotationManager {

    private final PathfinderConfig config;
    private final Random random = new Random();
    private Vec3d lookTarget;
    private int saccadeTicks = 0;

    public RotationManager(PathfinderConfig config) {
        this.config = config;
    }

    public void updateRotation(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget, BlockPos finalDestination) {
        if (saccadeTicks <= 0) {
            if (random.nextInt(100) > config.SACCADE_CHANCE_PERCENT) {
                lookTarget = currentTarget.add(nextTarget).multiply(0.5); // Predictive aiming
            } else {
                lookTarget = finalDestination.toCenterPos(); // Saccadic glance
                saccadeTicks = config.SACCADE_DURATION_TICKS;
            }
        } else {
            saccadeTicks--;
        }

        double targetYaw = Math.toDegrees(Math.atan2(-(lookTarget.x - player.getX()), lookTarget.z - player.getZ()));
        Vec3d eyePos = player.getEyePos();
        double dy = lookTarget.y - eyePos.y;
        double horizontalDist = Math.sqrt(Math.pow(lookTarget.x - eyePos.x, 2) + Math.pow(lookTarget.z - eyePos.z, 2));
        double targetPitch = Math.toDegrees(-Math.atan2(dy, horizontalDist));

        targetPitch = MathHelper.clamp(targetPitch, config.MIN_PITCH, config.MAX_PITCH);
        targetYaw += (random.nextDouble() - 0.5) * config.ROTATION_JITTER_YAW;
        targetPitch += (random.nextDouble() - 0.5) * config.ROTATION_JITTER_PITCH;

        float newYaw = (float) MathHelper.lerpAngleDegrees(config.ROTATION_SMOOTH_FACTOR, player.getYaw(), (float) targetYaw);
        float newPitch = (float) MathHelper.lerp(config.ROTATION_SMOOTH_FACTOR, player.getPitch(), (float) targetPitch);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
    }
}
