package dev.sxmurxy.mre.modules.pathfinding.movement;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.utils.PerlinNoise;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class MovementManager {

    private final PathfinderConfig config;
    private final StuckDetector stuckDetector;
    private final PerlinNoise movementNoise = new PerlinNoise(new Random().nextInt());
    private int bhopTicks = 0;
    private final Random random = new Random();

    public MovementManager(PathfinderConfig config) {
        this.config = config;
        this.stuckDetector = new StuckDetector(config);
    }

    public void updateMovement(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget) {
        resetMovementKeys();

        // Apply Perlin noise for human-like wobble
        float time = (System.currentTimeMillis() % 100000) / 1000f;
        double offsetX = movementNoise.noise(time, 0) * config.MOVEMENT_WOBBLE_INTENSITY;
        double offsetZ = movementNoise.noise(0, time) * config.MOVEMENT_WOBBLE_INTENSITY;
        Vec3d imperfectTarget = new Vec3d(currentTarget.x + offsetX, currentTarget.y, currentTarget.z + offsetZ);

        boolean isPathStraight = isPathStraight(player, imperfectTarget, nextTarget);
        boolean canSprint = player.getHungerManager().getFoodLevel() > 6 && !player.isSneaking();

        if (isPathStraight && canSprint) {
            MinecraftClient.getInstance().options.sprintKey.setPressed(true);
            if (player.isOnGround() && bhopTicks <= 0 && random.nextInt(100) < config.BHOP_CHANCE_PERCENT) {
                player.jump();
                bhopTicks = config.BHOP_COOLDOWN_TICKS;
            }
        }
        if (bhopTicks > 0) bhopTicks--;

        float angleToTarget = getAngleDifference(player, imperfectTarget);
        MinecraftClient.getInstance().options.forwardKey.setPressed(true);

        if (angleToTarget > config.STRAFE_ANGLE_THRESHOLD) {
            MinecraftClient.getInstance().options.leftKey.setPressed(true);
        } else if (angleToTarget < -config.STRAFE_ANGLE_THRESHOLD) {
            MinecraftClient.getInstance().options.rightKey.setPressed(true);
        }

        if (imperfectTarget.y > player.getY() + config.JUMP_HEIGHT_THRESHOLD && player.isOnGround()) {
            MinecraftClient.getInstance().options.jumpKey.setPressed(true);
        }
    }

    private boolean isPathStraight(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget) {
        float angleToCurrent = getAngleDifference(player, currentTarget);
        float angleToNext = getAngleDifference(player, nextTarget);
        return Math.abs(angleToCurrent) < config.SPRINT_MAX_ANGLE_CURRENT && Math.abs(angleToNext) < config.SPRINT_MAX_ANGLE_NEXT;
    }

    private float getAngleDifference(ClientPlayerEntity player, Vec3d target) {
        double dx = target.x - player.getX();
        double dz = target.z - player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        return (float) MathHelper.wrapDegrees(player.getYaw() - targetYaw);
    }

    public static void resetMovementKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false); mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    public boolean isStuck() {
        return stuckDetector.isStuck();
    }

    public int getStuckTicks() {
        return stuckDetector.getStuckTicks();
    }

    public void tryToUnstick(ClientPlayerEntity player) {
        if (player.isOnGround()) player.jump();
    }

    public void resetStuckDetector() {
        stuckDetector.reset();
    }

    private static class StuckDetector {
        private final PathfinderConfig config;
        private final Queue<Vec3d> positionHistory = new LinkedList<>();
        private int stuckTicks = 0;

        public StuckDetector(PathfinderConfig config) { this.config = config; }

        public boolean isStuck() {
            Vec3d currentPos = MinecraftClient.getInstance().player.getPos();
            positionHistory.offer(currentPos);
            if (positionHistory.size() > config.STUCK_HISTORY_SIZE) positionHistory.poll();
            if (positionHistory.size() < config.STUCK_HISTORY_SIZE) return false;

            boolean stuck = positionHistory.peek().distanceTo(currentPos) < config.STUCK_DISTANCE_THRESHOLD;
            if (stuck) stuckTicks++; else stuckTicks = 0;
            return stuck;
        }
        public void reset() { positionHistory.clear(); stuckTicks = 0; }
        public int getStuckTicks() { return stuckTicks; }
    }
}
