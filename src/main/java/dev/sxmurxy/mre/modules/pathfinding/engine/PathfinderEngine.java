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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * High-level pathfinding engine that manages A* pathfinder and path optimization
 * Uses only PathfinderConfig constants for all settings (no GUI dependency)
 *
 * Features:
 * - Asynchronous path calculation with thread pool
 * - Path optimization and smoothing using config constants
 * - Performance monitoring and statistics
 * - Cancellation support for responsive UI
 * - Error handling and recovery mechanisms
 * - Cached segments for performance on long distances
 * - Path validation and safety checks
 */
public class PathfinderEngine {

    private final PathfinderConfig config;
    private final AtomicBoolean cancellationToken = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Pathfinding-Thread");
        t.setDaemon(true);
        return t;
    });

    // Performance tracking
    private long lastCalculationTime = 0;
    private int totalCalculations = 0;
    private double averageCalculationTime = 0;
    private int successfulPaths = 0;
    private int failedPaths = 0;

    // Path caching for segments
    private static final int SEGMENT_CACHE_SIZE = 50;
    private final List<CachedPathSegment> segmentCache = new ArrayList<>();

    public PathfinderEngine(PathfinderConfig config) {
        this.config = config;
    }

    /**
     * Calculate path to target asynchronously with optimization
     */
    public CompletableFuture<List<PathNode>> calculatePath(BlockPos target) {
        cancellationToken.set(false);

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;

        if (player == null || world == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        BlockPos start = player.getBlockPos();

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            totalCalculations++;

            try {
                // Validate pathfinding request
                if (!isValidPathfindingRequest(start, target, world)) {
                    failedPaths++;
                    return new ArrayList<>();
                }

                // Check for cached segments first
                List<PathNode> cachedPath = findCachedPath(start, target);
                if (!cachedPath.isEmpty()) {
                    System.out.println("Using cached path segment");
                    successfulPaths++;
                    updatePerformanceStats(startTime);
                    return cachedPath;
                }

                // Create and run A* pathfinder
                AStarPathfinder pathfinder = new AStarPathfinder(world, config, cancellationToken);
                List<PathNode> rawPath = pathfinder.findPath(start, target);

                if (rawPath.isEmpty()) {
                    failedPaths++;
                    return new ArrayList<>();
                }

                // Optimize and smooth path using config constants
                List<PathNode> optimizedPath = optimizePath(rawPath, world);

                // Cache segments for future use
                cachePathSegments(optimizedPath);

                successfulPaths++;
                updatePerformanceStats(startTime);

                return optimizedPath;

            } catch (Exception e) {
                System.err.println("Pathfinding error: " + e.getMessage());
                e.printStackTrace();
                failedPaths++;
                return new ArrayList<>();
            }
        }, executorService);
    }

    /**
     * Validate pathfinding request
     */
    private boolean isValidPathfindingRequest(BlockPos start, BlockPos target, ClientWorld world) {
        // Check distance constraints using config
        double distance = Math.sqrt(start.getSquaredDistance(target));
        if (distance > PathfinderConfig.MAX_SEARCH_DISTANCE) {
            System.out.println("Target too far: " + distance + " blocks");
            return false;
        }

        // Check for reasonable Y levels
        if (target.getY() < -64 || target.getY() > 320) {
            System.out.println("Target Y level out of bounds: " + target.getY());
            return false;
        }

        // Check if world is loaded
        if (!world.isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)) {
            System.out.println("Target chunk not loaded");
            return false;
        }

        return true;
    }

    /**
     * Optimize path using various techniques and config constants
     */
    private List<PathNode> optimizePath(List<PathNode> rawPath, World world) {
        if (rawPath.size() < 3) return rawPath;

        List<PathNode> optimizedPath = new ArrayList<>(rawPath);

        // 1. Remove redundant waypoints using line-of-sight
        optimizedPath = removeRedundantWaypoints(optimizedPath, world);

        // 2. Smooth path using config smoothness factor
        optimizedPath = smoothPath(optimizedPath);

        // 3. Optimize vertical movements
        optimizedPath = optimizeVerticalMovements(optimizedPath, world);

        // 4. Add safety waypoints for dangerous sections
        optimizedPath = addSafetyWaypoints(optimizedPath, world);

        return optimizedPath;
    }

    /**
     * Remove waypoints that can be skipped using line-of-sight
     */
    private List<PathNode> removeRedundantWaypoints(List<PathNode> path, World world) {
        if (path.size() < 3) return path;

        List<PathNode> simplified = new ArrayList<>();
        simplified.add(path.get(0)); // Always keep start

        int currentIndex = 0;

        while (currentIndex < path.size() - 1) {
            int farthestReachable = currentIndex + 1;

            // Find farthest reachable waypoint
            for (int testIndex = currentIndex + 2; testIndex < path.size(); testIndex++) {
                if (hasLineOfSight(path.get(currentIndex), path.get(testIndex), world)) {
                    farthestReachable = testIndex;
                } else {
                    break;
                }
            }

            // Add the farthest reachable waypoint
            if (farthestReachable < path.size()) {
                simplified.add(path.get(farthestReachable));
            }

            currentIndex = farthestReachable;
        }

        // Always keep destination
        if (!simplified.contains(path.get(path.size() - 1))) {
            simplified.add(path.get(path.size() - 1));
        }

        System.out.println("Simplified path from " + path.size() + " to " + simplified.size() + " waypoints");
        return simplified;
    }

    /**
     * Check line of sight between two nodes
     */
    private boolean hasLineOfSight(PathNode from, PathNode to, World world) {
        Vec3d fromPos = from.getCenterVec3d();
        Vec3d toPos = to.getCenterVec3d();

        // Simple raycast to check for obstacles
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
     * Smooth path using config smoothness factor
     */
    private List<PathNode> smoothPath(List<PathNode> path) {
        if (path.size() < 3) return path;

        double smoothness = PathfinderConfig.PATH_SMOOTHNESS;
        if (smoothness <= 0.0) return path;

        List<PathNode> smoothed = new ArrayList<>();
        smoothed.add(path.get(0)); // Keep start point

        for (int i = 1; i < path.size() - 1; i++) {
            PathNode prev = path.get(i - 1);
            PathNode current = path.get(i);
            PathNode next = path.get(i + 1);

            // Apply smoothing by interpolating between waypoints
            Vec3d prevPos = prev.getCenterVec3d();
            Vec3d currentPos = current.getCenterVec3d();
            Vec3d nextPos = next.getCenterVec3d();

            // Calculate smoothed position
            Vec3d smoothedPos = currentPos
                    .multiply(1.0 - smoothness)
                    .add(prevPos.add(nextPos).multiply(0.5 * smoothness));

            // Create smoothed node
            BlockPos smoothedBlockPos = BlockPos.ofFloored(smoothedPos);
            PathNode smoothedNode = new PathNode(smoothedBlockPos);
            smoothedNode.setMovementType(current.getMovementType());
            smoothedNode.setParent(current.getParent());

            smoothed.add(smoothedNode);
        }

        smoothed.add(path.get(path.size() - 1)); // Keep end point
        return smoothed;
    }

    /**
     * Optimize vertical movements for efficiency
     */
    private List<PathNode> optimizeVerticalMovements(List<PathNode> path, World world) {
        List<PathNode> optimized = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            PathNode current = path.get(i);

            // Look for consecutive vertical movements that can be combined
            if (i < path.size() - 2) {
                PathNode next = path.get(i + 1);
                PathNode afterNext = path.get(i + 2);

                // If we have consecutive climbing movements, try to combine them
                if (current.isClimbing() && next.isClimbing()) {
                    BlockPos combinedPos = new BlockPos(
                            current.getPos().getX(),
                            afterNext.getPos().getY(),
                            current.getPos().getZ()
                    );

                    // Check if direct climb is possible
                    if (canDirectClimb(current.getPos(), combinedPos, world)) {
                        PathNode combined = new PathNode(combinedPos);
                        combined.setMovementType(current.getMovementType());
                        combined.setParent(current.getParent());
                        optimized.add(combined);
                        i += 2; // Skip the intermediate nodes
                        continue;
                    }
                }
            }

            optimized.add(current);
        }

        return optimized;
    }

    /**
     * Check if direct climbing is possible between positions
     */
    private boolean canDirectClimb(BlockPos from, BlockPos to, World world) {
        // Simple check - ensure there are climbable blocks in between
        int minY = Math.min(from.getY(), to.getY());
        int maxY = Math.max(from.getY(), to.getY());

        for (int y = minY; y <= maxY; y++) {
            BlockPos checkPos = new BlockPos(from.getX(), y, from.getZ());
            if (!isClimbableOrAir(world.getBlockState(checkPos))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if block state is climbable or air
     */
    private boolean isClimbableOrAir(net.minecraft.block.BlockState state) {
        return state.isAir() ||
                state.getBlock() instanceof net.minecraft.block.LadderBlock ||
                state.getBlock() instanceof net.minecraft.block.VineBlock ||
                state.getBlock() == net.minecraft.block.Blocks.SCAFFOLDING;
    }

    /**
     * Add safety waypoints for dangerous sections
     */
    private List<PathNode> addSafetyWaypoints(List<PathNode> path, World world) {
        List<PathNode> saferPath = new ArrayList<>();

        for (int i = 0; i < path.size() - 1; i++) {
            PathNode current = path.get(i);
            PathNode next = path.get(i + 1);

            saferPath.add(current);

            // Check if we need safety waypoints between current and next
            if (needsSafetyWaypoint(current, next, world)) {
                PathNode safetyWaypoint = createSafetyWaypoint(current, next, world);
                if (safetyWaypoint != null) {
                    saferPath.add(safetyWaypoint);
                }
            }
        }

        saferPath.add(path.get(path.size() - 1)); // Add final destination
        return saferPath;
    }

    /**
     * Check if safety waypoint is needed between nodes
     */
    private boolean needsSafetyWaypoint(PathNode from, PathNode to, World world) {
        // Add safety waypoint for large gaps
        double distance = from.distanceTo(to);
        if (distance > 4.0) return true;

        // Add safety waypoint near void
        if (from.getPos().getY() < 5 || to.getPos().getY() < 5) return true;

        // Add safety waypoint for big height differences
        int heightDiff = Math.abs(to.getPos().getY() - from.getPos().getY());
        if (heightDiff > PathfinderConfig.MAX_FALL_DISTANCE) return true;

        return false;
    }

    /**
     * Create a safety waypoint between two nodes
     */
    private PathNode createSafetyWaypoint(PathNode from, PathNode to, World world) {
        Vec3d midpoint = from.getCenterVec3d().add(to.getCenterVec3d()).multiply(0.5);
        BlockPos safePos = findSafePosition(BlockPos.ofFloored(midpoint), world);

        if (safePos != null) {
            PathNode safetyNode = new PathNode(safePos);
            safetyNode.setMovementType(from.getMovementType());
            safetyNode.setParent(from);
            return safetyNode;
        }

        return null;
    }

    /**
     * Find safe position near given position
     */
    private BlockPos findSafePosition(BlockPos pos, World world) {
        // Simple implementation: find solid ground nearby
        for (int y = pos.getY(); y >= Math.max(pos.getY() - 10, -64); y--) {
            BlockPos testPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (world.getBlockState(testPos).isSolidBlock(world, testPos) &&
                    world.getBlockState(testPos.up()).isAir()) {
                return testPos.up();
            }
        }

        return null;
    }

    // ==================== PATH CACHING SYSTEM ====================

    /**
     * Find cached path segments that can be reused
     */
    private List<PathNode> findCachedPath(BlockPos start, BlockPos target) {
        // Simple implementation - check for exact matches or nearby segments
        for (CachedPathSegment segment : segmentCache) {
            if (segment.isUsable(start, target)) {
                return new ArrayList<>(segment.getPath());
            }
        }

        return new ArrayList<>();
    }

    /**
     * Cache path segments for future reuse
     */
    private void cachePathSegments(List<PathNode> path) {
        if (path.size() < 10) return; // Only cache longer paths

        // Split path into segments and cache them
        int segmentSize = 20;
        for (int i = 0; i < path.size() - segmentSize; i += segmentSize / 2) {
            int endIndex = Math.min(i + segmentSize, path.size());
            List<PathNode> segment = path.subList(i, endIndex);

            CachedPathSegment cachedSegment = new CachedPathSegment(segment);
            addToCache(cachedSegment);
        }
    }

    /**
     * Add segment to cache with size management
     */
    private void addToCache(CachedPathSegment segment) {
        segmentCache.add(segment);

        // Remove old segments if cache is too large
        if (segmentCache.size() > SEGMENT_CACHE_SIZE) {
            segmentCache.removeIf(cached -> cached.isExpired());

            // If still too large, remove oldest
            while (segmentCache.size() > SEGMENT_CACHE_SIZE) {
                segmentCache.remove(0);
            }
        }
    }

    // ==================== PERFORMANCE TRACKING ====================

    /**
     * Update performance statistics
     */
    private void updatePerformanceStats(long startTime) {
        lastCalculationTime = System.currentTimeMillis() - startTime;

        // Update average using exponential moving average
        if (averageCalculationTime == 0) {
            averageCalculationTime = lastCalculationTime;
        } else {
            averageCalculationTime = averageCalculationTime * 0.9 + lastCalculationTime * 0.1;
        }
    }

    /**
     * Get performance statistics
     */
    public String getPerformanceStats() {
        double successRate = totalCalculations > 0 ?
                (double) successfulPaths / totalCalculations * 100.0 : 0.0;

        return String.format(
                "Pathfinding Performance:\n" +
                        "  Total calculations: %d\n" +
                        "  Successful paths: %d (%.1f%%)\n" +
                        "  Failed paths: %d\n" +
                        "  Last calculation: %d ms\n" +
                        "  Average time: %.1f ms\n" +
                        "  Cached segments: %d",
                totalCalculations, successfulPaths, successRate, failedPaths,
                lastCalculationTime, averageCalculationTime, segmentCache.size()
        );
    }

    /**
     * Cancel current pathfinding operation
     */
    public void cancelPathfinding() {
        cancellationToken.set(true);
    }

    /**
     * Clear cache and reset statistics
     */
    public void reset() {
        segmentCache.clear();
        totalCalculations = 0;
        successfulPaths = 0;
        failedPaths = 0;
        averageCalculationTime = 0;
        lastCalculationTime = 0;
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }

    // ==================== CACHED PATH SEGMENT CLASS ====================

    /**
     * Represents a cached path segment for reuse
     */
    private static class CachedPathSegment {
        private final List<PathNode> path;
        private final BlockPos start;
        private final BlockPos end;
        private final long timestamp;
        private static final long EXPIRY_TIME = 300000; // 5 minutes

        public CachedPathSegment(List<PathNode> path) {
            this.path = new ArrayList<>(path);
            this.start = path.get(0).getPos();
            this.end = path.get(path.size() - 1).getPos();
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isUsable(BlockPos queryStart, BlockPos queryEnd) {
            if (isExpired()) return false;

            // Simple proximity check
            double startDist = Math.sqrt(start.getSquaredDistance(queryStart));
            double endDist = Math.sqrt(end.getSquaredDistance(queryEnd));

            return startDist < 5.0 && endDist < 5.0;
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > EXPIRY_TIME;
        }

        public List<PathNode> getPath() {
            return path;
        }
    }
}