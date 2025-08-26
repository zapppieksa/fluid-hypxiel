package dev.sxmurxy.mre.modules.pathfinding.engine;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-level pathfinding engine that manages A* pathfinder and path optimization
 *
 * Features:
 * - Asynchronous path calculation
 * - Path optimization and smoothing
 * - Performance monitoring
 * - Cancellation support
 * - Error handling and recovery
 */
public class PathfinderEngine {

    private final PathfinderConfig config;
    private final AtomicBoolean cancellationToken = new AtomicBoolean(false);

    // Performance tracking
    private long lastCalculationTime = 0;
    private int totalCalculations = 0;
    private double averageCalculationTime = 0;

    public PathfinderEngine(PathfinderConfig config) {
        this.config = config;
    }

    /**
     * Calculate path to target asynchronously
     */
    public CompletableFuture<List<Vec3d>> calculatePath(BlockPos target) {
        cancellationToken.set(false);

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;

        if (player == null || world == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        BlockPos start = player.getBlockPos();

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                // Validate inputs
                if (!isValidPathfindingRequest(start, target, world)) {
                    return new ArrayList<>();
                }

                // Create and run A* pathfinder
                AStarPathfinder pathfinder = new AStarPathfinder(world, config, cancellationToken);
                List<PathNode> rawPath = pathfinder.findPath(start, target);

                if (cancellationToken.get() || rawPath == null || rawPath.isEmpty()) {
                    return new ArrayList<>();
                }

                // Optimize path
                List<PathNode> optimizedPath = optimizePath(rawPath, world);

                // Smooth path
                List<Vec3d> smoothedPath = PathSmoother.smoothPath(optimizedPath, config.PATH_SMOOTHNESS);

                // Update performance metrics
                updatePerformanceMetrics(startTime);

                return smoothedPath;

            } catch (Exception e) {
                System.err.println("Pathfinding error: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * Cancel current pathfinding operation
     */
    public void cancel() {
        cancellationToken.set(true);
    }

    /**
     * Validate pathfinding request
     */
    private boolean isValidPathfindingRequest(BlockPos start, BlockPos target, World world) {
        // Check if start and target are the same
        if (start.equals(target)) {
            return false;
        }

        // Check if positions are within world bounds
        if (!config.isPositionSafelyLoaded(start) || !config.isPositionSafelyLoaded(target)) {
            return false;
        }

        // Check distance limits
        double distance = Math.sqrt(start.getSquaredDistance(target));
        if (distance > config.getMaxSearchDistance(start, target)) {
            System.out.println("Target too far: " + distance + " blocks");
            return false;
        }

        // Check if world is available
        if (world == null) {
            return false;
        }

        return true;
    }

    /**
     * Optimize raw path by removing unnecessary waypoints
     */
    private List<PathNode> optimizePath(List<PathNode> rawPath, World world) {
        if (rawPath.size() < 3) {
            return rawPath;
        }

        List<PathNode> optimized = new ArrayList<>();
        optimized.add(rawPath.get(0)); // Always keep start

        int current = 0;

        while (current < rawPath.size() - 1) {
            int farthest = current + 1;

            // Find the farthest point we can reach directly
            for (int i = current + 2; i < rawPath.size(); i++) {
                if (hasLineOfSight(rawPath.get(current), rawPath.get(i), world)) {
                    farthest = i;
                } else {
                    break;
                }
            }

            optimized.add(rawPath.get(farthest));
            current = farthest;
        }

        return optimized;
    }

    /**
     * Check if there's a clear line of sight between two path nodes
     */
    private boolean hasLineOfSight(PathNode from, PathNode to, World world) {
        Vec3d fromPos = from.getBottomCenterVec3d().add(0, 1.6, 0); // Player eye height
        Vec3d toPos = to.getBottomCenterVec3d().add(0, 1.6, 0);

        // Don't optimize very long line-of-sight checks
        if (fromPos.distanceTo(toPos) > 10) {
            return false;
        }

        RaycastContext context = new RaycastContext(
                fromPos,
                toPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                MinecraftClient.getInstance().player
        );

        HitResult result = world.raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }

    /**
     * Update performance tracking metrics
     */
    private void updatePerformanceMetrics(long startTime) {
        long calculationTime = System.currentTimeMillis() - startTime;
        lastCalculationTime = calculationTime;
        totalCalculations++;

        // Update moving average
        averageCalculationTime = (averageCalculationTime * (totalCalculations - 1) + calculationTime) / totalCalculations;

        // Log performance for very long calculations
        if (calculationTime > 2000) {
            System.out.println("Slow pathfinding: " + calculationTime + "ms");
        }
    }

    /**
     * Get pathfinding performance statistics
     */
    public PathfindingPerformance getPerformanceStats() {
        return new PathfindingPerformance(
                lastCalculationTime,
                averageCalculationTime,
                totalCalculations,
                cancellationToken.get()
        );
    }

    /**
     * Check if engine is currently calculating a path
     */
    public boolean isCalculating() {
        return !cancellationToken.get();
    }

    /**
     * Reset performance statistics
     */
    public void resetPerformanceStats() {
        totalCalculations = 0;
        averageCalculationTime = 0;
        lastCalculationTime = 0;
    }

    /**
     * Create emergency path (direct line) when normal pathfinding fails
     */
    public List<Vec3d> createEmergencyPath(BlockPos start, BlockPos target) {
        List<Vec3d> emergencyPath = new ArrayList<>();

        Vec3d startVec = Vec3d.ofCenter(start);
        Vec3d targetVec = Vec3d.ofCenter(target);

        // Create simple direct path with intermediate points
        double distance = startVec.distanceTo(targetVec);
        int waypoints = Math.max(2, (int) (distance / 5)); // Every 5 blocks

        for (int i = 0; i <= waypoints; i++) {
            double progress = (double) i / waypoints;
            Vec3d waypoint = startVec.lerp(targetVec, progress);
            emergencyPath.add(waypoint);
        }

        return emergencyPath;
    }

    /**
     * Validate existing path and check if recalculation is needed
     */
    public boolean shouldRecalculatePath(List<Vec3d> currentPath, Vec3d playerPos) {
        if (currentPath == null || currentPath.isEmpty()) {
            return true;
        }

        // Check if player has deviated significantly from path
        if (currentPath.size() > 1) {
            Vec3d closestPoint = findClosestPointOnPath(currentPath, playerPos);
            if (playerPos.distanceTo(closestPoint) > 5.0) {
                return true; // Player too far from path
            }
        }

        // Check if path is blocked
        if (isPathBlocked(currentPath)) {
            return true;
        }

        return false;
    }

    /**
     * Find closest point on path to player position
     */
    private Vec3d findClosestPointOnPath(List<Vec3d> path, Vec3d playerPos) {
        Vec3d closest = path.get(0);
        double minDistance = playerPos.distanceTo(closest);

        for (Vec3d point : path) {
            double distance = playerPos.distanceTo(point);
            if (distance < minDistance) {
                minDistance = distance;
                closest = point;
            }
        }

        return closest;
    }

    /**
     * Check if current path has major obstacles
     */
    private boolean isPathBlocked(List<Vec3d> path) {
        if (path.size() < 2) return false;

        World world = MinecraftClient.getInstance().world;
        if (world == null) return false;

        // Check first few segments of path for major blockages
        int segmentsToCheck = Math.min(5, path.size() - 1);

        for (int i = 0; i < segmentsToCheck; i++) {
            Vec3d from = path.get(i);
            Vec3d to = path.get(i + 1);

            // Simple obstacle check
            BlockPos checkPos = BlockPos.ofFloored(from.lerp(to, 0.5));
            if (!world.getBlockState(checkPos).isAir() &&
                    !world.getBlockState(checkPos.up()).isAir()) {
                return true; // Path blocked
            }
        }

        return false;
    }

    /**
     * Get recommended path recalculation interval based on context
     */
    public long getRecalculationInterval() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return 30000; // 30 seconds default

        // More frequent recalculation if player is moving fast
        double velocity = player.getVelocity().length();
        if (velocity > 0.3) {
            return 15000; // 15 seconds when moving fast
        }

        return 30000; // 30 seconds normal
    }

    /**
     * Performance statistics record
     */
    public record PathfindingPerformance(
            long lastCalculationTime,
            double averageCalculationTime,
            int totalCalculations,
            boolean isCurrentlyCancelled
    ) {
        public boolean isPerformanceGood() {
            return averageCalculationTime < 1000 && lastCalculationTime < 2000;
        }

        public String getPerformanceString() {
            if (totalCalculations == 0) return "No calculations yet";

            return String.format("Last: %dms, Avg: %.0fms, Total: %d",
                    lastCalculationTime, averageCalculationTime, totalCalculations);
        }

        public double getCalculationsPerSecond() {
            if (totalCalculations == 0 || averageCalculationTime == 0) return 0;
            return 1000.0 / averageCalculationTime;
        }
    }
}