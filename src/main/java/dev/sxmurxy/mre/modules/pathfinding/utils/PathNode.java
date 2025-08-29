package dev.sxmurxy.mre.modules.pathfinding.utils;

import dev.sxmurxy.mre.modules.pathfinding.movement.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Represents a single node in the pathfinding search with comprehensive metadata
 * Contains position, costs, parent reference, movement data, and state information
 */
public class PathNode implements Comparable<PathNode> {

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
    private boolean cached = false;

    // Performance tracking
    private int calculationGeneration = 0;
    private double originalGCost = Double.MAX_VALUE;

    public PathNode(BlockPos pos) {
        this.pos = pos;
        this.timestamp = System.currentTimeMillis();
        this.originalGCost = Double.MAX_VALUE;
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
        if (originalGCost == Double.MAX_VALUE) {
            originalGCost = gCost;
        }
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
    }

    public boolean isSpecialMove() {
        return isSpecialMove;
    }

    public void setSpecialMove(boolean specialMove) {
        isSpecialMove = specialMove;
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

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCalculationGeneration() {
        return calculationGeneration;
    }

    public void setCalculationGeneration(int generation) {
        this.calculationGeneration = generation;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get center position as Vec3d for precise calculations
     */
    public Vec3d getCenterVec3d() {
        return Vec3d.ofCenter(pos);
    }

    /**
     * Get bottom center position as Vec3d (for player positioning)
     */
    public Vec3d getBottomCenterVec3d() {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    /**
     * Calculate distance to another node
     */
    public double distanceTo(PathNode other) {
        return Math.sqrt(pos.getSquaredDistance(other.pos));
    }

    /**
     * Calculate squared distance to another node (faster)
     */
    public double squaredDistanceTo(PathNode other) {
        return pos.getSquaredDistance(other.pos);
    }

    /**
     * Calculate distance to a BlockPos
     */
    public double distanceTo(BlockPos otherPos) {
        return Math.sqrt(pos.getSquaredDistance(otherPos));
    }

    /**
     * Calculate Manhattan distance to another node
     */
    public int manhattanDistanceTo(PathNode other) {
        return pos.getManhattanDistance(other.pos);
    }

    /**
     * Check if this node is adjacent to another node
     */
    public boolean isAdjacentTo(PathNode other) {
        return pos.getManhattanDistance(other.pos) == 1;
    }

    /**
     * Check if this node is diagonally adjacent to another node
     */
    public boolean isDiagonalTo(PathNode other) {
        int dx = Math.abs(pos.getX() - other.pos.getX());
        int dy = Math.abs(pos.getY() - other.pos.getY());
        int dz = Math.abs(pos.getZ() - other.pos.getZ());
        return (dx + dy + dz) <= 2 && dx <= 1 && dy <= 1 && dz <= 1;
    }

    /**
     * Check if node has been improved since original calculation
     */
    public boolean hasBeenImproved() {
        return gCost < originalGCost - 0.001; // Small epsilon for floating point comparison
    }

    /**
     * Get improvement ratio (how much better this path is)
     */
    public double getImprovementRatio() {
        if (originalGCost == Double.MAX_VALUE) return 0.0;
        return (originalGCost - gCost) / originalGCost;
    }

    /**
     * Create a deep copy of this node
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
        copy.cached = this.cached;
        copy.calculationGeneration = this.calculationGeneration;
        copy.originalGCost = this.originalGCost;
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
     * Check if this node requires precise timing
     */
    public boolean requiresPreciseTiming() {
        return movementType.requiresPrecision();
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
        return movementType.getExecutionTicks();
    }

    /**
     * Check if this movement requires sprinting
     */
    public boolean requiresSprinting() {
        return movementType.prefersSprinting() ||
                (parent != null && distanceTo(parent) > 2.5);
    }

    /**
     * Get movement priority for pathfinding tie-breaking
     */
    private int getMovementPriority() {
        return movementType.getPriority();
    }

    /**
     * Check if this node is stale (too old to be reliable)
     */
    public boolean isStale(long currentTime, long maxAge) {
        return (currentTime - timestamp) > maxAge;
    }

    /**
     * Refresh timestamp
     */
    public void refreshTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get age of this node in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
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
    @Override
    public int compareTo(PathNode other) {
        // Primary: F-cost comparison
        int fCompare = Double.compare(this.getFCost(), other.getFCost());
        if (fCompare != 0) {
            return fCompare;
        }

        // Secondary: H-cost comparison (prefer nodes closer to goal)
        int hCompare = Double.compare(this.hCost, other.hCost);
        if (hCompare != 0) {
            return hCompare;
        }

        // Tertiary: Movement priority (prefer simpler movements)
        int priorityCompare = Integer.compare(this.getMovementPriority(), other.getMovementPriority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        // Quaternary: Timestamp (prefer newer nodes)
        return Long.compare(other.timestamp, this.timestamp);
    }

    /**
     * Validate this node's state for debugging
     */
    public boolean isValid() {
        return pos != null &&
                gCost >= 0 &&
                hCost >= 0 &&
                movementType != null &&
                timestamp > 0;
    }

    /**
     * Get comprehensive node statistics
     */
    public String getStats() {
        return String.format(
                "PathNode Stats:\n" +
                        "  Position: %s\n" +
                        "  G-Cost: %.2f (Original: %.2f)\n" +
                        "  H-Cost: %.2f\n" +
                        "  F-Cost: %.2f\n" +
                        "  Movement: %s\n" +
                        "  Special: %s\n" +
                        "  Dangerous: %s\n" +
                        "  Age: %dms\n" +
                        "  Generation: %d\n" +
                        "  Improved: %s",
                pos.toShortString(), gCost, originalGCost, hCost, getFCost(),
                movementType, isSpecialMove, dangerous, getAge(),
                calculationGeneration, hasBeenImproved()
        );
    }
}