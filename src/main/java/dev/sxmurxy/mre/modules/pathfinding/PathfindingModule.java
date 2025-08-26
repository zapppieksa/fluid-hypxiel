package dev.sxmurxy.mre.modules.pathfinding;

import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.engine.PathfinderEngine;
import dev.sxmurxy.mre.modules.pathfinding.movement.MovementManager;
import dev.sxmurxy.mre.modules.pathfinding.movement.RotationManager;
import dev.sxmurxy.mre.modules.pathfinding.render.PathRenderer;
import dev.sxmurxy.mre.modules.settings.impl.BoolSetting;
import dev.sxmurxy.mre.modules.settings.impl.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Advanced 3D pathfinding module with humanization and performance optimization
 * Features:
 * - High-performance A* pathfinding with 3D navigation
 * - Humanized movement with realistic timing and variations
 * - Hazard avoidance and obstacle detection
 * - Blue semi-transparent node rendering
 * - Configurable settings and real-time adjustments
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

    // Module settings - configurable via GUI
    private final NumberSetting maxFallDistance;
    public final NumberSetting pathSmoothness;
    private final NumberSetting movementSpeed;
    private final NumberSetting rotationSpeed;
    private final NumberSetting reachDistance;
    public final BoolSetting showPath;
    public final BoolSetting humanizeMovement;
    private final BoolSetting avoidHazards;
    private final BoolSetting allowParkour;
    private final BoolSetting allowClimbing;
    private final NumberSetting maxSearchDistance;

    public PathfindingModule() {
        super("Pathfinder", "Advanced 3D pathfinding with human-like movement", ModuleCategory.MOVEMENT);
        instance = this;

        // Initialize configuration settings
        maxFallDistance = new NumberSetting("Max Fall Distance", 4.0, 1.0, 10.0, 1.0);
        pathSmoothness = new NumberSetting("Path Smoothness", 0.3, 0.0, 1.0, 0.1);
        movementSpeed = new NumberSetting("Movement Speed", 1.0, 0.1, 2.0, 0.1);
        rotationSpeed = new NumberSetting("Rotation Speed", 2.5, 0.5, 10.0, 0.5);
        reachDistance = new NumberSetting("Reach Distance", 1.2, 0.5, 3.0, 0.1);
        maxSearchDistance = new NumberSetting("Max Search Distance", 1000.0, 100.0, 5000.0, 100.0);

        showPath = new BoolSetting("Show Path", true);
        humanizeMovement = new BoolSetting("Humanize Movement", true);
        avoidHazards = new BoolSetting("Avoid Hazards", true);
        allowParkour = new BoolSetting("Allow Parkour", true);
        allowClimbing = new BoolSetting("Allow Climbing", true);

        // Initialize core components
        this.config = new PathfinderConfig();
        this.engine = new PathfinderEngine(config);
        this.movementManager = new MovementManager(config);
        this.rotationManager = new RotationManager(config);

        // Initialize path renderer
        PathRenderer.initialize();
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

        // Handle stuck detection
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

        // Check if we've reached current waypoint
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

        // Update movement and rotation
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
                synchronized (currentPath) {
                    currentPath.clear();
                    currentPath.addAll(path);
                    pathIndex = 0;
                }
                isPathing = true;
                instance.movementManager.resetStuckDetector();

                double distance = calculatePathDistance(path);
                int waypoints = path.size();

                instance.sendMessage("§a[Pathfinder] §7Path found! §f" + waypoints + " §7waypoints, §f"
                        + String.format("%.1f", distance) + " §7blocks total.");
            }
        }).exceptionally(throwable -> {
            instance.sendMessage("§c[Pathfinder] Pathfinding error: " + throwable.getMessage());
            stopPathfinding();
            return null;
        });
    }

    /**
     * Stop all pathfinding activities
     */
    public static void stopPathfinding() {
        if (!isPathing && (instance == null || !instance.isToggled())) {
            return;
        }

        // Cancel pathfinding
        isPathing = false;
        synchronized (currentPath) {
            currentPath.clear();
        }
        pathIndex = 0;

        // Cancel engine calculations
        if (instance != null) {
            instance.engine.cancel();
        }

        // Reset player movement
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.setVelocity(Vec3d.ZERO);
            MovementManager.resetMovementKeys();
        }

        // Update module state
        if (instance != null) {
            instance.setToggled(false);
            long duration = (System.currentTimeMillis() - pathStartTime) / 1000;
            instance.sendMessage("§b[Pathfinder] §7Pathfinding stopped. Duration: §f" + duration + "s");
        }

        finalDestination = null;
    }

    /**
     * Get current path for rendering (returns null if path should be hidden)
     */
    public static List<Vec3d> getCurrentPath() {
        return (instance != null && instance.showPath.isEnabled()) ? currentPath : null;
    }

    /**
     * Check if currently pathfinding
     */
    public static boolean isPathing() {
        return isPathing && instance != null && instance.isToggled();
    }

    /**
     * Update player movement and rotation
     */
    private void updatePlayerMovement(ClientPlayerEntity player, Vec3d currentTarget, Vec3d nextTarget) {
        if (humanizeMovement.isEnabled()) {
            // Use humanized movement with realistic timing
            rotationManager.updateRotation(player, currentTarget, nextTarget, finalDestination);
            movementManager.updateMovement(player, currentTarget, nextTarget);
        } else {
            // Use direct, efficient movement
            updateDirectMovement(player, currentTarget);
        }
    }

    /**
     * Direct movement without humanization (for speed)
     */
    private void updateDirectMovement(ClientPlayerEntity player, Vec3d target) {
        Vec3d playerPos = player.getPos();
        Vec3d direction = target.subtract(playerPos).normalize();

        // Reset all movement keys
        MovementManager.resetMovementKeys();

        // Calculate movement based on player's facing direction
        Vec3d facing = Vec3d.fromPolar(0, player.getYaw());
        double forward = direction.dotProduct(facing);
        double strafe = direction.dotProduct(new Vec3d(-facing.z, 0, facing.x));

        // Apply movement keys
        if (forward > 0.2) mc.options.forwardKey.setPressed(true);
        else if (forward < -0.2) mc.options.backKey.setPressed(true);

        if (strafe > 0.2) mc.options.rightKey.setPressed(true);
        else if (strafe < -0.2) mc.options.leftKey.setPressed(true);

        // Handle vertical movement
        if (target.y > playerPos.y + 0.5) {
            mc.options.jumpKey.setPressed(true);
        }

        // Direct rotation to target
        rotationManager.snapToTarget(player, target);
    }

    /**
     * Handle stuck situations
     */
    private void handleStuckSituation(ClientPlayerEntity player) {
        if (movementManager.getStuckTicks() > config.RECALCULATE_STUCK_TICKS) {
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
        double totalDistance = calculatePathDistance(currentPath);

        sendMessage("§a[Pathfinder] §7Destination reached!");
        sendMessage("§7Duration: §f" + duration + "s §7| Distance: §f" +
                String.format("%.1f", totalDistance) + " §7blocks");

        stopPathfinding();
    }

    /**
     * Check if path needs recalculation
     */
    private void checkForPathRecalculation() {
        if (config.shouldRecalculatePath(lastPathCalculation) && finalDestination != null) {
            sendMessage("§7[Pathfinder] Optimizing path...");
            walkTo(finalDestination);
        }
    }

    /**
     * Validate if destination is reasonable
     */
    private static boolean isValidDestination(BlockPos pos) {
        // Check world boundaries
        if (Math.abs(pos.getX()) > 30000000 || Math.abs(pos.getZ()) > 30000000) {
            return false;
        }

        // Check Y boundaries
        if (pos.getY() < -64 || pos.getY() > 320) {
            return false;
        }

        return true;
    }

    /**
     * Calculate total distance of path
     */
    private static double calculatePathDistance(List<Vec3d> path) {
        if (path.size() < 2) return 0;

        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += path.get(i - 1).distanceTo(path.get(i));
        }
        return total;
    }

    /**
     * Get effective reach distance based on settings
     */
    private double getEffectiveReachDistance() {
        // Cast the generic Object from getValue() to a number type (Double) before performing arithmetic.
        return (Double) reachDistance.getValue() * ((Double) movementSpeed.getValue() / 1.0);
    }

    /**
     * Send message to player
     */
    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }

    // ==================== PUBLIC API METHODS ====================

    public void setMaxFallDistance(double distance) {
        maxFallDistance.setValue(Math.max(1.0, Math.min(10.0, distance)));
    }

    public void setPathSmoothness(double smoothness) {
        pathSmoothness.setValue(Math.max(0.0, Math.min(1.0, smoothness)));
    }

    public void setMovementSpeed(double speed) {
        movementSpeed.setValue(Math.max(0.1, Math.min(2.0, speed)));
    }

    public void setRotationSpeed(double speed) {
        rotationSpeed.setValue(Math.max(0.5, Math.min(10.0, speed)));
    }

    public void setShowPath(boolean show) {
        showPath.setEnabled(show);
    }

    public void setHumanizeMovement(boolean humanize) {
        humanizeMovement.setEnabled(humanize);
    }

    public void setAvoidHazards(boolean avoid) {
        avoidHazards.setEnabled(avoid);
    }

    public void setAllowParkour(boolean allow) {
        allowParkour.setEnabled(allow);
    }

    public void setAllowClimbing(boolean allow) {
        allowClimbing.setEnabled(allow);
    }

    /**
     * Force recalculation of current path
     */
    public void recalculatePath() {
        if (finalDestination != null && isPathing) {
            sendMessage("§7[Pathfinder] Recalculating path...");
            walkTo(finalDestination);
        }
    }

    /**
     * Get current pathfinding statistics
     */
    public PathfindingStats getStats() {
        return new PathfindingStats(
                currentPath.size(),
                pathIndex,
                isPathing,
                finalDestination,
                movementManager != null ? movementManager.getStuckTicks() : 0,
                System.currentTimeMillis() - lastPathCalculation,
                pathStartTime > 0 ? System.currentTimeMillis() - pathStartTime : 0
        );
    }

    /**
     * Pathfinding statistics record
     */
    public record PathfindingStats(
            int totalWaypoints,
            int currentWaypointIndex,
            boolean isActive,
            BlockPos destination,
            int stuckTicks,
            long timeSinceCalculation,
            long totalPathingTime
    ) {
        public double getProgress() {
            return totalWaypoints > 0 ? (double) currentWaypointIndex / totalWaypoints : 0.0;
        }

        public boolean isStuck() {
            return stuckTicks > 60; // 3 seconds at 20 TPS
        }

        public String getStatusString() {
            if (!isActive) return "§7Inactive";
            if (isStuck()) return "§cStuck (" + (stuckTicks / 20) + "s)";
            return "§aActive §7(" + currentWaypointIndex + "/" + totalWaypoints + ")";
        }

        public String getDurationString() {
            return String.format("%.1fs", totalPathingTime / 1000.0);
        }
    }
}