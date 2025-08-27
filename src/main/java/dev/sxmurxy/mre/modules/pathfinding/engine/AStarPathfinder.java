package dev.sxmurxy.mre.modules.pathfinding.engine;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
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
 * - 3D movement with jumping, climbing, falling
 * - Parkour movements for gap crossing
 * - Hazard avoidance (void, lava, damage blocks)
 * - Performance optimizations for long-distance pathfinding
 * - Node caching and memory management
 * - Cancellation support for responsive UI
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
    private static final int MAX_ITERATIONS = 15000;
    private static final int CACHE_CLEANUP_INTERVAL = 2000;
    private int iterations = 0;
    private long startTime = 0;

    // Movement directions for pathfinding
    private static final int[][] CARDINAL_DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int[][] DIAGONAL_DIRECTIONS = {
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}
    };

    private static final int[][] JUMP_DIRECTIONS = {
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, 1, 1}, {-1, 1, 1}, {1, 1, -1}, {-1, 1, -1}
    };

    public AStarPathfinder(World world, PathfinderConfig config, AtomicBoolean cancellationToken) {
        this.world = world;
        this.config = config;
        this.cancellationToken = cancellationToken;
    }

    /**
     * Find path from start to goal using A* algorithm with 3D movement
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

            // Explore neighbors
            List<PathNode> neighbors = generateNeighbors(current, goal);
            for (PathNode neighbor : neighbors) {
                if (closedSet.contains(neighbor.getPos()) || cancellationToken.get()) {
                    continue;
                }

                double tentativeG = current.getGCost() + calculateMovementCost(current, neighbor);

                PathNode existingNode = nodeCache.get(neighbor.getPos());
                if (existingNode == null) {
                    // New node
                    neighbor.setGCost(tentativeG);
                    neighbor.setHCost(calculateHeuristic(neighbor.getPos(), goal));
                    neighbor.setParent(current);
                    neighbor.setMovementType(determineMovementType(current.getPos(), neighbor.getPos()));

                    openSet.add(neighbor);
                    nodeCache.put(neighbor.getPos(), neighbor);
                } else if (tentativeG < existingNode.getGCost()) {
                    // Better path found
                    openSet.remove(existingNode);
                    existingNode.setGCost(tentativeG);
                    existingNode.setParent(current);
                    existingNode.setMovementType(determineMovementType(current.getPos(), neighbor.getPos()));
                    openSet.add(existingNode);
                }
            }

            iterations++;

            // Periodic maintenance
            if (iterations % CACHE_CLEANUP_INTERVAL == 0) {
                performMaintenance();
            }

            // Time limit check
            if (System.currentTimeMillis() - startTime > config.PATH_CALCULATION_TIMEOUT_MS) {
                System.out.println("Pathfinding timeout after " + iterations + " iterations");
                break;
            }
        }

        // Reconstruct and return path
        if (goalNode != null) {
            List<PathNode> path = reconstructPath(goalNode);
            System.out.println("Path found in " + iterations + " iterations, " +
                    (System.currentTimeMillis() - startTime) + "ms");
            return path;
        }

        System.out.println("No path found after " + iterations + " iterations");
        return Collections.emptyList();
    }

    /**
     * Generate valid neighbor nodes for current position
     */
    private List<PathNode> generateNeighbors(PathNode current, BlockPos goal) {
        List<PathNode> neighbors = new ArrayList<>();
        BlockPos pos = current.getPos();

        // Standard ground movement
        addGroundMovement(neighbors, pos);

        // Vertical movements
        addVerticalMovement(neighbors, pos, goal);

        // Parkour movements (if enabled and close to goal)
        if (shouldAttemptParkour(pos, goal)) {
            addParkourMovement(neighbors, pos, goal);
        }

        return neighbors;
    }

    /**
     * Add ground-level movement options
     */
    private void addGroundMovement(List<PathNode> neighbors, BlockPos pos) {
        // Cardinal directions
        for (int[] dir : CARDINAL_DIRECTIONS) {
            BlockPos newPos = pos.add(dir[0], dir[1], dir[2]);
            if (isWalkable(newPos)) {
                neighbors.add(new PathNode(newPos));
            }
        }

        // Diagonal movement
        for (int[] dir : DIAGONAL_DIRECTIONS) {
            BlockPos newPos = pos.add(dir[0], dir[1], dir[2]);
            if (isWalkable(newPos) && isDiagonalClear(pos, newPos)) {
                neighbors.add(new PathNode(newPos));
            }
        }
    }

    /**
     * Add vertical movement options (climbing, jumping, falling)
     */
    private void addVerticalMovement(List<PathNode> neighbors, BlockPos pos, BlockPos goal) {
        // Climbing up
        if (canClimbUp(pos)) {
            neighbors.add(new PathNode(pos.up()));
            // Multi-level climbing for efficiency
            if (canClimbUp(pos.up())) {
                neighbors.add(new PathNode(pos.up(2)));
            }
        }

        // Jumping
        for (int[] dir : JUMP_DIRECTIONS) {
            BlockPos jumpPos = pos.add(dir[0], dir[1], dir[2]);
            if (canJumpTo(pos, jumpPos)) {
                neighbors.add(new PathNode(jumpPos));
            }
        }

        // Falling
        BlockPos fallPos = findSafeFallPosition(pos);
        if (fallPos != null && !fallPos.equals(pos)) {
            neighbors.add(new PathNode(fallPos));
        }
    }

    /**
     * Add parkour movement options for gap crossing
     */
    private void addParkourMovement(List<PathNode> neighbors, BlockPos pos, BlockPos goal) {
        int[][] parkourMoves = {
                {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},  // 2-block jumps
                {3, 0, 0}, {-3, 0, 0}, {0, 0, 3}, {0, 0, -3},  // 3-block jumps
                {2, 1, 0}, {-2, 1, 0}, {0, 1, 2}, {0, 1, -2},  // Up-jumps
                {4, -1, 0}, {-4, -1, 0}, {0, -1, 4}, {0, -1, -4} // Down-jumps
        };

        for (int[] move : parkourMoves) {
            BlockPos parkourPos = pos.add(move[0], move[1], move[2]);
            if (canParkourTo(pos, parkourPos)) {
                neighbors.add(new PathNode(parkourPos));
            }
        }
    }

    /**
     * Check if position is walkable
     */
    private boolean isWalkable(BlockPos pos) {
        // Bounds check
        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopY(Heightmap.Type.WORLD_SURFACE,0,0)) {
            return false;
        }

        // Must have solid ground
        BlockState ground = world.getBlockState(pos.down());
        if (!isSolid(ground) && !isClimbable(pos.down())) {
            return false;
        }

        // Must have clear space for player (2 blocks high)
        if (!isClear(pos) || !isClear(pos.up())) {
            return false;
        }

        // Avoid dangerous positions
        return !isDangerous(pos);
    }

    /**
     * Check if can jump to target position
     */
    private boolean canJumpTo(BlockPos from, BlockPos to) {
        // Check horizontal distance (max 4 blocks for sprint-jump)
        double horizontalDist = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) +
                Math.pow(to.getZ() - from.getZ(), 2));
        if (horizontalDist > 4.2) return false;

        // Check vertical distance (max 1.25 blocks jump height)
        int verticalDist = to.getY() - from.getY();
        if (verticalDist > 1 || verticalDist < -3) return false;

        // Check if landing position is safe
        if (!isWalkable(to)) return false;

        // Check path is clear
        return isJumpPathClear(from, to);
    }

    /**
     * Check if can parkour to target (longer jumps, gaps)
     */
    private boolean canParkourTo(BlockPos from, BlockPos to) {
        // Check distance limits
        double horizontalDist = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) +
                Math.pow(to.getZ() - from.getZ(), 2));
        if (horizontalDist > 5.0 || horizontalDist < 2.0) return false;

        // Check landing area
        if (!isWalkable(to)) return false;

        // Ensure there's actually a gap to jump over
        Vec3d center = Vec3d.ofCenter(from).lerp(Vec3d.ofCenter(to), 0.5);
        BlockPos midPos = BlockPos.ofFloored(center);
        if (isSolid(world.getBlockState(midPos.down()))) {
            return false; // No gap to jump
        }

        // Check jump path is clear
        return isJumpPathClear(from, to);
    }

    /**
     * Check if can climb up from position
     */
    private boolean canClimbUp(BlockPos pos) {
        BlockPos upPos = pos.up();

        // Must have climbable block or be able to step up
        if (isClimbable(upPos) || isClimbable(pos)) {
            return isClear(upPos) && isClear(upPos.up());
        }

        // Check for step-up blocks (stairs, slabs)
        BlockState upState = world.getBlockState(upPos);
        return isClear(upPos) && isClear(upPos.up()) &&
                isSolid(world.getBlockState(upPos.down()));
    }

    /**
     * Find safe position to fall to
     */
    private BlockPos findSafeFallPosition(BlockPos pos) {
        for (int y = pos.getY() - 1; y >= Math.max(pos.getY() - config.MAX_FALL_DISTANCE, world.getBottomY()); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());

            if (isSolid(world.getBlockState(checkPos))) {
                BlockPos landingPos = checkPos.up();
                if (isWalkable(landingPos)) {
                    return landingPos;
                }
                break;
            }

            if (isDangerous(checkPos)) {
                break;
            }
        }
        return null;
    }

    /**
     * Check if diagonal movement path is clear
     */
    private boolean isDiagonalClear(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        // Check both adjacent cardinal directions are clear
        BlockPos side1 = from.add(dx, 0, 0);
        BlockPos side2 = from.add(0, 0, dz);

        return isClear(side1) && isClear(side2) &&
                isClear(side1.up()) && isClear(side2.up());
    }

    /**
     * Check if jump path is clear of obstacles
     */
    private boolean isJumpPathClear(Vec3d from, Vec3d to) {
        Vec3d direction = to.subtract(from);
        int steps = Math.max(2, (int) direction.length());
        direction = direction.normalize();

        for (int i = 1; i < steps; i++) {
            Vec3d checkPos = from.add(direction.multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);

            // Check head clearance
            if (!isClear(blockPos) || !isClear(blockPos.up())) {
                return false;
            }
        }
        return true;
    }

    private boolean isJumpPathClear(BlockPos from, BlockPos to) {
        return isJumpPathClear(Vec3d.ofCenter(from), Vec3d.ofCenter(to));
    }

    /**
     * Check if block position is clear for movement
     */
    private boolean isClear(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || !state.blocksMovement();
    }

    /**
     * Check if block is solid for standing on
     */
    private boolean isSolid(BlockState state) {
        return state.isSolidBlock(world, BlockPos.ORIGIN) && !state.isAir();
    }

    /**
     * Check if position/block is climbable
     */
    private boolean isClimbable(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        return block instanceof LadderBlock ||
                block instanceof VineBlock ||
                block instanceof ScaffoldingBlock ||
                block instanceof TwistingVinesBlock ||
                block instanceof WeepingVinesBlock;
    }

    /**
     * Check if position is dangerous
     */
    private boolean isDangerous(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Immediate dangers
        if (block == Blocks.LAVA ||
                block == Blocks.FIRE ||
                block == Blocks.SOUL_FIRE ||
                block == Blocks.CACTUS ||
                block == Blocks.SWEET_BERRY_BUSH ||
                block == Blocks.WITHER_ROSE ||
                block == Blocks.CAMPFIRE ||
                block == Blocks.SOUL_CAMPFIRE) {
            return true;
        }

        // Water check (dangerous if no water breathing)
        if (block == Blocks.WATER && !hasWaterBreathing()) {
            return true;
        }

        // Void check
        if (pos.getY() < world.getBottomY() + 5) {
            return true;
        }

        // Magma block check
        if (block == Blocks.MAGMA_BLOCK) {
            return true;
        }

        return false;
    }

    /**
     * Check if should attempt parkour based on distance to goal
     */
    private boolean shouldAttemptParkour(BlockPos pos, BlockPos goal) {
        return pos.getSquaredDistance(goal) < 64 * 64; // Within 64 blocks
    }

    /**
     * Check if near enough to goal
     */
    private boolean isNearGoal(BlockPos pos, BlockPos goal) {
        return pos.getSquaredDistance(goal) <= 2.0;
    }

    /**
     * Calculate heuristic distance (A* h-cost)
     */
    private double calculateHeuristic(BlockPos current, BlockPos goal) {
        double dx = Math.abs(current.getX() - goal.getX());
        double dy = Math.abs(current.getY() - goal.getY());
        double dz = Math.abs(current.getZ() - goal.getZ());

        // 3D Euclidean distance
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Penalize height differences (climbing/falling is expensive)
        double heightPenalty = dy * 0.5;

        // Add obstacle penalty for walls
        double obstaclePenalty = 0;
        if (isObstructed(current, goal)) {
            obstaclePenalty = 5.0;
        }

        return distance + heightPenalty + obstaclePenalty;
    }

    /**
     * Check if path is obstructed by walls
     */
    private boolean isObstructed(BlockPos from, BlockPos to) {
        Vec3d direction = Vec3d.of(to.subtract(from)).normalize();

        // Check a few blocks ahead
        for (int i = 1; i <= 3; i++) {
            BlockPos checkPos = from.add((int)(direction.x * i), 0, (int)(direction.z * i));
            if (isSolid(world.getBlockState(checkPos))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate movement cost between nodes
     */
    private double calculateMovementCost(PathNode from, PathNode to) {
        BlockPos fromPos = from.getPos();
        BlockPos toPos = to.getPos();

        // Base distance cost
        double distance = Math.sqrt(fromPos.getSquaredDistance(toPos));

        // Movement type multipliers
        MovementType movementType = determineMovementType(fromPos, toPos);
        double multiplier = switch (movementType) {
            case WALK -> 1.0;
            case DIAGONAL -> 1.414; // √2
            case JUMP -> 1.3;
            case CLIMB -> 1.8;
            case FALL -> 0.9;
            case PARKOUR -> 2.5;
        };

        // Environmental penalties
        if (isDangerous(toPos)) {
            multiplier *= 3.0; // Heavily penalize dangerous moves
        }

        if (toPos.getY() < 10) {
            multiplier *= 1.2; // Slightly penalize low altitude
        }

        return distance * multiplier;
    }

    /**
     * Determine type of movement between positions
     */
    private MovementType determineMovementType(BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());

        if (dy > 0) {
            if (dx > 1 || dz > 1 || dy > 1) return MovementType.PARKOUR;
            if (isClimbable(to) || isClimbable(from)) return MovementType.CLIMB;
            return MovementType.JUMP;
        } else if (dy < 0) {
            return MovementType.FALL;
        } else if (dx > 0 && dz > 0) {
            return MovementType.DIAGONAL;
        }
        return MovementType.WALK;
    }

    /**
     * Check if player has water breathing effect
     */
    private boolean hasWaterBreathing() {
        return MinecraftClient.getInstance().player != null &&
                MinecraftClient.getInstance().player.hasStatusEffect(StatusEffects.WATER_BREATHING);
    }

    /**
     * Reconstruct path from goal back to start
     */
    private List<PathNode> reconstructPath(PathNode goal) {
        List<PathNode> path = new ArrayList<>();
        PathNode current = goal;

        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }

        return path;
    }

    /**
     * Clear search state for new pathfinding
     */
    private void clearSearchState() {
        openSet.clear();
        closedSet.clear();
        iterations = 0;

        // Limit cache size for memory management
        if (nodeCache.size() > config.MAX_CACHED_NODES) {
            nodeCache.clear();
        }
    }

    /**
     * Perform maintenance during search
     */
    private void performMaintenance() {
        // Clean up old cache entries
        if (nodeCache.size() > config.MAX_CACHED_NODES * 1.5) {
            // Remove random entries from closed set
            Iterator<Map.Entry<BlockPos, PathNode>> it = nodeCache.entrySet().iterator();
            while (it.hasNext() && nodeCache.size() > config.MAX_CACHED_NODES) {
                Map.Entry<BlockPos, PathNode> entry = it.next();
                if (closedSet.contains(entry.getKey()) && Math.random() < 0.3) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Movement types for pathfinding cost calculation
     */
    public enum MovementType {
        WALK, DIAGONAL, JUMP, CLIMB, FALL, PARKOUR
    }
}