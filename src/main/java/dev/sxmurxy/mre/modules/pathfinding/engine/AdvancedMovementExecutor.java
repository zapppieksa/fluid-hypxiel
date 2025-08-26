package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AdvancedMovementExecutor {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private MovementHumanizer humanizer;
    private RotationController rotationController;
    private MovementPredictor predictor;

    // Movement state
    private Vec3d lastTargetPos = null;
    private long movementStartTime = 0;
    private int pauseTicksRemaining = 0;
    private PathNode.MovementType currentMovementType = PathNode.MovementType.WALK;

    // Initialization state
    private boolean isInitialized = false;

    public AdvancedMovementExecutor() {
        // Safe initialization - components created lazily when needed
        System.out.println("[AdvancedMovementExecutor] Created, will initialize components on first use");
        initializeComponents();
    }

    private void initializeComponents() {
        try {
            if (isInitialized) {
                return;
            }

            System.out.println("[AdvancedMovementExecutor] Initializing components...");

            // Initialize components safely
            humanizer = new MovementHumanizer();
            rotationController = new RotationController();
            predictor = new MovementPredictor();

            isInitialized = true;
            System.out.println("[AdvancedMovementExecutor] ✓ All components initialized successfully");

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error initializing components: " + e.getMessage());
            e.printStackTrace();

            // Clean up partial initialization
            humanizer = null;
            rotationController = null;
            predictor = null;
            isInitialized = false;

            throw new RuntimeException("Failed to initialize AdvancedMovementExecutor", e);
        }
    }

    public void executeMovement(PathNode currentNode, PathNode nextNode) {
        // Safety checks
        if (!isInitialized) {
            System.err.println("[AdvancedMovementExecutor] Not initialized, cannot execute movement");
            return;
        }

        if (mc.player == null || mc.world == null) {
            return;
        }

        if (currentNode == null) {
            System.err.println("[AdvancedMovementExecutor] Current node is null");
            return;
        }

        try {
            // Handle pause if needed
            if (pauseTicksRemaining > 0) {
                pauseTicksRemaining--;
                stopAllMovement();
                return;
            }

            // Check for random pauses
            if (humanizer != null && humanizer.shouldPauseMovement()) {
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

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error executing movement: " + e.getMessage());
            e.printStackTrace();

            // Attempt to stop all movement on error
            try {
                stopAllMovement();
            } catch (Exception stopError) {
                System.err.println("[AdvancedMovementExecutor] Error stopping movement: " + stopError.getMessage());
            }
        }
    }

    private Vec3d getHumanizedTarget(PathNode currentNode, PathNode nextNode) {
        if (currentNode == null || currentNode.exactPosition == null) {
            return Vec3d.ZERO;
        }

        Vec3d baseTarget = currentNode.exactPosition;

        try {
            if (humanizer != null && mc.player != null) {
                // Apply humanization
                Vec3d playerPos = mc.player.getPos();
                Vec3d playerVelocity = mc.player.getVelocity();

                return humanizer.getHumanizedTarget(baseTarget, playerPos, playerVelocity);
            }
        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error in getHumanizedTarget: " + e.getMessage());
        }

        return baseTarget;
    }

    private void executeGroundMovement(Vec3d targetPos, boolean shouldSprint) {
        if (targetPos == null || mc.player == null) {
            return;
        }

        try {
            Vec3d playerPos = mc.player.getPos();
            Vec3d direction = targetPos.subtract(playerPos);

            if (direction.lengthSquared() < 0.001) {
                return; // Too close to target
            }

            direction = direction.normalize();

            // Apply humanized rotation
            Vec3d humanizedDirection = humanizer != null ?
                    humanizer.getImperfectRotation(direction) : direction;

            if (rotationController != null) {
                rotationController.updateRotation(humanizedDirection);
            }

            // Calculate movement inputs
            double speed = humanizer != null ? humanizer.getHumanizedSpeed() : 1.0;

            // Set movement keys
            setMovementInputs(humanizedDirection, speed, shouldSprint);

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error in executeGroundMovement: " + e.getMessage());
        }
    }

    private void executeJumpMovement(Vec3d targetPos, PathNode node) {
        if (targetPos == null || mc.player == null || node == null) {
            return;
        }

        try {
            Vec3d playerPos = mc.player.getPos();
            Vec3d direction = targetPos.subtract(playerPos);

            if (direction.lengthSquared() < 0.001) {
                return;
            }

            direction = direction.normalize();

            // More precise rotation for jumping
            Vec3d jumpDirection = humanizer != null ?
                    humanizer.getImperfectRotation(direction) : direction;

            if (rotationController != null) {
                rotationController.updateRotation(jumpDirection);
            }

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

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error in executeJumpMovement: " + e.getMessage());
        }
    }

    private void executeClimbMovement(Vec3d targetPos) {
        if (targetPos == null || mc.player == null) {
            return;
        }

        try {
            Vec3d playerPos = mc.player.getPos();

            // Look towards climb direction
            Vec3d climbDirection = targetPos.subtract(playerPos);
            if (climbDirection.lengthSquared() > 0.001) {
                climbDirection = climbDirection.normalize();
                if (rotationController != null) {
                    rotationController.updateRotation(climbDirection);
                }
            }

            // Climb up by holding W + looking up slightly
            if (mc.options != null) {
                mc.options.forwardKey.setPressed(true);
            }

            // Set pitch for climbing
            if (rotationController != null) {
                float climbPitch = -30.0f;
                if (humanizer != null) {
                    Vec3d upDir = humanizer.getImperfectRotation(Vec3d.of(Direction.UP.getVector()));
                    climbPitch += (float) (upDir.y * 10);
                }
                rotationController.setPitch(climbPitch);
            }

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error in executeClimbMovement: " + e.getMessage());
        }
    }

    private void executeSwimMovement(Vec3d targetPos) {
        if (targetPos == null || mc.player == null) {
            return;
        }

        try {
            Vec3d playerPos = mc.player.getPos();
            Vec3d swimDirection = targetPos.subtract(playerPos);

            if (swimDirection.lengthSquared() < 0.001) {
                return;
            }

            swimDirection = swimDirection.normalize();

            // Apply humanized direction
            Vec3d humanizedDirection = humanizer != null ?
                    humanizer.getImperfectRotation(swimDirection) : swimDirection;

            if (rotationController != null) {
                rotationController.updateRotation(humanizedDirection);
            }

            // Swimming controls
            setMovementInputs(humanizedDirection, 0.8, false);

            // Jump in water to swim up
            if (targetPos.y > playerPos.y + 0.5 && mc.options != null) {
                mc.options.jumpKey.setPressed(true);
            }

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error in executeSwimMovement: " + e.getMessage());
        }
    }

    private void executeFallMovement(Vec3d targetPos) {
        if (targetPos == null || mc.player == null) {
            return;
        }

        try {
            Vec3d playerPos = mc.player.getPos();

            // Only apply horizontal movement during fall
            Vec3d horizontalDirection = new Vec3d(targetPos.x - playerPos.x, 0, targetPos.z - playerPos.z);

            if (horizontalDirection.lengthSquared() > 0.001) {
                horizontalDirection = horizontalDirection.normalize();
                Vec3d humanizedDirection = humanizer != null ?
                        humanizer.getImperfectRotation(horizontalDirection) : horizontalDirection;

                if (rotationController != null) {
                    rotationController.updateRotation(humanizedDirection);
                }
                setMovementInputs(humanizedDirection, 0.6, false);
            }

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error in executeFallMovement: " + e.getMessage());
        }
    }

    private void setMovementInputs(Vec3d direction, double speedMultiplier, boolean shouldSprint) {
        if (direction == null || mc.player == null || mc.options == null) {
            return;
        }

        try {
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

        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error in setMovementInputs: " + e.getMessage());
        }
    }

    private boolean shouldJumpNow(Vec3d playerPos, Vec3d targetPos) {
        if (playerPos == null || targetPos == null) {
            return false;
        }

        try {
            double horizontalDistance = Math.sqrt(Math.pow(targetPos.x - playerPos.x, 2) +
                    Math.pow(targetPos.z - playerPos.z, 2));

            // Jump when we're close enough to the edge
            return horizontalDistance < 3.5 && horizontalDistance > 0.5;
        } catch (Exception e) {
            return false;
        }
    }

    private void stopAllMovement() {
        try {
            if (mc.options != null) {
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
            }
        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error stopping movement: " + e.getMessage());
        }
    }

    // Utility methods
    public boolean isInitialized() {
        return isInitialized;
    }

    public void reset() {
        try {
            stopAllMovement();
            lastTargetPos = null;
            movementStartTime = 0;
            pauseTicksRemaining = 0;
            currentMovementType = PathNode.MovementType.WALK;

            if (humanizer != null) {
                humanizer.reset();
            }

            System.out.println("[AdvancedMovementExecutor] State reset successfully");
        } catch (Exception e) {
            System.err.println("[AdvancedMovementExecutor] Error resetting: " + e.getMessage());
        }
    }

    public String getDebugInfo() {
        try {
            return String.format("AdvancedMovementExecutor[initialized=%s, type=%s, paused=%d]",
                    isInitialized,
                    currentMovementType,
                    pauseTicksRemaining);
        } catch (Exception e) {
            return "AdvancedMovementExecutor[error getting debug info]";
        }
    }
}