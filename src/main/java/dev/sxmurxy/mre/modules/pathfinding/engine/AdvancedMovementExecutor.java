package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AdvancedMovementExecutor {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final MovementHumanizer humanizer = new MovementHumanizer();
    private final RotationController rotationController = new RotationController();
    private final MovementPredictor predictor = new MovementPredictor();

    // Movement state
    private Vec3d lastTargetPos = null;
    private long movementStartTime = 0;
    private int pauseTicksRemaining = 0;
    private PathNode.MovementType currentMovementType = PathNode.MovementType.WALK;

    public void executeMovement(PathNode currentNode, PathNode nextNode) {
        if (mc.player == null || mc.world == null) return;

        // Handle pause if needed
        if (pauseTicksRemaining > 0) {
            pauseTicksRemaining--;
            stopAllMovement();
            return;
        }

        // Check for random pauses
        if (humanizer.shouldPauseMovement()) {
            pauseTicksRemaining = humanizer.getRandomPauseDuration();
            return;
        }

        // Get target position with humanization
        Vec3d targetPos = getHumanizedTarget(currentNode, nextNode);

        // Execute specific movement type
        switch (currentNode.movementType) {
            case WALK:
            case SPRINT:
                executeGroundMovement(targetPos, currentNode.requiresSprint);
                break;
            case JUMP:
            case PARKOUR:
                executeJumpMovement(targetPos, currentNode);
                break;
            case CLIMB_LADDER:
            case CLIMB_VINE:
                executeClimbMovement(targetPos);
                break;
            case SWIM:
                executeSwimMovement(targetPos);
                break;
            case FALL:
                executeFallMovement(targetPos);
                break;
            default:
                executeGroundMovement(targetPos, false);
        }

        lastTargetPos = targetPos;
        currentMovementType = currentNode.movementType;
    }

    private Vec3d getHumanizedTarget(PathNode currentNode, PathNode nextNode) {
        Vec3d baseTarget = currentNode.exactPosition;

        // Apply humanization
        Vec3d playerPos = mc.player.getPos();
        Vec3d playerVelocity = mc.player.getVelocity();

        return humanizer.getHumanizedTarget(baseTarget, playerPos, playerVelocity);
    }

    private void executeGroundMovement(Vec3d targetPos, boolean shouldSprint) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d direction = targetPos.subtract(playerPos).normalize();

        // Apply humanized rotation
        Vec3d humanizedDirection = humanizer.getImperfectRotation(direction);
        rotationController.updateRotation(humanizedDirection);

        // Calculate movement inputs
        double speed = humanizer.getHumanizedSpeed();

        // Set movement keys
        setMovementInputs(humanizedDirection, speed, shouldSprint);
    }

    private void executeJumpMovement(Vec3d targetPos, PathNode node) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d direction = targetPos.subtract(playerPos).normalize();

        // More precise rotation for jumping
        Vec3d jumpDirection = humanizer.getImperfectRotation(direction);
        rotationController.updateRotation(jumpDirection);

        // Calculate jump timing and sprint requirement
        double horizontalDistance = Math.sqrt(Math.pow(targetPos.x - playerPos.x, 2) +
                Math.pow(targetPos.z - playerPos.z, 2));

        boolean needsSprint = horizontalDistance > 2.0;

        // Execute jump movement
        setMovementInputs(jumpDirection, 1.0, needsSprint);

        // Jump timing
        if (mc.player.isOnGround() && shouldJumpNow(playerPos, targetPos)) {
            mc.player.jump();
        }
    }

    private void executeClimbMovement(Vec3d targetPos) {
        Vec3d playerPos = mc.player.getPos();

        // Look towards climb direction
        Vec3d climbDirection = targetPos.subtract(playerPos).normalize();
        rotationController.updateRotation(climbDirection);

        // Climb up by holding W + looking up slightly
        mc.options.forwardKey.setPressed(true);

        // Set pitch for climbing
        float climbPitch = -30.0f + (float) (humanizer.getImperfectRotation(Vec3d.of(Direction.UP.getVector())).y * 10);
        rotationController.setPitch(climbPitch);
    }

    private void executeSwimMovement(Vec3d targetPos) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d swimDirection = targetPos.subtract(playerPos).normalize();

        // Apply humanized direction
        Vec3d humanizedDirection = humanizer.getImperfectRotation(swimDirection);
        rotationController.updateRotation(humanizedDirection);

        // Swimming controls
        setMovementInputs(humanizedDirection, 0.8, false);

        // Jump in water to swim up
        if (targetPos.y > playerPos.y + 0.5) {
            mc.options.jumpKey.setPressed(true);
        }
    }

    private void executeFallMovement(Vec3d targetPos) {
        Vec3d playerPos = mc.player.getPos();

        // Only apply horizontal movement during fall
        Vec3d horizontalDirection = new Vec3d(targetPos.x - playerPos.x, 0, targetPos.z - playerPos.z).normalize();

        if (horizontalDirection.lengthSquared() > 0) {
            Vec3d humanizedDirection = humanizer.getImperfectRotation(horizontalDirection);
            rotationController.updateRotation(humanizedDirection);
            setMovementInputs(humanizedDirection, 0.6, false);
        }
    }

    private void setMovementInputs(Vec3d direction, double speedMultiplier, boolean shouldSprint) {
        // Calculate forward/side movement based on player's current yaw
        double playerYaw = Math.toRadians(mc.player.getYaw());
        double forwardComponent = direction.x * -Math.sin(playerYaw) + direction.z * Math.cos(playerYaw);
        double sideComponent = direction.x * Math.cos(playerYaw) + direction.z * Math.sin(playerYaw);

        // Apply speed multiplier
        forwardComponent *= speedMultiplier;
        sideComponent *= speedMultiplier;

        // Set key presses based on movement direction
        mc.options.forwardKey.setPressed(forwardComponent > 0.1);
        mc.options.backKey.setPressed(forwardComponent < -0.1);
        mc.options.leftKey.setPressed(sideComponent > 0.1);
        mc.options.rightKey.setPressed(sideComponent < -0.1);
        mc.options.sprintKey.setPressed(shouldSprint && forwardComponent > 0);
    }

    private boolean shouldJumpNow(Vec3d playerPos, Vec3d targetPos) {
        double horizontalDistance = Math.sqrt(Math.pow(targetPos.x - playerPos.x, 2) +
                Math.pow(targetPos.z - playerPos.z, 2));

        // Jump when we're close enough to the edge
        return horizontalDistance < 3.5 && horizontalDistance > 0.5;
    }

    private void stopAllMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }
}