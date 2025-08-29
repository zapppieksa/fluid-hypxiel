package dev.sxmurxy.mre.modules.pathfinding.engine;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
import dev.sxmurxy.mre.modules.pathfinding.movement.MovementType;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced A* pathfinding algorithm with 3D navigation capabilities
 *
 * Features:
 * - High-performance 3D pathfinding optimized for thousands of blocks
 * - Parkour movements for skyblock-style navigation
 * - Climbing support (ladders, vines, scaffolding)
 * - Hazard avoidance (void, lava, dangerous blocks)
 * - Cached segments for performance
 * - Heuristic-based A* with optimizations
 */
public class AStarPathfinder {

    private final World world;
    private final PathfinderConfig config;
    private final AtomicBoolean cancellationToken;

    // Search state
    private final Map<BlockPos, PathNode> nodeCache = new ConcurrentHashMap<>();
    private final Set<BlockPos> closedSet = new HashSet<>();
    private final PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparing(PathNode::getFCost));

    // Performance tracking
    private static final int MAX_ITERATIONS = 25000; // Increased for long distance
    private static final int CACHE_CLEANUP_INTERVAL = 3000;
    private int iterations = 0;
    private long startTime = 0;

    // Movement directions for comprehensive pathfinding
    private static final int[][] CARDINAL_DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int[][] DIAGONAL_DIRECTIONS = {
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}
    };

    private static final int[][] JUMP_DIRECTIONS = {
            // 1-block jumps (standard parkour)
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, 1, 1}, {-1, 1, 1}, {1, 1, -1}, {-1, 1, -1},
            // 2-block jumps (advanced parkour)
            {2, 1, 0}, {-2, 1, 0}, {0, 1, 2}, {0, 1, -2},
            {2, 1, 1}, {-2, 1, 1}, {1, 1, 2}, {-1, 1, 2}
    };

    // Climbing directions
    private static final int[][] CLIMB_DIRECTIONS = {
            {0, 1, 0}, {0, 2, 0}, {0, 3, 0}, // Vertical climbing
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1} // Diagonal climbing
    };

    public AStarPathfinder(World world, PathfinderConfig config, AtomicBoolean cancellationToken) {
        this.world = world;
        this.config = config;
        this.cancellationToken = cancellationToken;
    }

    /**
     * Find path from start to goal using optimized A* algorithm
     */
    public List<PathNode> findPath(BlockPos start, BlockPos goal) {
        if (world == null || start.equals(goal)) {
            return Collections.emptyList();
        }

        startTime = System.currentTimeMillis();
        clearSearchState();

        PathNode startNode = new PathNode(start);
        startNode.setGCost(0);
        startNode.setHCost(calculateHeuristic(start, goal));
        startNode.setMovementType(MovementType.WALK);

        openSet.add(startNode);
        nodeCache.put(start, startNode);

        PathNode goalNode = null;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            // Check for cancellation
            if (cancellationToken.get()) {
                return Collections.emptyList();
            }

            // Get node with lowest f-cost
            PathNode current = openSet.poll();
            closedSet.add(current.getPos());

            // Check if we've reached the goal
            if (current.getPos().equals(goal) || isNearGoal(current.getPos(), goal)) {
                goalNode = current;
                break;
            }

            // Explore neighbors with comprehensive movement options
            List<PathNode> neighbors = generateNeighbors(current, goal);
            for (PathNode neighbor : neighbors) {
                if (closedSet.contains(neighbor.getPos()) || cancellationToken.get()) {
                    continue;
                }

                double tentativeG = current.getGCost() + calculateMovementCost(current, neighbor);

                PathNode existingNode = nodeCache.get(neighbor.getPos());
                if (existingNode == null) {
                    // New node discovered
                    neighbor.setGCost(tentativeG);
                    neighbor.setHCost(calculateHeuristic(neighbor.getPos(), goal));
                    neighbor.setParent(current);

                    openSet.add(neighbor);
                    nodeCache.put(neighbor.getPos(), neighbor);
                } else if (tentativeG < existingNode.getGCost()) {
                    // Better path found to existing node
                    openSet.remove(existingNode);
                    existingNode.setGCost(tentativeG);
                    existingNode.setParent(current);
                    openSet.add(existingNode);
                }
            }

            iterations++;

            // Periodic maintenance for long-distance pathfinding
            if (iterations % CACHE_CLEANUP_INTERVAL == 0) {
                performMaintenance();
            }

            // Time limit check
            if (System.currentTimeMillis() - startTime > PathfinderConfig.PATH_CALCULATION_TIMEOUT_MS) {
                System.out.println("Pathfinding timeout after " + iterations + " iterations");
                break;
            }
        }

        // Reconstruct and return path
        if (goalNode != null) {
            List<PathNode> path = reconstructPath(goalNode);
            System.out.println("Path found in " + iterations + " iterations, " +
                    (System.currentTimeMillis() - startTime) + "ms, distance: " +
                    String.format("%.1f", calculatePathLength(path)));
            return path;
        }

        System.out.println("No path found after " + iterations + " iterations");
        return Collections.emptyList();
    }

    /**
     * Generate comprehensive neighbor nodes with all movement types
     */
    private List<PathNode> generateNeighbors(PathNode current, BlockPos goal) {
        List<PathNode> neighbors = new ArrayList<>();
        BlockPos pos = current.getPos();

        // 1. Standard ground movement (walking)
        addGroundMovement(neighbors, pos);

        // 2. Vertical movements (climbing, jumping, falling)
        addVerticalMovement(neighbors, pos, goal);

        // 3. Parkour movements (for skyblock islands)
        if (shouldAttemptParkour(pos, goal)) {
            addParkourMovement(neighbors, pos, goal);
        }

        // 4. Special movement optimizations
        addOptimizedMovements(neighbors, pos, goal);

        return neighbors;
    }

    /**
     * Add standard ground-level movement options
     */
    private void addGroundMovement(List<PathNode> neighbors, BlockPos pos) {
        // Cardinal directions
        for (int[] dir : CARDINAL_DIRECTIONS) {
            BlockPos newPos = pos.add(dir[0], dir[1], dir[2]);
            if (isWalkable(newPos)) {
                PathNode node = new PathNode(newPos);
                node.setMovementType(MovementType.WALK);
                neighbors.add(node);
            }
        }

        // Diagonal movement (if not blocked)
        for (int[] dir : DIAGONAL_DIRECTIONS) {
            BlockPos newPos = pos.add(dir[0], dir[1], dir[2]);
            if (isWalkable(newPos) && isDiagonalClear(pos, newPos)) {
                PathNode node = new PathNode(newPos);
                node.setMovementType(MovementType.DIAGONAL);
                neighbors.add(node);
            }
        }
    }

    /**
     * Add vertical movement options (climbing, jumping, falling)
     */
    private void addVerticalMovement(List<PathNode> neighbors, BlockPos pos, BlockPos goal) {
        // Climbing movements
        for (int[] dir : CLIMB_DIRECTIONS) {
            BlockPos climbPos = pos.add(dir[0], dir[1], dir[2]);
            if (canClimbTo(pos, climbPos)) {
                PathNode node = new PathNode(climbPos);
                node.setMovementType(MovementType.CLIMB);
                neighbors.add(node);
            }
        }

        // Jumping movements
        for (int[] dir : JUMP_DIRECTIONS) {
            BlockPos jumpPos = pos.add(dir[0], dir[1], dir[2]);
            if (canJumpTo(pos, jumpPos)) {
                PathNode node = new PathNode(jumpPos);
                node.setMovementType(MovementType.JUMP);
                neighbors.add(node);
            }
        }

        // Falling with safety checks
        BlockPos fallPos = findSafeFallPosition(pos);
        if (fallPos != null && !fallPos.equals(pos)) {
            PathNode node = new PathNode(fallPos);
            node.setMovementType(MovementType.FALL);
            neighbors.add(node);
        }
    }

    /**
     * Add parkour movement options for skyblock-style navigation
     */
    private void addParkourMovement(List<PathNode> neighbors, BlockPos pos, BlockPos goal) {
        // Extended jumps for skyblock islands
        int[][] extendedJumps = {
                {3, 0, 0}, {-3, 0, 0}, {0, 0, 3}, {0, 0, -3},
                {4, 0, 0}, {-4, 0, 0}, {0, 0, 4}, {0, 0, -4},
                {3, 1, 0}, {-3, 1, 0}, {0, 1, 3}, {0, 1, -3},
                {4, 1, 0}, {-4, 1, 0}, {0, 1, 4}, {0, 1, -4}
        };

        for (int[] jump : extendedJumps) {
            BlockPos jumpPos = pos.add(jump[0], jump[1], jump[2]);
            if (canParkourTo(pos, jumpPos)) {
                PathNode node = new PathNode(jumpPos);
                node.setMovementType(MovementType.PARKOUR);
                node.setSpecialMove(true);
                neighbors.add(node);
            }
        }
    }

    /**
     * Add optimized movements for long-distance pathfinding
     */
    private void addOptimizedMovements(List<PathNode> neighbors, BlockPos pos, BlockPos goal) {
        double distanceToGoal = Math.sqrt(pos.getSquaredDistance(goal));

        // Long-distance optimization: larger steps when far from goal
        if (distanceToGoal > 50) {
            int[][] longSteps = {
                    {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},
                    {2, 0, 1}, {-2, 0, 1}, {1, 0, 2}, {-1, 0, 2}
            };

            for (int[] step : longSteps) {
                BlockPos stepPos = pos.add(step[0], step[1], step[2]);
                if (isWalkable(stepPos) && isPathClear(pos, stepPos)) {
                    PathNode node = new PathNode(stepPos);
                    node.setMovementType(MovementType.WALK);
                    neighbors.add(node);
                }
            }
        }
    }

    // ==================== MOVEMENT VALIDATION METHODS ====================

    /**
     * Check if position is walkable
     */
    private boolean isWalkable(BlockPos pos) {
<<<<<<< Updated upstream
        // Bounds check
        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopY(Heightmap.Type.WORLD_SURFACE,0,0)) {
            return false;
        }
=======
        if (isDangerous(pos)) return false;
>>>>>>> Stashed changes

        BlockState ground = world.getBlockState(pos.down());
        BlockState atPos = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.up());

        // Must have solid ground beneath
        if (!isSolid(ground) && !isClimbable(ground)) {
            return false;
        }

        // Must have air or passable blocks at head level
        return isPassable(atPos) && isPassable(above);
    }

    /**
     * Check if diagonal movement is clear
     */
    private boolean isDiagonalClear(BlockPos from, BlockPos to) {
        BlockPos corner1 = new BlockPos(from.getX(), from.getY(), to.getZ());
        BlockPos corner2 = new BlockPos(to.getX(), from.getY(), from.getZ());
        return isPassable(world.getBlockState(corner1)) && isPassable(world.getBlockState(corner2));
    }

    /**
     * Check if can climb to target position
     */
    private boolean canClimbTo(BlockPos from, BlockPos to) {
        if (isDangerous(to)) return false;

        // Check for climbable blocks (ladders, vines, scaffolding)
        BlockState fromState = world.getBlockState(from);
        BlockState toState = world.getBlockState(to);

        if (isClimbable(fromState) || isClimbable(toState)) {
            return isPassable(world.getBlockState(to)) && isPassable(world.getBlockState(to.up()));
        }

        // Check for blocks we can climb on
        BlockState supportState = world.getBlockState(to.down());
        return isSolid(supportState) && isPassable(world.getBlockState(to)) && isPassable(world.getBlockState(to.up()));
    }

    /**
     * Check if can jump to target position
     */
    private boolean canJumpTo(BlockPos from, BlockPos to) {
        if (isDangerous(to)) return false;

        int heightDiff = to.getY() - from.getY();
        if (heightDiff > 1) return false; // Can't jump higher than 1 block

        double horizontalDist = Math.sqrt((to.getX() - from.getX()) * (to.getX() - from.getX()) +
                (to.getZ() - from.getZ()) * (to.getZ() - from.getZ()));

        // Check jump feasibility
        if (horizontalDist <= 1.0) return true; // Easy jump
        if (horizontalDist <= 4.0 && heightDiff <= 0) return true; // Running jump

        // Check for landing area
        BlockState ground = world.getBlockState(to.down());
        BlockState atPos = world.getBlockState(to);
        BlockState above = world.getBlockState(to.up());

        return isSolid(ground) && isPassable(atPos) && isPassable(above);
    }

    /**
     * Check if can perform parkour movement to target
     */
    private boolean canParkourTo(BlockPos from, BlockPos to) {
        if (isDangerous(to)) return false;

        double horizontalDist = Math.sqrt((to.getX() - from.getX()) * (to.getX() - from.getX()) +
                (to.getZ() - from.getZ()) * (to.getZ() - from.getZ()));

        // Only allow parkour for significant gaps
        if (horizontalDist < 3.0) return false;

        // Check for safe landing
        BlockState ground = world.getBlockState(to.down());
        if (!isSolid(ground)) return false;

        // Check path is clear for jumping
        return isPathClear(from, to) && isPassable(world.getBlockState(to)) && isPassable(world.getBlockState(to.up()));
    }

    /**
     * Find safe fall position from current location
     */
    private BlockPos findSafeFallPosition(BlockPos from) {
        int maxFall = PathfinderConfig.MAX_FALL_DISTANCE;

        for (int y = 1; y <= maxFall; y++) {
            BlockPos checkPos = from.down(y);
            BlockState ground = world.getBlockState(checkPos);

            if (isSolid(ground)) {
                BlockPos landingPos = checkPos.up();
                if (!isDangerous(landingPos) && isPassable(world.getBlockState(landingPos))) {
                    return landingPos;
                }
            }
        }

        return null;
    }

    /**
     * Check if path between two positions is clear
     */
    private boolean isPathClear(BlockPos from, BlockPos to) {
        Vec3d fromVec = Vec3d.ofCenter(from);
        Vec3d toVec = Vec3d.ofCenter(to);
        Vec3d direction = toVec.subtract(fromVec).normalize();

        double distance = fromVec.distanceTo(toVec);
        int steps = (int) Math.ceil(distance);

        for (int i = 1; i < steps; i++) {
            Vec3d checkPos = fromVec.add(direction.multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);

            if (!isPassable(world.getBlockState(blockPos)) ||
                    !isPassable(world.getBlockState(blockPos.up()))) {
                return false;
            }
        }

        return true;
    }

    // ==================== BLOCK STATE CHECKING METHODS ====================

    /**
     * Check if should attempt parkour movements
     */
    private boolean shouldAttemptParkour(BlockPos pos, BlockPos goal) {
        double distance = Math.sqrt(pos.getSquaredDistance(goal));
        return distance > 10 && distance < 1000; // Use parkour for medium-long distances
    }

    /**
     * Check if diagonal movement is clear
     */
    private boolean isDiagonalClear(BlockPos from, BlockPos to) {
        BlockPos corner1 = new BlockPos(from.getX(), from.getY(), to.getZ());
        BlockPos corner2 = new BlockPos(to.getX(), from.getY(), from.getZ());
        return isPassable(world.getBlockState(corner1)) && isPassable(world.getBlockState(corner2));
    }

    /**
     * Find safe fall position from current location
     */
    private BlockPos findSafeFallPosition(BlockPos from) {
        int maxFall = PathfinderConfig.MAX_FALL_DISTANCE;

        for (int y = 1; y <= maxFall; y++) {
            BlockPos checkPos = from.down(y);
            BlockState ground = world.getBlockState(checkPos);

            if (isSolid(ground)) {
                BlockPos landingPos = checkPos.up();
                if (!isDangerous(landingPos) && isPassable(world.getBlockState(landingPos))) {
                    return landingPos;
                }
            }
        }

        return null;
    }

    /**
     * Check if path between two positions is clear
     */
    private boolean isPathClear(BlockPos from, BlockPos to) {
        Vec3d fromVec = Vec3d.ofCenter(from);
        Vec3d toVec = Vec3d.ofCenter(to);
        Vec3d direction = toVec.subtract(fromVec).normalize();

        double distance = fromVec.distanceTo(toVec);
        int steps = (int) Math.ceil(distance);

        for (int i = 1; i < steps; i++) {
            Vec3d checkPos = fromVec.add(direction.multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);

            if (!isPassable(world.getBlockState(blockPos)) ||
                    !isPassable(world.getBlockState(blockPos.up()))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if block state is passable
     */
    private boolean isPassable(BlockState state) {
        Block block = state.getBlock();
        return state.isAir() ||
                block == Blocks.WATER ||
                isClimbable(state) ||
                block instanceof DoorBlock ||
                block instanceof FenceGateBlock ||
                block instanceof CarpetBlock ||
                block instanceof FlowerBlock ||
                block instanceof SaplingBlock ||
                state.isIn(BlockTags.SIGNS);
    }

    /**
     * Check if block state is climbable
     */
    private boolean isClimbable(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.LADDER ||
                block == Blocks.VINE ||
                block == Blocks.SCAFFOLDING ||
                block instanceof VineBlock;
    }

    /**
     * Check if position is dangerous (hazards)
     */
    private boolean isDangerous(BlockPos pos) {
        // Check for void (Y < 0 in overworld)
        if (pos.getY() < -50) return true;

        // Check surrounding blocks for hazards
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.LAVA ||
                block == Blocks.FIRE ||
                block == Blocks.MAGMA_BLOCK ||
                block == Blocks.CACTUS ||
                block == Blocks.SWEET_BERRY_BUSH ||
                block == Blocks.WITHER_ROSE) {
            return true;
        }

        return false;
    }

    /**
     * Check if player has water breathing (for water hazard assessment)
     */
    private boolean hasWaterBreathing() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        return mc.player.hasStatusEffect(StatusEffects.WATER_BREATHING) ||
                mc.player.hasStatusEffect(StatusEffects.CONDUIT_POWER);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear search state for new pathfinding
     */
    private void clearSearchState() {
        nodeCache.clear();
        closedSet.clear();
        openSet.clear();
        iterations = 0;
    }

    /**
     * Perform maintenance during long searches
     */
    private void performMaintenance() {
        // Remove old nodes from cache to prevent memory issues
        if (nodeCache.size() > PathfinderConfig.MAX_CACHED_NODES) {
            nodeCache.clear();
            System.out.println("Cleared node cache to prevent memory issues");
        }

        System.gc(); // Suggest garbage collection for long-running pathfinding
    }

    /**
     * Determine movement type between two positions
     */
    private MovementType determineMovementType(BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());

        if (dy > 1) return MovementType.CLIMB;
        if (dy < -1) return MovementType.FALL;
        if (dy == 1) return MovementType.JUMP;
        if (dx + dz > 1) {
            if (dx + dz > 3) return MovementType.PARKOUR;
            return MovementType.DIAGONAL;
        }

        return MovementType.WALK;
    }

    /**
     * Check if can climb to target position
     */
    private boolean canClimbTo(BlockPos from, BlockPos to) {
        if (isDangerous(to)) return false;

        // Check for climbable blocks (ladders, vines, scaffolding)
        BlockState fromState = world.getBlockState(from);
        BlockState toState = world.getBlockState(to);

        if (isClimbable(fromState) || isClimbable(toState)) {
            return isPassable(world.getBlockState(to)) && isPassable(world.getBlockState(to.up()));
        }

        // Check for blocks we can climb on
        BlockState supportState = world.getBlockState(to.down());
        return isSolid(supportState) && isPassable(world.getBlockState(to)) && isPassable(world.getBlockState(to.up()));
    }

    /**
     * Check if block state is climbable
     */
    private boolean isClimbable(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.LADDER ||
                block == Blocks.VINE ||
                block == Blocks.SCAFFOLDING;
    }

    /**
     * Check if block state is passable
     */
    private boolean isPassable(BlockState state) {
        Block block = state.getBlock();
        return state.isAir() ||
                block == Blocks.WATER ||
                isClimbable(state) ||
                block instanceof DoorBlock ||
                block instanceof FenceGateBlock ||
                block instanceof CarpetBlock;
    }

    /**
     * Check if block state is solid
     */
    private boolean isSolid(BlockState state) {
        Block block = state.getBlock();
        return !state.isAir() &&
                block != Blocks.WATER &&
                block != Blocks.LAVA &&
                state.isSolidBlock(world, BlockPos.ORIGIN);
    }

    /**
     * Check if near enough to goal
     */
    private boolean isNearGoal(BlockPos pos, BlockPos goal) {
        return pos.getSquaredDistance(goal) <= 2.0;
    }

    /**
     * Calculate heuristic distance (A* h-cost) with optimizations
     */
    private double calculateHeuristic(BlockPos current, BlockPos goal) {
        double dx = Math.abs(current.getX() - goal.getX());
        double dy = Math.abs(current.getY() - goal.getY());
        double dz = Math.abs(current.getZ() - goal.getZ());

        // 3D Euclidean distance as base
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Height penalty (climbing/falling is expensive)
        double heightPenalty = dy * 0.3;

        // Add penalty for very low positions (void avoidance)
        if (current.getY() < 10) {
            heightPenalty += (10 - current.getY()) * 2.0;
        }

        // Distance-based optimizations
        double optimizationFactor = 1.0;
        if (distance > 100) {
            optimizationFactor = 0.9; // Slightly favor long-distance paths
        }

        return (distance + heightPenalty) * optimizationFactor;
    }

    /**
     * Calculate movement cost between nodes
     */
    private double calculateMovementCost(PathNode from, PathNode to) {
        BlockPos fromPos = from.getPos();
        BlockPos toPos = to.getPos();

        // Base distance cost
        double distance = Math.sqrt(fromPos.getSquaredDistance(toPos));

        // Movement type cost multipliers
        MovementType movementType = determineMovementType(fromPos, toPos);
        double multiplier = switch (movementType) {
            case WALK -> 1.0;
            case DIAGONAL -> 1.414; // √2
            case JUMP -> 1.5;
            case CLIMB -> 2.2;
            case FALL -> 0.8; // Falling is faster
            case PARKOUR -> 3.0; // Expensive but sometimes necessary
        };

        // Environmental penalties
        if (isDangerous(toPos)) {
            multiplier *= 5.0; // Heavily penalize dangerous moves
        }

        // Encourage staying at safe heights
        if (toPos.getY() < 5) {
            multiplier *= 1.5;
        }

        return distance * multiplier;
    }

    /**
     * Reconstruct path from goal node to start
     */
    private List<PathNode> reconstructPath(PathNode goalNode) {
        List<PathNode> path = new ArrayList<>();
        PathNode current = goalNode;

        while (current != null) {
            path.add(current);
            current = current.getParent();
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Determine movement type between two positions
     */
    private MovementType determineMovementType(BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());

        if (dy > 1) return MovementType.CLIMB;
        if (dy < -1) return MovementType.FALL;
        if (dy == 1) return MovementType.JUMP;
        if (dx + dz > 1) {
            if (dx + dz > 3) return MovementType.PARKOUR;
            return MovementType.DIAGONAL;
        }

        return MovementType.WALK;
    }

    /**
     * Calculate total path length for statistics
     */
    private double calculatePathLength(List<PathNode> path) {
        if (path.size() < 2) return 0.0;

        double totalLength = 0.0;
        for (int i = 1; i < path.size(); i++) {
            BlockPos prev = path.get(i - 1).getPos();
            BlockPos curr = path.get(i).getPos();
            totalLength += Math.sqrt(prev.getSquaredDistance(curr));
        }

        return totalLength;
    }
}