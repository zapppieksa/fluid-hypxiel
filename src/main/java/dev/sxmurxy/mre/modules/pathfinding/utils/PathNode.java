package dev.sxmurxy.mre.modules.pathfinding.utils;

import dev.sxmurxy.mre.modules.pathfinding.engine.AStarPathfinder.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Represents a single node in the pathfinding search
 * Contains position, costs, parent reference, and movement metadata
 */
public class PathNode {

    // Core pathfinding data
    private final BlockPos pos;
    private double gCost; // Distance from start node
    private double hCost; // Heuristic distance to goal
    private PathNode parent;

    // Movement metadata
    private MovementType movementType = MovementType.WALK;
    private long timestamp;
    private boolean isSpecialMove = false;

    // Node state flags
    private boolean visited = false;
    private boolean dangerous = false;

    public PathNode(BlockPos pos) {
        this.pos = pos;
        this.timestamp = System.currentTimeMillis();
    }

    public PathNode(int x, int y, int z) {
        this(new BlockPos(x, y, z));
    }

    // ==================== GETTERS AND SETTERS ====================

    public BlockPos getPos() {
        return pos;
    }

    public double getGCost() {
        return gCost;
    }

    public void setGCost(double gCost) {
        this.gCost = gCost;
    }

    public double getHCost() {
        return hCost;
    }

    public void setHCost(double hCost) {
        this.hCost = hCost;
    }

    /**
     * F-cost is the total cost (G + H) used by A* algorithm
     */
    public double getFCost() {
        return gCost + hCost;
    }

    public PathNode getParent() {
        return parent;
    }

    public void setParent(PathNode parent) {
        this.parent = parent;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
        this.isSpecialMove = (movementType != MovementType.WALK && movementType != MovementType.DIAGONAL);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean isDangerous() {
        return dangerous;
    }

    public void setDangerous(boolean dangerous) {
        this.dangerous = dangerous;
    }

    public boolean isSpecialMove() {
        return isSpecialMove;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get position as Vec3d centered on the block
     */
    public Vec3d getCenterVec3d() {
        return Vec3d.ofCenter(pos);
    }

    /**
     * Get position as Vec3d at ground level (for player positioning)
     */
    public Vec3d getBottomCenterVec3d() {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    /**
     * Get position with slight random offset for humanization
     */
    public Vec3d getHumanizedVec3d() {
        double offsetX = (Math.random() - 0.5) * 0.3;
        double offsetZ = (Math.random() - 0.5) * 0.3;
        return new Vec3d(pos.getX() + 0.5 + offsetX, pos.getY(), pos.getZ() + 0.5 + offsetZ);
    }

    /**
     * Calculate distance to another node
     */
    public double distanceTo(PathNode other) {
        return Math.sqrt(pos.getSquaredDistance(other.pos));
    }

    /**
     * Calculate Manhattan distance to another node
     */
    public int manhattanDistanceTo(PathNode other) {
        return Math.abs(pos.getX() - other.pos.getX()) +
                Math.abs(pos.getY() - other.pos.getY()) +
                Math.abs(pos.getZ() - other.pos.getZ());
    }

    /**
     * Check if this node requires special movement handling
     */
    public boolean requiresSpecialMovement() {
        return isSpecialMove;
    }

    /**
     * Get movement priority (lower = higher priority)
     * Used for sorting nodes when multiple paths have same cost
     */
    public int getMovementPriority() {
        return switch (movementType) {
            case WALK -> 1;
            case DIAGONAL -> 2;
            case FALL -> 3;
            case JUMP -> 4;
            case CLIMB -> 5;
            case PARKOUR -> 6;
        };
    }

    /**
     * Get relative cost multiplier based on movement type
     */
    public double getMovementCostMultiplier() {
        return switch (movementType) {
            case WALK -> 1.0;
            case DIAGONAL -> 1.414;
            case JUMP -> 1.3;
            case CLIMB -> 1.8;
            case FALL -> 0.9;
            case PARKOUR -> 2.5;
        };
    }

    /**
     * Check if this node is adjacent to another node
     */
    public boolean isAdjacentTo(PathNode other) {
        int dx = Math.abs(pos.getX() - other.pos.getX());
        int dy = Math.abs(pos.getY() - other.pos.getY());
        int dz = Math.abs(pos.getZ() - other.pos.getZ());

        return dx <= 1 && dy <= 1 && dz <= 1 && (dx + dy + dz > 0);
    }

    /**
     * Check if this node is directly connected to another (no obstacles)
     */
    public boolean isDirectlyConnectedTo(PathNode other) {
        // Simple check - could be enhanced with world collision detection
        return manhattanDistanceTo(other) <= 2;
    }

    /**
     * Get the age of this node in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }

    /**
     * Check if this node is considered "fresh" (recently created)
     */
    public boolean isFresh(long maxAge) {
        return getAge() < maxAge;
    }

    /**
     * Create a copy of this node with updated costs
     */
    public PathNode copy() {
        PathNode copy = new PathNode(pos);
        copy.gCost = this.gCost;
        copy.hCost = this.hCost;
        copy.parent = this.parent;
        copy.movementType = this.movementType;
        copy.isSpecialMove = this.isSpecialMove;
        copy.visited = this.visited;
        copy.dangerous = this.dangerous;
        return copy;
    }

    /**
     * Get debug information about this node
     */
    public String getDebugInfo() {
        return String.format("PathNode{pos=%s, g=%.2f, h=%.2f, f=%.2f, type=%s, special=%s}",
                pos.toShortString(), gCost, hCost, getFCost(), movementType, isSpecialMove);
    }

    /**
     * Check if this is a valid landing position for falling
     */
    public boolean isValidLandingPosition() {
        // Basic check - node should not be dangerous and should be on solid ground
        return !dangerous && movementType != MovementType.PARKOUR;
    }

    /**
     * Check if this is a valid jumping position
     */
    public boolean isValidJumpPosition() {
        return !dangerous && (movementType == MovementType.JUMP || movementType == MovementType.PARKOUR);
    }

    /**
     * Check if this node represents a climbing action
     */
    public boolean isClimbing() {
        return movementType == MovementType.CLIMB;
    }

    /**
     * Check if this node represents a falling action
     */
    public boolean isFalling() {
        return movementType == MovementType.FALL;
    }

    /**
     * Get movement vector from parent to this node
     */
    public Vec3d getMovementVector() {
        if (parent == null) {
            return Vec3d.ZERO;
        }
        return getCenterVec3d().subtract(parent.getCenterVec3d());
    }

    /**
     * Get movement direction as a unit vector
     */
    public Vec3d getMovementDirection() {
        Vec3d movement = getMovementVector();
        return movement.length() > 0 ? movement.normalize() : Vec3d.ZERO;
    }

    /**
     * Get estimated time to execute this movement (in ticks)
     */
    public int getEstimatedExecutionTime() {
        return switch (movementType) {
            case WALK -> 2;
            case DIAGONAL -> 3;
            case JUMP -> 4;
            case CLIMB -> 6;
            case FALL -> 1;
            case PARKOUR -> 8;
        };
    }

    /**
     * Check if this movement requires sprinting
     */
    public boolean requiresSprinting() {
        return movementType == MovementType.PARKOUR ||
                (movementType == MovementType.JUMP && distanceTo(parent) > 2.5);
    }

    /**
     * Check if this movement requires precise timing
     */
    public boolean requiresPreciseTiming() {
        return movementType == MovementType.PARKOUR || movementType == MovementType.JUMP;
    }

    // ==================== OBJECT OVERRIDES ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PathNode pathNode = (PathNode) obj;
        return pos.equals(pathNode.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return String.format("PathNode{pos=%s, f=%.2f, movement=%s}",
                pos.toShortString(), getFCost(), movementType);
    }

    /**
     * Compare nodes for priority queue (lower F-cost = higher priority)
     */
    public int compareTo(PathNode other) {
        int fCompare = Double.compare(this.getFCost(), other.getFCost());
        if (fCompare != 0) {
            return fCompare;
        }

        // If F-costs are equal, prefer nodes with lower H-cost (closer to goal)
        int hCompare = Double.compare(this.hCost, other.hCost);
        if (hCompare != 0) {
            return hCompare;
        }

        // If still equal, prefer simpler movement types
        return Integer.compare(this.getMovementPriority(), other.getMovementPriority());
    }
}