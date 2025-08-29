package dev.sxmurxy.mre.modules.pathfinding;

import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.engine.PathfinderEngine;
import dev.sxmurxy.mre.modules.pathfinding.movement.MovementManager;
import dev.sxmurxy.mre.modules.pathfinding.movement.RotationManager;
<<<<<<< Updated upstream
import dev.sxmurxy.mre.modules.settings.impl.BoolSetting;
import dev.sxmurxy.mre.modules.settings.impl.NumberSetting;
=======
import dev.sxmurxy.mre.modules.pathfinding.render.PathRenderer;
import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
>>>>>>> Stashed changes
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Advanced 3D pathfinding module with humanization and performance optimization
 *
 * Uses only PathfinderConfig constants for all settings (no GUI dependency)
 *
 * Features:
 * - High-performance A* pathfinding with 3D navigation
 * - Humanized movement with realistic timing and variations
 * - Parkour movements for skyblock-style navigation
 * - Climbing support (ladders, vines, scaffolding)
 * - Hazard avoidance (void, lava, dangerous blocks)
 * - Blue semi-transparent node rendering
 * - Performance optimization for long distances
 */
public class PathfindingModule extends Module {

    // Singleton instance for API access
    private static PathfindingModule instance;

    // Core pathfinding components
    private final PathfinderConfig config;
    public final PathfinderEngine engine;
    private final MovementManager movementManager;
    private final RotationManager rotationManager;

    // Path state management
    private static volatile boolean isPathing = false;
    private static final List<Vec3d> currentPath = new CopyOnWriteArrayList<>();
    private static int pathIndex = 0;
    private static BlockPos finalDestination;
    private static long lastPathCalculation = 0;
    private static long pathStartTime = 0;

    // Performance tracking
    private static int totalPathsCalculated = 0;
    private static double totalDistance = 0.0;
    private static long totalTime = 0;

    public PathfindingModule() {
        super("Pathfinder", "Advanced 3D pathfinding with human-like movement", ModuleCategory.MOVEMENT);
        instance = this;

        // Initialize core components using only config constants
        this.config = new PathfinderConfig();
        this.engine = new PathfinderEngine(config);
        this.movementManager = new MovementManager(config);
        this.rotationManager = new RotationManager(config);

        // Initialize path renderer
    }

    /**
     * Get singleton instance for API access
     */
    public static PathfindingModule getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        sendMessage("§a[Pathfinder] §7Enhanced pathfinding enabled.");
        sendMessage("§7Use §b.pathfind <x> <y> <z> §7to navigate or §b.pathfind help §7for commands.");
        sendMessage("§7Settings configured via PathfinderConfig constants:");
        sendMessage("§7  Max Fall Distance: §f" + PathfinderConfig.MAX_FALL_DISTANCE);
        sendMessage("§7  Path Smoothness: §f" + PathfinderConfig.PATH_SMOOTHNESS);
        sendMessage("§7  Max Search Distance: §f" + PathfinderConfig.MAX_SEARCH_DISTANCE);
        sendMessage("§7  Humanization: §aEnabled");
    }

    @Override
    public void onDisable() {
        stopPathfinding();
        sendMessage("§c[Pathfinder] §7Pathfinding disabled.");
        super.onDisable();
    }

    /**
     * Main update loop - handles pathfinding logic each tick
     */
    @Override
    public void onUpdate() {
        if (!isToggled() || !isPathing || currentPath.isEmpty()) {
            return;
        }

        ClientPlayerEntity player = mc.player;
        if (player == null || !player.isAlive()) {
            stopPathfinding();
            return;
        }

        // Handle stuck detection using config constants
        if (movementManager.isStuck()) {
            handleStuckSituation(player);
            return;
        }

        // Check if we've completed the path
        if (pathIndex >= currentPath.size()) {
            handlePathCompletion();
            return;
        }

        // Get current and next targets
        Vec3d currentTarget = currentPath.get(pathIndex);
        Vec3d nextTarget = (pathIndex + 1 < currentPath.size()) ?
                currentPath.get(pathIndex + 1) : currentTarget;

        // Check if we've reached current waypoint (using config reach distance)
        double reachDist = getEffectiveReachDistance();
        if (player.getPos().distanceTo(currentTarget) < reachDist) {
            pathIndex++;
            movementManager.resetStuckDetector();

            if (pathIndex >= currentPath.size()) {
                handlePathCompletion();
                return;
            }

            // Update targets for next waypoint
            currentTarget = currentPath.get(pathIndex);
            nextTarget = (pathIndex + 1 < currentPath.size()) ?
                    currentPath.get(pathIndex + 1) : currentTarget;
        }

        // Update movement and rotation using humanization from config
        updatePlayerMovement(player, currentTarget, nextTarget);

        // Periodic path validation and recalculation
        checkForPathRecalculation();
    }

    /**
     * Start pathfinding to the specified destination
     */
    public static void walkTo(BlockPos destination) {
        if (instance == null) {
            System.err.println("PathfindingModule not initialized!");
            return;
        }

        // Stop any existing pathfinding
        if (isPathing) {
            stopPathfinding();
        }

        // Validate destination
        if (!isValidDestination(destination)) {
            instance.sendMessage("§c[Pathfinder] Invalid destination: " + destination.toShortString());
            return;
        }

        // Enable the module and start pathfinding
        instance.setToggled(true);
        finalDestination = destination;
        lastPathCalculation = System.currentTimeMillis();
        pathStartTime = System.currentTimeMillis();

        instance.sendMessage("§a[Pathfinder] §7Calculating path to §f" + destination.toShortString() + "§7...");

        // Calculate path asynchronously
        instance.engine.calculatePath(destination).thenAccept(path -> {
            if (path.isEmpty()) {
                instance.sendMessage("§c[Pathfinder] No path found to destination.");
                stopPathfinding();
            } else {
                // Convert PathNode list to Vec3d list for rendering
                List<Vec3d> vec3dPath = path.stream()
                        .map(PathNode::getBottomCenterVec3d)
                        .collect(Collectors.toList());

                synchronized (currentPath) {
                    currentPath.clear();
                    currentPath.addAll(vec3dPath);
                    pathIndex = 0;
                }
                isPathing = true;
                instance.movementManager.resetStuckDetector();

                double distance = calculatePathDistance(vec3dPath);
                int waypoints = vec3dPath.size();
                totalPathsCalculated++;
                totalDistance += distance;

                instance.sendMessage("§a[Pathfinder] §7Path found! §f" + waypoints + " §7waypoints, §f"
                        + String.format("%.1f", distance) + " §7blocks total.");
                instance.sendMessage("§7Using humanized movement with config settings.");
            }
        }).exceptionally(throwable -> {
            instance.sendMessage("§c[Pathfinder] Error calculating path: " + throwable.getMessage());
            stopPathfinding();
            return null;
        });
    }

    /**
     * Stop current pathfinding
     */
    public static void stopPathfinding() {
        if (isPathing && instance != null) {
            long duration = (System.currentTimeMillis() - pathStartTime) / 1000;
            totalTime += duration;

            instance.sendMessage("§c[Pathfinder] §7Pathfinding stopped. Duration: §f" + duration + "s");
        }

        isPathing = false;
        synchronized (currentPath) {
            currentPath.clear();
        }
        pathIndex = 0;
        finalDestination = null;

        // Reset movement keys
        if (instance != null) {
            MovementManager.resetMovementKeys();
        }
    }

    /**
     * Get current path for rendering
     */
    public static List<Vec3d> getCurrentPath() {
        // Always show path when module is active (using config constant for visibility)
        return isPathing && instance != null && instance.isToggled() ?
                new CopyOnWriteArrayList<>(currentPath) : null;
    }

    /**
     * Check if currently pathfinding
     */
    public static boolean isPathing() {
        return isPathing && instance != null && instance.isToggled();
    }

    /**
     * Get pathfinding statistics
     */
    public static String getStatistics() {
        if (instance == null) return "Module not initialized";

        double avgDistance = totalPathsCalculated > 0 ? totalDistance / totalPathsCalculated : 0;
        double avgTime = totalPathsCalculated > 0 ? (double) totalTime / totalPathsCalculated : 0;

        return String.format(
                "§6Pathfinding Statistics:\n" +
                        "§7Paths calculated: §f%d\n" +
                        "§7Total distance: §f%.1f blocks\n" +
                        "§7Total time: §f%d seconds\n" +
                        "§7Average distance: §f%.1f blocks\n" +
                        "§7Average time: §f%.1f seconds\n" +
                        "§7Current status: %s",
                totalPathsCalculated, totalDistance, totalTime, avgDistance, avgTime,
                isPathing ? "§aPathfinding" : "§cIdle"
        );
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Update player movement and rotation using config-based humanization
     */
    private void updatePlayerMovement(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget) {
        // Always use humanized movement (config-controlled)
        rotationManager.updateRotation(player, currentTarget, nextTarget, finalDestination);
        movementManager.updateMovement(player, currentTarget, nextTarget);
    }

    /**
     * Handle stuck situations using config timeouts
     */
    private void handleStuckSituation(ClientPlayerEntity player) {
        if (movementManager.getStuckTicks() > PathfinderConfig.RECALCULATE_STUCK_TICKS) {
            sendMessage("§e[Pathfinder] §7Stuck detected. Recalculating path...");
            walkTo(finalDestination); // Recalculate entire path
        } else {
            // Try to unstick with random movement
            movementManager.tryToUnstick(player);
        }
    }

    /**
     * Handle path completion
     */
    private void handlePathCompletion() {
        long duration = (System.currentTimeMillis() - pathStartTime) / 1000;
        double pathDistance = calculatePathDistance(currentPath);
        totalTime += duration;

        sendMessage("§a[Pathfinder] §7Destination reached!");
        sendMessage("§7Path completed in §f" + duration + "s §7covering §f" +
                String.format("%.1f", pathDistance) + "§7 blocks.");

        stopPathfinding();
    }

    /**
     * Get effective reach distance from config
     */
    private double getEffectiveReachDistance() {
        // Use config constant instead of GUI setting
        return 1.2; // Could be made configurable in PathfinderConfig if needed
    }

    /**
     * Check for path recalculation needs
     */
    private void checkForPathRecalculation() {
        long timeSinceLastCalc = System.currentTimeMillis() - lastPathCalculation;

        // Recalculate if path is very old (using config timeout)
        if (timeSinceLastCalc > PathfinderConfig.PATH_CALCULATION_TIMEOUT_MS * 2) {
            sendMessage("§e[Pathfinder] §7Path is stale, recalculating...");
            walkTo(finalDestination);
        }
    }

    /**
     * Validate destination using config constraints
     */
    private static boolean isValidDestination(BlockPos destination) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        BlockPos playerPos = mc.player.getBlockPos();
        double distance = Math.sqrt(playerPos.getSquaredDistance(destination));

        // Check against max search distance from config
        if (distance > PathfinderConfig.MAX_SEARCH_DISTANCE) {
            if (instance != null) {
                instance.sendMessage("§c[Pathfinder] Destination too far: " +
                        String.format("%.1f", distance) + " blocks (max: " +
                        PathfinderConfig.MAX_SEARCH_DISTANCE + ")");
            }
            return false;
        }

        // Check for dangerous Y levels
        if (destination.getY() < 0) {
            if (instance != null) {
                instance.sendMessage("§c[Pathfinder] Destination is in the void (Y < 0)");
            }
            return false;
        }

        return true;
    }

    /**
     * Calculate total path distance
     */
    private static double calculatePathDistance(List<Vec3d> path) {
        if (path.size() < 2) return 0.0;

        double totalDistance = 0.0;
        for (int i = 1; i < path.size(); i++) {
            totalDistance += path.get(i - 1).distanceTo(path.get(i));
        }

<<<<<<< Updated upstream
    /**
     * Get effective reach distance based on settings
     */
    private double getEffectiveReachDistance() {
        // Cast the generic Object from getValue() to a number type (Double) before performing arithmetic.
        return (Double) reachDistance.get() * ((Double) movementSpeed.get() / 1.0);
=======
        return totalDistance;
>>>>>>> Stashed changes
    }

    /**
     * Send message to player
     */
    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }
}