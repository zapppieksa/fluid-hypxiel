package dev.sxmurxy.mre.client.pathfinding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The public-facing API for the advanced pathfinding system. It provides a
 * clean, asynchronous interface to control the navigation agent.
 */
public class PathfinderAPI {

    private static Pathfinder pathfinder;

    /**
     * Ensures the Pathfinder instance is initialized and ready.
     */
    private static void initialize() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && (pathfinder == null || pathfinder.player != player)) {
            pathfinder = new Pathfinder(player);
        }
    }

    /**
     * Asynchronously finds a path and begins execution.
     * @param goal The target destination.
     * @param callback A consumer that receives true on successful path calculation, false otherwise.
     */
    public static void findAndFollowPath(BlockPos goal, Consumer<Boolean> callback) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            if (callback != null) callback.accept(false);
            return;
        }

        initialize();
        pathfinder.stop();

        // Asynchronously find the path on a separate thread
        CompletableFuture.supplyAsync(() -> pathfinder.findPath(mc.player.getBlockPos(), goal))
                // Then execute it on the main client thread
                .thenAcceptAsync(path -> {
                    if (path != null && !path.isEmpty()) {
                        pathfinder.executePath(path, goal);
                        if (callback != null) callback.accept(true);
                    } else {
                        if (pathfinder.debugMode) System.err.println("PathfinderAPI: Failed to calculate a path.");
                        if (callback != null) callback.accept(false);
                    }
                }, mc::execute);
    }

    /**
     * Must be called from a client tick event to update the agent's logic.
     */
    public static void tick() {
        if (pathfinder != null) {
            pathfinder.tick();
        }
    }

    /**
     * Immediately stops any active pathfinding.
     */
    public static void stop() {
        if (pathfinder != null) {
            pathfinder.stop();
        }
    }

    /**
     * @return True if the pathfinder is currently executing a path.
     */
    public static boolean isActive() {
        return pathfinder != null && pathfinder.isExecuting();
    }

    // --- Configuration Setters ---

    public static void setAotvEnabled(boolean enabled) {
        initialize();
        if (pathfinder != null) pathfinder.aotvEnabled = enabled;
    }

    public static void setEtherwarpEnabled(boolean enabled) {
        initialize();
        if (pathfinder != null) pathfinder.etherwarpEnabled = enabled;
    }

    public static void setDebugMode(boolean enabled) {
        initialize();
        if (pathfinder != null) pathfinder.debugMode = enabled;
    }

    public static void setPathfindingMode(Pathfinder.PathfindingMode mode) {
        initialize();
        if (pathfinder != null) pathfinder.mode = mode;
    }
}

