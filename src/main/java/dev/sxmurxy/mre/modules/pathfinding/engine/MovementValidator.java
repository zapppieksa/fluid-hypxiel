package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

class MovementValidator {
    private final World world;

    public MovementValidator(World world) {
        this.world = world;
    }

    public boolean isValidWalkPosition(BlockPos from, BlockPos to) {
        // Check if we can walk from 'from' to 'to'
        int distance = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));

        for (int i = 0; i <= distance; i++) {
            double progress = distance == 0 ? 0 : (double) i / distance;
            BlockPos checkPos = new BlockPos(
                    (int) (from.getX() + progress * (to.getX() - from.getX())),
                    to.getY(),
                    (int) (from.getZ() + progress * (to.getZ() - from.getZ()))
            );

            if (!isPositionSafe(checkPos)) return false;
        }

        return true;
    }

    public boolean canJumpTo(BlockPos from, BlockPos to) {
        // Check horizontal distance
        double horizontalDist = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
        if (horizontalDist > 4.2) return false; // Max jump distance with sprint

        // Check vertical distance
        int verticalDist = to.getY() - from.getY();
        if (verticalDist > 1 && horizontalDist > 3.5) return false; // Can't jump too high and far
        if (verticalDist < -3) return false; // Don't jump into deep holes

        // Check landing position
        if (!isPositionSafe(to)) return false;

        // Check for obstacles in jump path
        return isJumpPathClear(from, to);
    }

    public boolean canParkourTo(BlockPos from, BlockPos to) {
        double distance = Vec3d.ofCenter(from).distanceTo(Vec3d.ofCenter(to));
        if (distance > 5.0) return false;

        // More lenient parkour rules
        return isPositionSafe(to) && hasValidLandingArea(to);
    }

    public boolean isValidClimbPosition(BlockPos pos, Block climbBlock) {
        // Check if there's the climb block and space for player
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != climbBlock) return false;

        return isPositionSafe(pos.up()) && !world.getBlockState(pos.up(2)).isSolidBlock(world, pos.up(2));
    }

    public boolean isValidSwimPosition(BlockPos from, BlockPos to) {
        FluidState toState = world.getFluidState(to);
        if (toState.isEmpty()) return false;

        // Avoid swimming in lava unless necessary
        if (toState.isOf(Fluids.LAVA)) {
            return world.getFluidState(from).isOf(Fluids.LAVA); // Only if already in lava
        }

        return true;
    }

    public BlockPos findSafeLanding(BlockPos startPos, int maxFallDistance) {
        for (int i = 1; i <= maxFallDistance; i++) {
            BlockPos checkPos = startPos.down(i);

            if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
                BlockPos landingPos = checkPos.up();
                if (isPositionSafe(landingPos) && !isVoid(landingPos)) {
                    return landingPos;
                }
            }
        }

        return null;
    }

    public boolean isPlatformSuitable(BlockPos pos) {
        // Check if it's a valid platform for parkour
        BlockState ground = world.getBlockState(pos.down());
        if (!ground.isSolidBlock(world, pos.down())) return false;

        // Ensure there's space for player
        return isPositionSafe(pos) && !isVoid(pos);
    }

    public double getDangerPenalty(BlockPos pos) {
        double penalty = 0;

        // Void penalty
        if (isVoid(pos)) penalty += 50.0;

        // Lava penalty
        if (world.getFluidState(pos).isOf(Fluids.LAVA)) penalty += 5.0;

        // Fall damage penalty
        BlockPos groundPos = findSafeLanding(pos, 10);
        if (groundPos != null && pos.getY() - groundPos.getY() > 3) {
            penalty += (pos.getY() - groundPos.getY()) * 0.5;
        }

        // Monster spawning penalty (dark areas)
        if (world.getLightLevel(pos) < 7) penalty += 1.0;

        return penalty;
    }

    public boolean needsIntermediateNode(BlockPos from, BlockPos to) {
        // Check if we need to go through intermediate positions
        double distance = Vec3d.ofCenter(from).distanceTo(Vec3d.ofCenter(to));
        if (distance <= 3.0) return false; // Close enough for direct movement

        // Check for obstacles in path
        return !isDirectPathClear(from, to);
    }

    public BlockPos findNearestValidPosition(BlockPos center, int radius) {
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos pos = center.add(dx, dy, dz);
                        if (isPositionSafe(pos)) {
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isPositionSafe(BlockPos pos) {
        // Check if player can stand here
        BlockState ground = world.getBlockState(pos.down());
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());

        // Ground must be solid or ladder/vine
        if (!ground.isSolidBlock(world, pos.down()) &&
                !(ground.getBlock() instanceof LadderBlock) &&
                !(ground.getBlock() instanceof VineBlock) &&
                world.getFluidState(pos).isEmpty()) {
            return false;
        }

        // Feet and head positions must not be solid
        if (feet.isSolidBlock(world, pos) && !(feet.getBlock() instanceof LadderBlock) &&
                !(feet.getBlock() instanceof VineBlock) && world.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (head.isSolidBlock(world, pos.up())) {
            return false;
        }

        return !isVoid(pos);
    }

    private boolean isVoid(BlockPos pos) {
        // Fixed void detection for Minecraft 1.21.4
        String dimensionKey = world.getRegistryKey().getValue().toString();

        int voidLevel;
        switch (dimensionKey) {
            case "minecraft:overworld":
                voidLevel = -64; // Overworld void starts at Y=-64
                break;
            case "minecraft:the_nether":
                voidLevel = 0; // Nether void is at Y=0 and below
                break;
            case "minecraft:the_end":
                voidLevel = -64; // End void starts at Y=-64
                break;
            default:
                voidLevel = -64; // Default void level for custom dimensions
                break;
        }

        // Consider it void if too far below the void level
        if (pos.getY() < voidLevel - 5) {
            return true;
        }

        // Additional void check: if we're below Y=-60 and there's no blocks nearby
        if (pos.getY() < -60) {
            return !hasNearbyBlocks(pos, 5);
        }

        return false;
    }

    private boolean hasNearbyBlocks(BlockPos center, int radius) {
        // Check if there are any solid blocks in the area (avoid void areas)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos checkPos = center.add(dx, dy, dz);
                    if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isJumpPathClear(BlockPos from, BlockPos to) {
        // Simple arc check for jumping
        Vec3d start = Vec3d.ofCenter(from).add(0, 1.8, 0); // Player eye level
        Vec3d end = Vec3d.ofCenter(to).add(0, 1.8, 0);
        Vec3d direction = end.subtract(start);

        int steps = Math.max(1, (int) (direction.length() * 2));
        for (int i = 1; i < steps; i++) {
            double progress = (double) i / steps;
            Vec3d checkPoint = start.add(direction.multiply(progress));

            // Add jump arc (parabolic path)
            double jumpHeight = 4.0 * progress * (1 - progress); // Peak at 50% progress
            checkPoint = checkPoint.add(0, jumpHeight, 0);

            BlockPos checkPos = BlockPos.ofFloored(checkPoint);
            if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasValidLandingArea(BlockPos pos) {
        // Check if there's enough space around the landing position
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = pos.add(dx, 0, dz);
                if (!isPositionSafe(checkPos)) {
                    continue; // Not all positions need to be valid
                }
            }
        }

        return true; // At least some valid landing area
    }

    private boolean isDirectPathClear(BlockPos from, BlockPos to) {
        Vec3d start = Vec3d.ofCenter(from);
        Vec3d end = Vec3d.ofCenter(to);
        Vec3d direction = end.subtract(start);

        int steps = Math.max(1, (int) direction.length());
        for (int i = 1; i < steps; i++) {
            Vec3d checkPoint = start.add(direction.multiply((double) i / steps));
            BlockPos checkPos = BlockPos.ofFloored(checkPoint);

            if (!isPositionSafe(checkPos)) {
                return false;
            }
        }

        return true;
    }
}