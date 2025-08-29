package dev.sxmurxy.mre.modules.pathfinding.movement;

/**
 * Represents different types of movements in pathfinding
 * Each movement type has different costs, execution times, and requirements
 */
public enum MovementType {

    /**
     * Standard walking movement (1 block horizontal)
     */
    WALK(1.0, 2, false, false),

    /**
     * Diagonal movement (corner-to-corner)
     */
    DIAGONAL(1.414, 3, false, false),

    /**
     * Jumping movement (1 block up, may include horizontal)
     */
    JUMP(1.5, 4, true, false),

    /**
     * Climbing movement (ladders, vines, scaffolding)
     */
    CLIMB(2.2, 6, true, true),

    /**
     * Falling movement (controlled falling)
     */
    FALL(0.8, 2, false, false),

    /**
     * Advanced parkour movement (gaps, precision jumps)
     */
    PARKOUR(3.0, 8, true, true);

    private final double baseCost;
    private final int executionTicks;
    private final boolean requiresJump;
    private final boolean requiresPrecision;

    MovementType(double baseCost, int executionTicks, boolean requiresJump, boolean requiresPrecision) {
        this.baseCost = baseCost;
        this.executionTicks = executionTicks;
        this.requiresJump = requiresJump;
        this.requiresPrecision = requiresPrecision;
    }

    /**
     * Get base movement cost multiplier
     */
    public double getBaseCost() {
        return baseCost;
    }

    /**
     * Get estimated execution time in ticks
     */
    public int getExecutionTicks() {
        return executionTicks;
    }

    /**
     * Check if movement requires jumping
     */
    public boolean requiresJump() {
        return requiresJump;
    }

    /**
     * Check if movement requires precision timing
     */
    public boolean requiresPrecision() {
        return requiresPrecision;
    }

    /**
     * Get movement priority (lower = higher priority)
     * Used for tie-breaking in pathfinding
     */
    public int getPriority() {
        return switch (this) {
            case WALK -> 1;
            case FALL -> 2;
            case DIAGONAL -> 3;
            case JUMP -> 4;
            case CLIMB -> 5;
            case PARKOUR -> 6;
        };
    }

    /**
     * Check if movement type is considered advanced
     */
    public boolean isAdvanced() {
        return this == PARKOUR || this == CLIMB;
    }

    /**
     * Check if movement type is vertical
     */
    public boolean isVertical() {
        return this == JUMP || this == CLIMB || this == FALL;
    }

    /**
     * Check if movement type requires sprinting
     */
    public boolean prefersSprinting() {
        return this == PARKOUR || this == JUMP;
    }

    /**
     * Get human-readable description
     */
    public String getDescription() {
        return switch (this) {
            case WALK -> "Walking";
            case DIAGONAL -> "Diagonal movement";
            case JUMP -> "Jumping";
            case CLIMB -> "Climbing";
            case FALL -> "Falling";
            case PARKOUR -> "Parkour movement";
        };
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}