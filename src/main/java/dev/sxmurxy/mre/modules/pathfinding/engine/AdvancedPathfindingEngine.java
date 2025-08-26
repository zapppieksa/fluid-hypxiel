package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancedPathfindingEngine {
    private final World world;
    private final PathfindingCache cache;
    private final MovementValidator validator;
    private final HeuristicCalculator heuristic;

    // Performance constants
    private static final int MAX_NODES_PER_CHUNK = 15000;
    private static final int MAX_SEARCH_TIME_MS = 5000;
    private static final int CHUNK_SIZE = 16;
    private static final double HIERARCHICAL_THRESHOLD = 100.0;

    public AdvancedPathfindingEngine(World world) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        this.world = world;
        this.cache = new PathfindingCache();
        this.validator = new MovementValidator(world);
        this.heuristic = new HeuristicCalculator();
        System.out.println("[AdvancedPathfindingEngine] Initialized successfully");
    }

    public List<PathNode> findPath(BlockPos start, BlockPos goal) {
        // Check cache first
        List<PathNode> cachedPath = cache.getCachedPath(start, goal);
        if (cachedPath != null) {
            return cachedPath;
        }

        // Use hierarchical pathfinding for long distances
        double distance = start.getSquaredDistance(goal);
        if (distance > HIERARCHICAL_THRESHOLD * HIERARCHICAL_THRESHOLD) {
            return findHierarchicalPath(start, goal);
        }

        return findDirectPath(start, goal);
    }

    private List<PathNode> findHierarchicalPath(BlockPos start, BlockPos goal) {
        // Create high-level waypoints
        List<BlockPos> waypoints = createWaypoints(start, goal);
        List<PathNode> fullPath = new ArrayList<>();

        BlockPos currentStart = start;
        for (BlockPos waypoint : waypoints) {
            List<PathNode> segment = findDirectPath(currentStart, waypoint);
            if (segment == null) {
                // Try alternate waypoint
                BlockPos alternate = findAlternateWaypoint(currentStart, waypoint);
                segment = findDirectPath(currentStart, alternate);
                if (segment == null) return null;
            }

            fullPath.addAll(segment);
            currentStart = waypoint;
        }

        // Final segment to goal
        List<PathNode> finalSegment = findDirectPath(currentStart, goal);
        if (finalSegment != null) {
            fullPath.addAll(finalSegment);
        }

        return fullPath.isEmpty() ? null : fullPath;
    }

    private List<PathNode> findDirectPath(BlockPos start, BlockPos goal) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<BlockPos, PathNode> allNodes = new ConcurrentHashMap<>();
        Set<BlockPos> closedSet = ConcurrentHashMap.newKeySet();

        PathNode startNode = new PathNode(start, null, 0, heuristic.calculate(start, goal),
                PathNode.MovementType.WALK, Direction.NORTH, false, false, false, false,
                Vec3d.ofCenter(start));

        openSet.add(startNode);
        allNodes.put(start, startNode);

        long startTime = System.currentTimeMillis();
        int nodesExplored = 0;

        while (!openSet.isEmpty() && System.currentTimeMillis() - startTime < MAX_SEARCH_TIME_MS) {
            PathNode current = openSet.poll();
            nodesExplored++;

            if (isGoalReached(current.pos, goal)) {
                List<PathNode> path = reconstructPath(current);
                cache.cachePath(start, goal, path);
                return path;
            }

            closedSet.add(current.pos);

            // Generate neighbors with advanced movement analysis
            for (PathNode neighbor : generateNeighbors(current, goal)) {
                if (closedSet.contains(neighbor.pos)) continue;

                PathNode existing = allNodes.get(neighbor.pos);
                if (existing == null || neighbor.gCost < existing.gCost) {
                    allNodes.put(neighbor.pos, neighbor);
                    openSet.remove(existing);
                    openSet.add(neighbor);
                }
            }

            if (nodesExplored > MAX_NODES_PER_CHUNK) break;
        }

        return null; // No path found
    }

    private List<PathNode> generateNeighbors(PathNode current, BlockPos goal) {
        List<PathNode> neighbors = new ArrayList<>();

        // Standard movement patterns
        generateWalkingMoves(neighbors, current, goal);
        generateJumpingMoves(neighbors, current, goal);
        generateClimbingMoves(neighbors, current, goal);
        generateSwimmingMoves(neighbors, current, goal);
        generateParkourMoves(neighbors, current, goal);
        generateFallingMoves(neighbors, current, goal);

        return neighbors;
    }

    private void generateWalkingMoves(List<PathNode> neighbors, PathNode current, BlockPos goal) {
        // Variable step sizes for efficiency
        int[] distances = {1, 2, 3, 4, 5}; // Adaptive based on terrain

        for (Direction dir : Direction.Type.HORIZONTAL) {
            for (int dist : distances) {
                BlockPos newPos = current.pos.offset(dir, dist);

                if (validator.isValidWalkPosition(current.pos, newPos)) {
                    double cost = calculateMovementCost(current, newPos, PathNode.MovementType.WALK, dir);
                    double hCost = heuristic.calculate(newPos, goal);

                    neighbors.add(new PathNode(newPos, current, current.gCost + cost, hCost,
                            PathNode.MovementType.WALK, dir, false, false, false, false,
                            Vec3d.ofCenter(newPos)));
                }
            }
        }
    }

    private void generateJumpingMoves(List<PathNode> neighbors, PathNode current, BlockPos goal) {
        // Parkour-style jumping with gap detection
        for (Direction dir : Direction.Type.HORIZONTAL) {
            for (int distance = 1; distance <= 4; distance++) { // Up to 4-block jumps
                for (int height = -2; height <= 3; height++) { // Jump up/down
                    BlockPos targetPos = current.pos.offset(dir, distance).add(0, height, 0);

                    if (validator.canJumpTo(current.pos, targetPos)) {
                        double cost = calculateMovementCost(current, targetPos, PathNode.MovementType.JUMP, dir);
                        double hCost = heuristic.calculate(targetPos, goal);

                        neighbors.add(new PathNode(targetPos, current, current.gCost + cost, hCost,
                                PathNode.MovementType.JUMP, dir, true, true, false, false,
                                Vec3d.ofCenter(targetPos)));
                    }
                }
            }
        }

        // Diagonal parkour jumps
        int[][] diagonals = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
        for (int[] diag : diagonals) {
            for (int dist = 1; dist <= 3; dist++) {
                BlockPos targetPos = current.pos.add(diag[0] * dist, 1, diag[1] * dist);

                if (validator.canJumpTo(current.pos, targetPos)) {
                    double cost = calculateMovementCost(current, targetPos, PathNode.MovementType.PARKOUR, Direction.NORTH);
                    double hCost = heuristic.calculate(targetPos, goal);

                    neighbors.add(new PathNode(targetPos, current, current.gCost + cost, hCost,
                            PathNode.MovementType.PARKOUR, Direction.NORTH, true, true, false, false,
                            Vec3d.ofCenter(targetPos)));
                }
            }
        }
    }

    private void generateClimbingMoves(List<PathNode> neighbors, PathNode current, BlockPos goal) {
        // Check for ladders and vines
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;

            BlockPos checkPos = current.pos.offset(dir);
            Block block = world.getBlockState(checkPos).getBlock();

            if (block instanceof LadderBlock || block instanceof VineBlock) {
                // Climb up
                for (int height = 1; height <= 5; height++) {
                    BlockPos climbPos = checkPos.add(0, height, 0);
                    if (validator.isValidClimbPosition(climbPos, block)) {
                        double cost = calculateMovementCost(current, climbPos, PathNode.MovementType.CLIMB_LADDER, dir);
                        double hCost = heuristic.calculate(climbPos, goal);

                        neighbors.add(new PathNode(climbPos, current, current.gCost + cost, hCost,
                                block instanceof LadderBlock ? PathNode.MovementType.CLIMB_LADDER : PathNode.MovementType.CLIMB_VINE,
                                dir, false, false, true, false, Vec3d.ofCenter(climbPos)));
                    }
                }
            }
        }
    }

    private void generateSwimmingMoves(List<PathNode> neighbors, PathNode current, BlockPos goal) {
        FluidState fluidState = world.getFluidState(current.pos);
        if (!fluidState.isOf(Fluids.WATER) && !fluidState.isOf(Fluids.LAVA)) return;

        // 3D swimming movement
        for (Direction dir : Direction.values()) {
            for (int dist = 1; dist <= 3; dist++) {
                BlockPos swimPos = current.pos.offset(dir, dist);

                if (validator.isValidSwimPosition(current.pos, swimPos)) {
                    double cost = calculateMovementCost(current, swimPos, PathNode.MovementType.SWIM, dir);
                    double hCost = heuristic.calculate(swimPos, goal);

                    neighbors.add(new PathNode(swimPos, current, current.gCost + cost, hCost,
                            PathNode.MovementType.SWIM, dir, false, false, false, true,
                            Vec3d.ofCenter(swimPos)));
                }
            }
        }
    }

    private void generateFallingMoves(List<PathNode> neighbors, PathNode current, BlockPos goal) {
        // Safe falling with landing prediction
        for (Direction dir : Direction.Type.HORIZONTAL) {
            for (int distance = 0; distance <= 3; distance++) {
                BlockPos fallStart = distance == 0 ? current.pos : current.pos.offset(dir, distance);
                BlockPos landingPos = validator.findSafeLanding(fallStart, 20); // Max 20 block fall

                if (landingPos != null && !landingPos.equals(current.pos)) {
                    double cost = calculateMovementCost(current, landingPos, PathNode.MovementType.FALL, dir);
                    double hCost = heuristic.calculate(landingPos, goal);

                    neighbors.add(new PathNode(landingPos, current, current.gCost + cost, hCost,
                            PathNode.MovementType.FALL, dir, false, false, false, false,
                            Vec3d.ofCenter(landingPos)));
                }
            }
        }
    }

    private void generateParkourMoves(List<PathNode> neighbors, PathNode current, BlockPos goal) {
        // Advanced parkour sequences for skyblock-style navigation

        // Corner jumps around obstacles
        for (Direction primary : Direction.Type.HORIZONTAL) {
            for (Direction secondary : Direction.Type.HORIZONTAL) {
                if (primary == secondary || primary.getOpposite() == secondary) continue;

                // L-shaped jump pattern
                BlockPos corner = current.pos.offset(primary, 2).offset(secondary, 1).add(0, 1, 0);
                if (validator.canParkourTo(current.pos, corner)) {
                    double cost = calculateMovementCost(current, corner, PathNode.MovementType.PARKOUR, primary);
                    double hCost = heuristic.calculate(corner, goal);

                    neighbors.add(new PathNode(corner, current, current.gCost + cost, hCost,
                            PathNode.MovementType.PARKOUR, primary, true, true, false, false,
                            Vec3d.ofCenter(corner)));
                }
            }
        }

        // Precision jumps to small platforms
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx == 0 && dz == 0) continue;

                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos platformPos = current.pos.add(dx, dy, dz);

                    if (validator.isPlatformSuitable(platformPos) && validator.canParkourTo(current.pos, platformPos)) {
                        double cost = calculateMovementCost(current, platformPos, PathNode.MovementType.PARKOUR, Direction.NORTH);
                        double hCost = heuristic.calculate(platformPos, goal);

                        neighbors.add(new PathNode(platformPos, current, current.gCost + cost, hCost,
                                PathNode.MovementType.PARKOUR, Direction.NORTH, true, true, false, false,
                                Vec3d.ofCenter(platformPos)));
                    }
                }
            }
        }
    }

    private double calculateMovementCost(PathNode current, BlockPos newPos, PathNode.MovementType moveType, Direction dir) {
        double baseCost = Vec3d.ofCenter(current.pos).distanceTo(Vec3d.ofCenter(newPos));
        double multiplier = 1.0;

        switch (moveType) {
            case WALK: multiplier = 1.0; break;
            case SPRINT: multiplier = 0.8; break;
            case JUMP: multiplier = 1.3; break;
            case FALL: multiplier = 0.4; break;
            case CLIMB_LADDER: multiplier = 2.5; break;
            case CLIMB_VINE: multiplier = 3.0; break;
            case SWIM: multiplier = 2.2; break;
            case PARKOUR: multiplier = 1.8; break;
            case BRIDGE: multiplier = 4.0; break;
        }

        // Add penalties for dangerous areas
        multiplier += validator.getDangerPenalty(newPos);

        // Add bonus for momentum conservation
        if (current.parent != null) {
            Vec3d prevDir = Vec3d.of(current.pos.subtract(current.parent.pos));
            Vec3d currentDir = Vec3d.of(newPos.subtract(current.pos));
            if (prevDir.normalize().dotProduct(currentDir.normalize()) > 0.7) {
                multiplier *= 0.9; // 10% bonus for straight-line movement
            }
        }

        return baseCost * multiplier;
    }

    private boolean isGoalReached(BlockPos current, BlockPos goal) {
        return current.getSquaredDistance(goal) <= 2.0; // Allow slight tolerance
    }

    private List<PathNode> reconstructPath(PathNode goalNode) {
        List<PathNode> path = new ArrayList<>();
        PathNode current = goalNode;

        while (current != null) {
            path.add(0, current);
            current = current.parent;
        }

        return optimizePath(path);
    }

    private List<PathNode> optimizePath(List<PathNode> originalPath) {
        if (originalPath.size() < 3) return originalPath;

        List<PathNode> optimized = new ArrayList<>();
        optimized.add(originalPath.get(0));

        for (int i = 1; i < originalPath.size() - 1; i++) {
            PathNode prev = optimized.get(optimized.size() - 1);
            PathNode current = originalPath.get(i);
            PathNode next = originalPath.get(i + 1);

            // Skip nodes if we can move directly
            if (!validator.needsIntermediateNode(prev.pos, next.pos)) {
                continue; // Skip current node
            }

            optimized.add(current);
        }

        optimized.add(originalPath.get(originalPath.size() - 1));
        return optimized;
    }

    private List<BlockPos> createWaypoints(BlockPos start, BlockPos goal) {
        List<BlockPos> waypoints = new ArrayList<>();
        Vec3d direction = Vec3d.ofCenter(goal).subtract(Vec3d.ofCenter(start));
        double totalDistance = direction.length();
        direction = direction.normalize();

        // Create waypoints every 64 blocks
        for (double d = 64; d < totalDistance; d += 64) {
            Vec3d waypointPos = Vec3d.ofCenter(start).add(direction.multiply(d));
            BlockPos waypoint = BlockPos.ofFloored(waypointPos);
            waypoint = validator.findNearestValidPosition(waypoint, 16);
            if (waypoint != null) {
                waypoints.add(waypoint);
            }
        }

        return waypoints;
    }

    private BlockPos findAlternateWaypoint(BlockPos from, BlockPos original) {
        // Try positions around the original waypoint
        for (int radius = 8; radius <= 32; radius += 8) {
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                int offsetX = (int) (radius * Math.cos(rad));
                int offsetZ = (int) (radius * Math.sin(rad));

                BlockPos alternate = original.add(offsetX, 0, offsetZ);
                alternate = validator.findNearestValidPosition(alternate, 16);

                if (alternate != null) {
                    return alternate;
                }
            }
        }

        return original; // Fallback
    }
}