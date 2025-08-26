package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PathNode implements Comparable<PathNode> {
    public final BlockPos pos;
    public final PathNode parent;
    public final double gCost; // Actual cost from start
    public final double hCost; // Heuristic cost to goal
    public final double fCost; // Total cost (g + h)
    public final MovementType movementType;
    public final Direction facing;
    public final boolean requiresJump;
    public final boolean requiresSprint;
    public final boolean isClimbing;
    public final boolean isSwimming;
    public final double executionTime; // Time to execute this movement
    public final Vec3d exactPosition; // Precise position within block

    public enum MovementType {
        WALK, SPRINT, JUMP, FALL, CLIMB_LADDER, CLIMB_VINE, SWIM, PARKOUR, BRIDGE
    }

    public PathNode(BlockPos pos, PathNode parent, double gCost, double hCost,
                    MovementType movementType, Direction facing, boolean requiresJump,
                    boolean requiresSprint, boolean isClimbing, boolean isSwimming,
                    Vec3d exactPosition) {
        this.pos = pos;
        this.parent = parent;
        this.gCost = gCost;
        this.hCost = hCost;
        this.fCost = gCost + hCost;
        this.movementType = movementType;
        this.facing = facing;
        this.requiresJump = requiresJump;
        this.requiresSprint = requiresSprint;
        this.isClimbing = isClimbing;
        this.isSwimming = isSwimming;
        this.executionTime = calculateExecutionTime();
        this.exactPosition = exactPosition;
    }

    private double calculateExecutionTime() {
        switch (movementType) {
            case WALK: return 0.5;
            case SPRINT: return 0.38;
            case JUMP: return 0.8;
            case FALL: return Math.min(2.0, Math.max(0.3, Math.abs(parent != null ? pos.getY() - parent.pos.getY() : 1) * 0.2));
            case CLIMB_LADDER: case CLIMB_VINE: return 1.2;
            case SWIM: return 0.9;
            case PARKOUR: return 1.5;
            case BRIDGE: return 2.0;
            default: return 0.5;
        }
    }

    @Override
    public int compareTo(PathNode other) {
        int fComparison = Double.compare(this.fCost, other.fCost);
        if (fComparison != 0) return fComparison;
        return Double.compare(this.hCost, other.hCost); // Tie-breaker: prefer closer to goal
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PathNode && ((PathNode) obj).pos.equals(this.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}