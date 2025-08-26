package dev.sxmurxy.mre.modules.pathfinding;

import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.engine.PathfinderEngine;
import dev.sxmurxy.mre.modules.pathfinding.movement.MovementManager;
import dev.sxmurxy.mre.modules.pathfinding.movement.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PathfinderAPI extends Module {

    private static PathfinderAPI instance;
    private final PathfinderEngine engine;
    private final MovementManager movementManager;
    private final RotationManager rotationManager;
    private final PathfinderConfig config;

    private static volatile boolean isPathing = false;
    private static final List<Vec3d> currentPath = new CopyOnWriteArrayList<>();
    private static int pathIndex = 0;
    private static BlockPos finalDestination;

    public PathfinderAPI() {
        super("Pathfinder", "Provides an API for advanced, human-like pathfinding.", ModuleCategory.MISCELLANEOUS);
        instance = this;
        this.config = new PathfinderConfig();
        this.engine = new PathfinderEngine(config);
        this.movementManager = new MovementManager(config);
        this.rotationManager = new RotationManager(config);
    }

    public static PathfinderAPI getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        stopPathfinding();
    }

    @Override
    public void onUpdate() {
        if (!isPathing || currentPath.isEmpty()) {
            return;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            stopPathfinding();
            return;
        }

        if (movementManager.isStuck()) {
            handleStuckSituation(player);
            return;
        }

        Vec3d targetNode = currentPath.get(pathIndex);
        if (player.getPos().distanceTo(targetNode) < config.getReachDistance(player)) {
            pathIndex++;
            movementManager.resetStuckDetector();
            if (pathIndex >= currentPath.size()) {
                sendMessage("§aPathfinder: Destination reached.");
                stopPathfinding();
                return;
            }
            targetNode = currentPath.get(pathIndex);
        }

        Vec3d nextNode = (pathIndex + 1 < currentPath.size()) ? currentPath.get(pathIndex + 1) : targetNode;

        rotationManager.updateRotation(player, targetNode, nextNode, finalDestination);
        movementManager.updateMovement(player, targetNode, nextNode);
    }
    public static List<Vec3d> getCurrentPath() {
        return currentPath;
    }
    private void handleStuckSituation(ClientPlayerEntity player) {
        if (movementManager.getStuckTicks() > config.RECALCULATE_STUCK_TICKS) {
            sendMessage("§cPathfinder: Stuck for too long. Recalculating path...");
            walkTo(finalDestination); // This will reset everything
        } else {
            movementManager.tryToUnstick(player);
        }
    }

    // FIX: Added sendMessage method to the class scope
    public void sendMessage(String message) {
        if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(message), false);
        }
    }

    public static void walkTo(BlockPos target) {
        if (isPathing) {
            stopPathfinding();
        }

        instance.setToggled(true);
        finalDestination = target;

        // FIX: Changed to use the instance method for sending messages
        instance.sendMessage("§7Pathfinder: Calculating path to " + target.toShortString() + "...");

        instance.engine.calculatePath(target).thenAccept(path -> {
            if (path.isEmpty()) {
                // FIX: Changed to use the instance method
                instance.sendMessage("§cPathfinder: Could not find a path.");
                stopPathfinding();
            } else {
                currentPath.clear();
                currentPath.addAll(path);
                pathIndex = 0;
                isPathing = true;
                instance.movementManager.resetStuckDetector();
                // FIX: Changed to use the instance method
                instance.sendMessage("§aPathfinder: Path found. Starting movement.");
            }
        });
    }

    public static void stopPathfinding() {
        if (!isPathing && !instance.isToggled()) return;

        isPathing = false;
        currentPath.clear();
        pathIndex = 0;
        instance.engine.cancel();

        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.setVelocity(Vec3d.ZERO);
            MovementManager.resetMovementKeys();
        }

        instance.setToggled(false);
        // FIX: Changed to use the instance method
        instance.sendMessage("§bPathfinder: Stopped.");
    }

    public static boolean isPathing() {
        return isPathing;
    }
}
