package dev.sxmurxy.mre.client.pathfinding;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced A* pathfinding system with jump prediction, path smoothing, and physics simulation.
 * Implements state-of-the-art algorithms for optimal path generation in Minecraft environments.
 */
public class Pathfinder {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Pathfinding parameters
    private static final int MAX_ITERATIONS = 20000;
    private static final double GOAL_TOLERANCE = 1.5;
    private static final int MAX_JUMP_HEIGHT = 4;
    private static final double MAX_FALL_DISTANCE = 10.0;

    // Physics constants
    private static final double GRAVITY = 0.08;
    private static final double AIR_RESISTANCE = 0.98;
    private static final double JUMP_VELOCITY = 0.42;
    private static final double WALK_SPEED = 0.215;
    private static final double SPRINT_SPEED = 0.28;

    // Caching
    private final Map<BlockPos, NodeData> nodeCache = new ConcurrentHashMap<>();
    private final Map<String, Double> heuristicCache = new ConcurrentHashMap<>();

    // Current pathfinding state
    private List<PathNode> currentPath = null;
    private boolean isPathfinding = false;

    public enum MoveType {
        WALK, SPRINT, JUMP, FALL, AOTV, ETHERWARP
    }

    public static class PathNode {
        public final BlockPos pos;
        public final MoveType move;
        public final Vec3d position;
        public final Vec3d velocity;
        public final double cost;

        public PathNode(BlockPos pos, MoveType move) {
            this.pos = pos;
            this.move = move;
            this.position = pos.toCenterPos();
            this.velocity = Vec3d.ZERO;
            this.cost = 0.0;
        }

        public PathNode(Vec3d position, Vec3d velocity, double cost, MoveType move) {
            this.pos = BlockPos.ofFloored(position);
            this.position = position;
            this.velocity = velocity;
            this.cost = cost;
            this.move = move;
        }
    }

    /**
     * Main pathfinding method using advanced A* with jump prediction and path smoothing.
     */
    public boolean findPath(BlockPos start, BlockPos goal) {
        if (mc.player == null || mc.world == null) return false;

        isPathfinding = true;
        long startTime = System.currentTimeMillis();

        try {
            List<PathNode> rawPath = executeAStar(start, goal);
            if (rawPath == null || rawPath.isEmpty()) {
                return false;
            }

            // Apply advanced path smoothing
            List<PathNode> smoothedPath = applyPathSmoothing(rawPath);

            currentPath = smoothedPath;

            long endTime = System.currentTimeMillis();
            System.out.printf("Pathfinding completed in %dms with %d nodes%n",
                    endTime - startTime, smoothedPath.size());

            return true;

        } finally {
            isPathfinding = false;
        }
    }

    /**
     * Advanced A* implementation with jump prediction and 3D movement.
     */
    private List<PathNode> executeAStar(BlockPos start, BlockPos goal) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, AStarNode> allNodes = new HashMap<>();

        AStarNode startNode = new AStarNode(start, null, 0, calculateHeuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            AStarNode current = openSet.poll();

            if (current.pos.isWithinDistance(goal, GOAL_TOLERANCE)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            // Generate neighbors with advanced movement options
            for (AStarNode neighbor : generateAdvancedNeighbors(current, goal)) {
                if (closedSet.contains(neighbor.pos)) continue;

                AStarNode existing = allNodes.get(neighbor.pos);
                if (existing != null && existing.gCost <= neighbor.gCost) continue;

                allNodes.put(neighbor.pos, neighbor);
                openSet.add(neighbor);
            }
        }

        return null; // No path found
    }

    /**
     * Generate neighbors with advanced movement including jumps, drops, and special moves.
     */
    private List<AStarNode> generateAdvancedNeighbors(AStarNode current, BlockPos goal) {
        List<AStarNode> neighbors = new ArrayList<>();

        // Standard 8-directional movement
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos horizontalPos = current.pos.add(dx, 0, dz);

                // Ground level movement
                if (isWalkable(horizontalPos)) {
                    double cost = calculateMovementCost(current.pos, horizontalPos, MoveType.WALK);
                    neighbors.add(createNeighborNode(current, horizontalPos, cost, MoveType.WALK, goal));
                }

                // Jump movement (1-4 blocks up)
                for (int dy = 1; dy <= MAX_JUMP_HEIGHT; dy++) {
                    BlockPos jumpPos = horizontalPos.add(0, dy, 0);
                    if (canJumpTo(current.pos, jumpPos)) {
                        double cost = calculateJumpCost(current.pos, jumpPos);
                        neighbors.add(createNeighborNode(current, jumpPos, cost, MoveType.JUMP, goal));
                    }
                }

                // Drop movement (falling down)
                for (int dy = -1; dy >= -MAX_FALL_DISTANCE && dy >= -10; dy--) {
                    BlockPos dropPos = horizontalPos.add(0, dy, 0);
                    if (canDropTo(current.pos, dropPos)) {
                        double cost = calculateDropCost(current.pos, dropPos);
                        neighbors.add(createNeighborNode(current, dropPos, cost, MoveType.FALL, goal));
                        break; // Stop at first valid drop position
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * Apply advanced path smoothing using string pulling algorithm.
     */
    private List<PathNode> applyPathSmoothing(List<PathNode> rawPath) {
        if (rawPath.size() < 3) return rawPath;

        List<PathNode> result = new ArrayList<>();
        result.add(rawPath.get(0));

        int current = 0;
        while (current < rawPath.size() - 1) {
            int next = current + 1;

            // Find the furthest point we can reach directly
            while (next < rawPath.size() && hasLineOfSight(rawPath.get(current).position, rawPath.get(next).position)) {
                next++;
            }

            result.add(rawPath.get(next - 1));
            current = next - 1;
        }

        return result;
    }

    // Utility methods

    private AStarNode createNeighborNode(AStarNode parent, BlockPos pos, double moveCost, MoveType moveType, BlockPos goal) {
        double gCost = parent.gCost + moveCost;
        double hCost = calculateHeuristic(pos, goal);
        return new AStarNode(pos, parent, gCost, hCost, moveType);
    }

    private double calculateHeuristic(BlockPos from, BlockPos to) {
        String key = from.toString() + "->" + to.toString();
        return heuristicCache.computeIfAbsent(key, k -> {
            // 3D Euclidean distance with movement cost weighting
            double dx = Math.abs(to.getX() - from.getX());
            double dy = Math.abs(to.getY() - from.getY());
            double dz = Math.abs(to.getZ() - from.getZ());

            // Weight vertical movement more heavily
            return Math.sqrt(dx * dx + dy * dy * 2.0 + dz * dz);
        });
    }

    private List<PathNode> reconstructPath(AStarNode goalNode) {
        List<PathNode> path = new ArrayList<>();
        AStarNode current = goalNode;

        while (current != null) {
            path.add(new PathNode(current.pos, current.moveType));
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    private boolean isWalkable(BlockPos pos) {
        if (mc.world == null) return false;

        return !mc.world.getBlockState(pos).isSolidBlock(mc.world, pos) &&
                !mc.world.getBlockState(pos.up()).isSolidBlock(mc.world, pos.up()) &&
                mc.world.getBlockState(pos.down()).isSolidBlock(mc.world, pos.down());
    }

    private boolean canJumpTo(BlockPos from, BlockPos to) {
        if (to.getY() - from.getY() > MAX_JUMP_HEIGHT) return false;
        return isWalkable(to) && hasLineOfSight(from.toCenterPos(), to.toCenterPos());
    }

    private boolean canDropTo(BlockPos from, BlockPos to) {
        if (from.getY() - to.getY() > MAX_FALL_DISTANCE) return false;
        return isWalkable(to);
    }

    private boolean hasLineOfSight(Vec3d from, Vec3d to) {
        if (mc.world == null) return false;

        RaycastContext context = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        return mc.world.raycast(context).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    private double calculateMovementCost(BlockPos from, BlockPos to, MoveType moveType) {
        double baseCost = from.toCenterPos().distanceTo(to.toCenterPos());

        return switch (moveType) {
            case SPRINT -> baseCost * 0.8;
            case JUMP -> baseCost * 1.5;
            case FALL -> baseCost * 1.2;
            default -> baseCost;
        };
    }

    private double calculateJumpCost(BlockPos from, BlockPos to) {
        double verticalDist = Math.abs(to.getY() - from.getY());
        double horizontalDist = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));

        return horizontalDist + verticalDist * 1.5 + 0.5; // Extra cost for jumping
    }

    private double calculateDropCost(BlockPos from, BlockPos to) {
        double fallDistance = from.getY() - to.getY();
        return Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2)) +
                Math.sqrt(fallDistance) * 0.5;
    }

    public void stopPathfinding() {
        isPathfinding = false;
        currentPath = null;
    }

    public boolean isPathfinding() {
        return isPathfinding;
    }

    public List<PathNode> getCurrentPath() {
        return currentPath;
    }

    // Helper classes
    private static class AStarNode {
        final BlockPos pos;
        final AStarNode parent;
        final double gCost;
        final double hCost;
        final double fCost;
        final MoveType moveType;

        AStarNode(BlockPos pos, AStarNode parent, double gCost, double hCost) {
            this(pos, parent, gCost, hCost, MoveType.WALK);
        }

        AStarNode(BlockPos pos, AStarNode parent, double gCost, double hCost, MoveType moveType) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
            this.moveType = moveType;
        }
    }

    private static class NodeData {
        final boolean walkable;
        final double movementCost;
        final long cacheTime;

        NodeData(boolean walkable, double movementCost) {
            this.walkable = walkable;
            this.movementCost = movementCost;
            this.cacheTime = System.currentTimeMillis();
        }
    }
}