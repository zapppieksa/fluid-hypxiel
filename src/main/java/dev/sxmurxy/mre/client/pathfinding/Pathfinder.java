package dev.sxmurxy.mre.client.pathfinding;

import dev.sxmurxy.mre.client.rotations.RotationController;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A state-of-the-art navigation agent featuring a high-speed, two-phase hybrid
 * pathfinding algorithm for exceptionally fast calculation with teleport abilities.
 */
public class Pathfinder {

    // --- Inner Classes ---
    public enum PathfindingMode { WALK, OPTIMIZED }
    public enum MoveType { WALK, JUMP, FALL, AOTV, ETHERWARP }
    public static class PathNode {
        final BlockPos pos; final MoveType move;
        public PathNode(BlockPos pos, MoveType move) { this.pos = pos; this.move = move; }
    }
    static class AStarNode {
        BlockPos pos; AStarNode parent; double g, h, f; MoveType move;
        AStarNode(BlockPos pos, AStarNode parent, double g, double h, MoveType move) { this.pos = pos; this.parent = parent; this.g = g; this.h = h; this.f = g + h; this.move = move; }
        @Override public boolean equals(Object o) { return o instanceof AStarNode && ((AStarNode) o).pos.equals(pos); }
    }

    final PlayerEntity player;
    private final dev.sxmurxy.mre.client.pathfinding.MovementController movementController;
    private final RotationController rotationController;
    private final ClientWorld world;
    private final MinecraftClient mc;

    private List<Vec3d> smoothedPath;
    private List<MoveType> moveTypes;
    private int pathIndex;
    private BlockPos finalGoal;
    private long nextValidationTime;
    private long lastProgressTime;
    private Vec3d lastPlayerPos;

    public boolean aotvEnabled = false;
    public boolean etherwarpEnabled = false;
    public boolean debugMode = false;
    public PathfindingMode mode = PathfindingMode.OPTIMIZED;

    private static final long STUCK_TIMEOUT_MS = 3000;
    private static final long VALIDATION_INTERVAL_MS = 750;
    private static final int AOTV_RANGE = 7;
    private static final int ETHERWARP_RANGE = 57;
    private static final int MAX_FALL_DISTANCE = 4;

    public Pathfinder(PlayerEntity player) {
        this.player = player;
        this.mc = MinecraftClient.getInstance();
        this.world = (ClientWorld) player.getWorld();
        this.movementController = new dev.sxmurxy.mre.client.pathfinding.MovementController(player);
        this.rotationController = new RotationController(player);
    }

    public void executePath(List<PathNode> rawPath, BlockPos goal) {
        if (rawPath == null || rawPath.isEmpty()) { stop(); return; }
        this.finalGoal = goal;

        PathSmoother.SmoothedPath smoothed = PathSmoother.smooth(rawPath, 10);
        this.smoothedPath = smoothed.path();
        this.moveTypes = smoothed.moveTypes();

        if (smoothedPath.isEmpty()) { if (debugMode) System.err.println("[Pathfinder] Path smoothing resulted in an empty path."); stop(); return; }
        if (debugMode) System.out.println("[Pathfinder] Starting execution of smoothed path with " + smoothedPath.size() + " points.");

        this.pathIndex = 0; this.lastPlayerPos = player.getPos(); this.lastProgressTime = System.currentTimeMillis();
        this.nextValidationTime = System.currentTimeMillis() + VALIDATION_INTERVAL_MS;
    }

    public void tick() {
        if (!isExecuting() || player.isDead()) return;
        if (player.getPos().distanceTo(lastPlayerPos) < 0.1) {
            if (System.currentTimeMillis() - lastProgressTime > STUCK_TIMEOUT_MS) {
                if (debugMode) System.err.println("[Pathfinder] Stuck detected!"); stop(); return;
            }
        } else { lastProgressTime = System.currentTimeMillis(); lastPlayerPos = player.getPos(); }
        if (System.currentTimeMillis() > nextValidationTime) {
            if (!isPathStillValid()) {
                if (debugMode) System.err.println("[Pathfinder] Path invalid, recalculating...");
                PathfinderAPI.findAndFollowPath(finalGoal, null); return;
            }
            nextValidationTime = System.currentTimeMillis() + VALIDATION_INTERVAL_MS;
        }

        MoveType currentMove = moveTypes.get(pathIndex);
        if (currentMove == MoveType.AOTV || currentMove == MoveType.ETHERWARP) { handleTeleport(smoothedPath.get(pathIndex), currentMove); }
        else { handleMovement(); }
    }

    private void handleMovement() {
        double reachDistance = 1.2 + (player.getVelocity().length() * 2.8);
        if (player.getPos().distanceTo(smoothedPath.get(pathIndex)) < reachDistance) {
            pathIndex++;
            if (pathIndex >= smoothedPath.size()) { stop(); return; }
        }
        movementController.tick(smoothedPath, pathIndex, moveTypes);
        rotationController.tick(smoothedPath, pathIndex, moveTypes);
    }

    private void handleTeleport(Vec3d target, MoveType move) {
        movementController.stop();
        rotationController.tickPrecise(target);
        Vec3d playerLookVec = player.getRotationVec(1.0F);
        Vec3d targetDir = target.subtract(player.getEyePos()).normalize();
        double angleDifference = Math.acos(playerLookVec.dotProduct(targetDir));
        if (Math.toDegrees(angleDifference) < 3) {
            if (move == MoveType.AOTV) movementController.setSneaking(true);
            mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            if (move == MoveType.AOTV) movementController.setSneaking(false);
            pathIndex++;
            if (pathIndex >= smoothedPath.size()) { stop(); }
        }
    }

    public List<PathNode> findPath(BlockPos start, BlockPos goal) {
        List<PathNode> walkPath = findRawWalkPath(start, goal);
        if (walkPath == null || walkPath.isEmpty()) {
            if (debugMode) System.err.println("[Pathfinder] Phase 1 failed: No walking path found.");
            return null;
        }

        if(mode == PathfindingMode.WALK) return PathSmoother.simplify(walkPath, this::canSee);

        List<PathNode> optimizedPath = optimizePathWithTeleports(walkPath);
        return PathSmoother.simplify(optimizedPath, this::canSee);
    }

    private List<PathNode> optimizePathWithTeleports(List<PathNode> walkPath) {
        if (walkPath.size() < 2) return walkPath;
        List<PathNode> optimizedPath = new ArrayList<>();
        optimizedPath.add(walkPath.get(0));
        int currentIndex = 0;

        while (currentIndex < walkPath.size() - 1) {
            int bestShortcutIndex = -1;
            MoveType bestShortcutType = null;

            if (etherwarpEnabled) {
                for (int i = walkPath.size() - 1; i > currentIndex; i--) {
                    if (isTeleportShortcutValid(walkPath.get(currentIndex), walkPath.get(i), ETHERWARP_RANGE)) {
                        bestShortcutIndex = i; bestShortcutType = MoveType.ETHERWARP; break;
                    }
                }
            }
            if (bestShortcutIndex == -1 && aotvEnabled) {
                for (int i = walkPath.size() - 1; i > currentIndex; i--) {
                    if (isTeleportShortcutValid(walkPath.get(currentIndex), walkPath.get(i), AOTV_RANGE)) {
                        bestShortcutIndex = i; bestShortcutType = MoveType.AOTV; break;
                    }
                }
            }
            if (bestShortcutIndex != -1) {
                if (debugMode) System.out.println("[Pathfinder Optimizer] Found " + bestShortcutType + " shortcut from node " + currentIndex + " to " + bestShortcutIndex);
                optimizedPath.add(new PathNode(walkPath.get(bestShortcutIndex).pos, bestShortcutType));
                currentIndex = bestShortcutIndex;
            } else {
                currentIndex++;
                optimizedPath.add(walkPath.get(currentIndex));
            }
        }
        return optimizedPath;
    }

    private boolean isTeleportShortcutValid(PathNode start, PathNode end, int range) {
        if (start.pos.getSquaredDistance(end.pos) > range * range) return false;
        if (!canSee(start.pos, end.pos)) return false;
        return isSafeLanding(end.pos);
    }

    private List<PathNode> findRawWalkPath(BlockPos start, BlockPos goal) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Set<BlockPos> closedSet = new HashSet<>();
        openSet.add(new AStarNode(start, null, 0, heuristic(start, goal), MoveType.WALK));
        int iterations = 0;
        while (!openSet.isEmpty()) {
            if (++iterations > 12000) { return null; }
            AStarNode currentNode = openSet.poll();
            if (currentNode.pos.isWithinDistance(goal, 1.5)) { return reconstructPath(currentNode); }
            closedSet.add(currentNode.pos);
            for (AStarNode neighborNode : getValidWalkNeighbors(currentNode, goal)) {
                if (closedSet.contains(neighborNode.pos)) continue;
                boolean inOpenSet = openSet.stream().anyMatch(n -> n.pos.equals(neighborNode.pos) && n.g < neighborNode.g);
                if (inOpenSet) continue;
                openSet.removeIf(n -> n.pos.equals(neighborNode.pos));
                openSet.add(neighborNode);
            }
        }
        return null;
    }

    private List<AStarNode> getValidWalkNeighbors(AStarNode parent, BlockPos goal) {
        List<AStarNode> neighbors = new ArrayList<>();
        BlockPos pos = parent.pos;
        int jumpHeight = 1 + (player.hasStatusEffect(StatusEffects.JUMP_BOOST) ? player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1 : 0);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            for (int y = -1; y <= jumpHeight; y++) {
                BlockPos neighborPos = pos.add(x, y, z);
                if (isTraversable(pos, neighborPos)) {
                    MoveType move = y > 0 ? MoveType.JUMP : (y < 0 ? MoveType.FALL : MoveType.WALK);
                    neighbors.add(createNode(neighborPos, parent, goal, move));
                }
            }
        }
        return neighbors;
    }

    private boolean isPathStillValid() {
        if (pathIndex >= smoothedPath.size() || player.getPos().distanceTo(smoothedPath.get(pathIndex)) > 6.0) return false;
        for (int i = pathIndex; i < Math.min(pathIndex + 20, smoothedPath.size()); i++) {
            BlockPos pointOnPath = BlockPos.ofFloored(smoothedPath.get(i));
            if (moveTypes.get(i) == MoveType.AOTV) { if (!isAir(pointOnPath)) return false; }
            else { if (!isSafeLanding(pointOnPath)) return false; }
        }
        return true;
    }

    private boolean isTraversable(BlockPos current, BlockPos neighbor) {
        if (!isSafeLanding(neighbor)) return false;
        BlockPos delta = neighbor.subtract(current);
        if (Math.abs(delta.getX()) != 0 && Math.abs(delta.getZ()) != 0) {
            if (!isAir(current.add(delta.getX(), 0, 0)) || !isAir(current.add(delta.getX(), 1, 0)) ||
                    !isAir(current.add(0, 0, delta.getZ())) || !isAir(current.add(0, 1, delta.getZ()))) {
                return false;
            }
        }
        if (Math.abs(delta.getY()) > 0) {
            if (!hasHeadroom(current) || !hasHeadroom(neighbor)) return false;
        }
        return true;
    }

    private boolean isSafeLanding(BlockPos pos) { return getBlockHeight(pos.down()) > 0 && hasHeadroom(pos); }

    private List<PathNode> reconstructPath(AStarNode node) {
        List<PathNode> path = new ArrayList<>();
        AStarNode current = node; while (current != null) { path.add(new PathNode(current.pos, current.move)); current = current.parent; }
        Collections.reverse(path); return path;
    }

    private AStarNode createNode(BlockPos pos, AStarNode parent, BlockPos goal, MoveType move) {
        double g = parent.g + parent.pos.getSquaredDistance(pos);
        if (parent.parent != null) {
            Vec3d v1 = parent.parent.pos.toCenterPos().subtract(parent.pos.toCenterPos());
            Vec3d v2 = parent.pos.toCenterPos().subtract(pos.toCenterPos());
            if (v1.normalize().dotProduct(v2.normalize()) < 0.7) g += 20;
        }
        if(isNearWall(pos)) g+= 5;
        return new AStarNode(pos, parent, g, heuristic(pos, goal), move);
    }

    private double getBlockHeight(BlockPos pos) {
        BlockState state = world.getBlockState(pos); if (state.isAir()) return 0;
        if (state.getBlock() instanceof SlabBlock) return state.get(SlabBlock.TYPE) == SlabType.TOP ? 1.0 : 0.5;
        return state.getCollisionShape(world, pos).getMax(net.minecraft.util.math.Direction.Axis.Y);
    }

    private boolean isNearWall(BlockPos pos){
        for(int x = -1; x<=1; x++) for(int z = -1; z<=1; z++){
            if(x==0 && z==0) continue;
            if(!isAir(pos.add(x,0,z)) || !isAir(pos.add(x,1,z))) return true;
        }
        return false;
    }

    private double heuristic(BlockPos a, BlockPos b) { return a.getManhattanDistance(b); }
    private boolean hasHeadroom(BlockPos pos) { return isAir(pos) && isAir(pos.up()); }
    private boolean isAir(BlockPos pos) { return world.getBlockState(pos).isAir(); }

    public boolean canSee(BlockPos start, BlockPos end) {
        RaycastContext context = new RaycastContext(Vec3d.ofCenter(start), Vec3d.ofCenter(end), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
        return world.raycast(context).getType() == HitResult.Type.MISS;
    }

    public void stop() {
        if (isExecuting() && debugMode) System.out.println("[Pathfinder] Execution stopped.");
        this.smoothedPath = null; this.moveTypes = null; this.pathIndex = 0; this.finalGoal = null;
        movementController.stop(); rotationController.stop();
    }

    public boolean isExecuting() { return this.smoothedPath != null && !this.smoothedPath.isEmpty(); }
}

