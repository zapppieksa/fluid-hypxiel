package dev.sxmurxy.mre.modules.pathfinding.engine;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AStarPathfinder {
    private final World world;
    private final PathfinderConfig config;
    private final AtomicBoolean cancellationToken;

    public AStarPathfinder(World world, PathfinderConfig config, AtomicBoolean cancellationToken) {
        this.world = world;
        this.config = config;
        this.cancellationToken = cancellationToken;
    }

    public List<PathNode> findPath(BlockPos start, BlockPos end) {
        PathNode startNode = new PathNode(start, null);
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<BlockPos, PathNode> allNodes = new HashMap<>();
        startNode.gCost = 0;
        startNode.hCost = calculateHeuristic(start, end);
        startNode.calculateFCost();
        openSet.add(startNode);
        allNodes.put(start, startNode);
        int processedNodes = 0;
        while (!openSet.isEmpty() && !cancellationToken.get()) {
            if (processedNodes++ > config.MAX_PATHFINDING_NODES) return null;
            PathNode currentNode = openSet.poll();
            if (currentNode.position.getManhattanDistance(end) <= 1) return reconstructPath(currentNode);

            for (PathNode neighbor : getNeighbors(currentNode)) {
                if (allNodes.containsKey(neighbor.position) && allNodes.get(neighbor.position).fCost < Double.MAX_VALUE) continue;

                double tentativeGCost = currentNode.gCost + getMovementCost(currentNode, neighbor);
                if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = currentNode;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = calculateHeuristic(neighbor.position, end);
                    neighbor.calculateFCost();
                    if (!openSet.contains(neighbor)) openSet.add(neighbor);
                    allNodes.put(neighbor.position, neighbor);
                }
            }
        }
        return null;
    }

    private List<PathNode> reconstructPath(PathNode endNode) {
        List<PathNode> path = new ArrayList<>();
        PathNode currentNode = endNode;
        while (currentNode != null) { path.add(currentNode); currentNode = currentNode.parent; }
        Collections.reverse(path);
        return path;
    }

    private double calculateHeuristic(BlockPos a, BlockPos b) { return a.getManhattanDistance(b) * 10; }

    private List<PathNode> getNeighbors(PathNode node) {
        List<PathNode> neighbors = new ArrayList<>();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        int jumpBoost = player != null && player.hasStatusEffect(StatusEffects.JUMP_BOOST) ? player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1 : 0;

        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            if (isTraversable(node.position.add(x, y, z))) neighbors.add(new PathNode(node.position.add(x, y, z), node));
        }

        int jumpHeight = 1 + jumpBoost;
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            if (x == 0 && z == 0) continue;
            for (int y = -3; y <= jumpHeight; y++) {
                BlockPos jumpTarget = node.position.add(x, y, z);
                if (isTraversable(jumpTarget) && PathfinderEngine.hasLineOfSight(node.position, jumpTarget, world)) {
                    neighbors.add(new PathNode(jumpTarget, node));
                }
            }
        }
        return neighbors;
    }

    private boolean isTraversable(BlockPos pos) {
        if (world.getBlockState(pos.down()).getCollisionShape(world, pos.down()).isEmpty()) return false;
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty() && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty();
    }

    private double getMovementCost(PathNode from, PathNode to) {
        double cost = from.position.getManhattanDistance(to.position) * 10;
        int dy = to.position.getY() - from.position.getY();
        if (dy > 0) cost += 15 * dy; else if (dy < 0) cost += 1;

        if (from.parent != null) {
            Vec3d prevDir = Vec3d.ofCenter(from.position).subtract(Vec3d.ofCenter(from.parent.position)).normalize();
            Vec3d nextDir = Vec3d.ofCenter(to.position).subtract(Vec3d.ofCenter(from.position)).normalize();
            if (prevDir.dotProduct(nextDir) < 0.7) cost += 10;
        }

        BlockState blockState = world.getBlockState(to.position);
        if (blockState.isOf(Blocks.WATER)) cost += 30;
        if (blockState.isOf(Blocks.SOUL_SAND)) cost += 20;
        if (blockState.isOf(Blocks.ICE) || blockState.isOf(Blocks.PACKED_ICE)) cost -= 5;
        return cost;
    }
}