package dev.sxmurxy.mre.client.pathfinding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * An advanced, predictive movement controller that implements the "Predictive
 * Flow" algorithm, analyzing path curvature to dynamically control speed.
 */
public class MovementController {

    private final MinecraftClient mc;
    private final PlayerEntity player;
    private final KeyBinding forwardKey, leftKey, rightKey, jumpKey, sprintKey, sneakKey;
    private int brakingTicks = 0;

    public MovementController(PlayerEntity player) {
        this.mc = MinecraftClient.getInstance();
        this.player = player;
        this.forwardKey = mc.options.forwardKey;
        this.leftKey = mc.options.leftKey;
        this.rightKey = mc.options.rightKey;
        this.jumpKey = mc.options.jumpKey;
        this.sprintKey = mc.options.sprintKey;
        this.sneakKey = mc.options.sneakKey;
    }

    public void tick(List<Vec3d> path, int pathIndex, List<Pathfinder.MoveType> moveTypes) {
        if (path == null || path.isEmpty() || pathIndex >= path.size()) { stop(); return; }

        Vec3d currentTarget = path.get(pathIndex);
        Pathfinder.MoveType currentMove = moveTypes.get(pathIndex);

        float targetYaw = (float) (MathHelper.atan2(currentTarget.z - player.getZ(), currentTarget.x - player.getX()) * (180.0D / Math.PI)) - 90.0F;
        float yawDifference = MathHelper.wrapDegrees(player.getYaw() - targetYaw);

        // --- Anticipatory Braking ---
        double curvature = calculatePathCurvature(path, pathIndex);
        if (curvature > 0.4 && player.getVelocity().length() > 0.15) {
            brakingTicks = 3; // Release W for 3 ticks to slow down for the turn
        }

        forwardKey.setPressed(brakingTicks <= 0);
        if (brakingTicks > 0) brakingTicks--;

        // --- Velocity-Relative Strafing ---
        if (Math.abs(yawDifference) > 5) {
            // Strafe less aggressively at high speeds to prevent overshooting
            float strafeFactor = 1.0f - MathHelper.clamp((float)player.getVelocity().length() / 0.3f, 0.0f, 0.7f);
            if (yawDifference > 0) { leftKey.setPressed(true); rightKey.setPressed(false); }
            else { rightKey.setPressed(true); leftKey.setPressed(false); }
        } else {
            leftKey.setPressed(false); rightKey.setPressed(false);
        }

        sprintKey.setPressed(forwardKey.isPressed() && player.getHungerManager().getFoodLevel() > 6);
        jumpKey.setPressed(currentMove == Pathfinder.MoveType.JUMP && player.isOnGround());
    }

    private double calculatePathCurvature(List<Vec3d> path, int currentIndex) {
        if (currentIndex + 10 >= path.size()) return 0;
        Vec3d p1 = path.get(currentIndex); Vec3d p2 = path.get(currentIndex + 5); Vec3d p3 = path.get(currentIndex + 10);
        Vec3d v1 = p2.subtract(p1).normalize(); Vec3d v2 = p3.subtract(p2).normalize();
        return 1.0 - v1.dotProduct(v2); // 0 = straight, >0 = curve
    }

    public void setSneaking(boolean sneaking) {
        sneakKey.setPressed(sneaking);
    }

    public void stop() {
        forwardKey.setPressed(false); leftKey.setPressed(false); rightKey.setPressed(false);
        jumpKey.setPressed(false); sprintKey.setPressed(false); sneakKey.setPressed(false);
        brakingTicks = 0;
    }
}

