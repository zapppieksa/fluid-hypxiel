package dev.sxmurxy.mre.client.pathfinding;

import dev.sxmurxy.mre.client.movement.MovementController;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Main API interface for the advanced pathfinding system.
 * Provides high-level methods for pathfinding operations with humanized movement and rendering.
 */
public class PathfinderAPI {
    private static PathfinderAPI instance;

    private final Pathfinder pathfinder;
    private final MovementController movementController;
    private final ExecutorService executorService;

    // State management
    private boolean isActive = false;
    private boolean renderPathEnabled = true;
    private boolean debugMode = true;
    private boolean aotvEnabled = true;
    private boolean etherwarpEnabled = true;
    private double pathfindingSpeed = 1.0;

    // Current path data for rendering
    private List<Pathfinder.PathNode> currentPath = null;
    private List<Vec3d> smoothedPath = null;

    private PathfinderAPI() {
        this.pathfinder = new Pathfinder();
        this.movementController = new MovementController();
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Pathfinder-Thread");
            t.setDaemon(true);
            return t;
        });

        initialize();
    }

    public static PathfinderAPI getInstance() {
        if (instance == null) {
            instance = new PathfinderAPI();
        }
        return instance;
    }

    private void initialize() {
        // Register tick event for movement controller
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            movementController.tick();
        });

        System.out.println("PathfinderAPI initialized with advanced humanized movement!");
    }

    /**
     * Main pathfinding method - finds path and executes movement.
     */
    public static void findAndFollowPath(BlockPos target, Consumer<Boolean> callback) {
        getInstance().pathfindToAsync(target).thenAccept(success -> {
            if (callback != null) {
                callback.accept(success);
            }
        });
    }

    /**
     * Asynchronous pathfinding to avoid blocking the main thread.
     */
    public CompletableFuture<Boolean> pathfindToAsync(BlockPos target) {
        if (isActive) {
            stop(); // Stop current pathfinding
        }

        return CompletableFuture.supplyAsync(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) {
                return false;
            }

            BlockPos start = BlockPos.ofFloored(mc.player.getPos());
            isActive = true;

            try {
                if (debugMode) {
                    System.out.printf("Starting pathfinding from %s to %s%n",
                            start.toShortString(), target.toShortString());
                }

                // Configure pathfinder based on settings
                configurePathfinder();

                // Find path
                boolean pathFound = pathfinder.findPath(start, target);

                if (pathFound) {
                    currentPath = pathfinder.getCurrentPath();
                    generateSmoothedPath();

                    // Start movement execution
                    movementController.executePath(currentPath);

                    if (debugMode) {
                        System.out.printf("Path found with %d nodes. Starting execution.%n",
                                currentPath.size());
                    }

                    return true;
                } else {
                    if (debugMode) {
                        System.out.println("No path found to target.");
                    }
                    return false;
                }

            } finally {
                isActive = false;
            }
        }, executorService);
    }

    /**
     * Generate smoothed path for rendering purposes.
     */
    private void generateSmoothedPath() {
        if (currentPath == null || currentPath.isEmpty()) {
            smoothedPath = null;
            return;
        }

        smoothedPath = new java.util.ArrayList<>();

        // Add all path positions
        for (Pathfinder.PathNode node : currentPath) {
            smoothedPath.add(node.position);
        }

        // Add interpolated points for smoother visualization
        List<Vec3d> interpolated = new java.util.ArrayList<>();
        for (int i = 0; i < smoothedPath.size() - 1; i++) {
            Vec3d start = smoothedPath.get(i);
            Vec3d end = smoothedPath.get(i + 1);

            interpolated.add(start);

            // Add 2 interpolated points between each waypoint
            for (int j = 1; j <= 2; j++) {
                double t = j / 3.0;
                Vec3d interpolatedPoint = start.lerp(end, t);
                interpolated.add(interpolatedPoint);
            }
        }

        if (!smoothedPath.isEmpty()) {
            interpolated.add(smoothedPath.get(smoothedPath.size() - 1));
        }

        smoothedPath = interpolated;
    }

    /**
     * Configure pathfinder based on current settings.
     */
    private void configurePathfinder() {
        movementController.setAotvEnabled(aotvEnabled);
        movementController.setEtherwarpEnabled(etherwarpEnabled);
    }

    /**
     * Stop all pathfinding and movement.
     */
    public static void stop() {
        getInstance().stopInternal();
    }

    private void stopInternal() {
        isActive = false;
        pathfinder.stopPathfinding();
        currentPath = null;
        smoothedPath = null;

        if (debugMode) {
            System.out.println("Pathfinding stopped.");
        }
    }

    // Configuration methods
    public static void setDebugMode(boolean enabled) {
        getInstance().debugMode = enabled;
        System.out.println("Pathfinding debug mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public static void setAotvEnabled(boolean enabled) {
        getInstance().aotvEnabled = enabled;
        getInstance().movementController.setAotvEnabled(enabled);
    }

    public static void setEtherwarpEnabled(boolean enabled) {
        getInstance().etherwarpEnabled = enabled;
        getInstance().movementController.setEtherwarpEnabled(enabled);
    }

    public static void setPathfindingSpeed(double speed) {
        getInstance().pathfindingSpeed = Math.max(0.1, Math.min(3.0, speed));
    }

    public static void setRenderPathEnabled(boolean enabled) {
        getInstance().renderPathEnabled = enabled;
    }

    // Status methods
    public static boolean isActive() {
        return getInstance().isActive || getInstance().movementController.isExecuting();
    }

    public static boolean isRenderPathEnabled() {
        return getInstance().renderPathEnabled;
    }

    public static boolean isDebugMode() {
        return getInstance().debugMode;
    }

    // Path data for rendering
    public static List<Pathfinder.PathNode> getSimplifiedPath() {
        return getInstance().currentPath;
    }

    public static List<Vec3d> getSmoothedPath() {
        return getInstance().smoothedPath;
    }

    /**
     * Get current pathfinding statistics.
     */
    public static PathfindingStats getStats() {
        PathfinderAPI api = getInstance();
        return new PathfindingStats(
                api.isActive,
                api.currentPath != null ? api.currentPath.size() : 0,
                api.movementController.getCurrentPathIndex(),
                api.renderPathEnabled,
                api.aotvEnabled,
                api.etherwarpEnabled,
                api.debugMode
        );
    }

    /**
     * Tick method for external calling if needed.
     */
    public static void tick() {
        // Movement controller is already ticked via event handler
        // This method is here for compatibility
    }

    public void shutdown() {
        stop();
        executorService.shutdown();
    }

    // Data classes
    public record PathfindingStats(
            boolean isActive,
            int pathLength,
            int currentIndex,
            boolean renderEnabled,
            boolean aotvEnabled,
            boolean etherwarpEnabled,
            boolean debugMode
    ) {
        public String getStatusString() {
            if (isActive) {
                return String.format("Active - %d/%d nodes", currentIndex, pathLength);
            } else {
                return "Inactive";
            }
        }
    }
}