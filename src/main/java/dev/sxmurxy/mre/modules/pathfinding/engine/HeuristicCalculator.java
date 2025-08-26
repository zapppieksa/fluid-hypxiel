package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

class HeuristicCalculator {

    public double calculate(BlockPos from, BlockPos to) {
        // Enhanced A* heuristic with tie-breaking and better estimates
        double dx = Math.abs(to.getX() - from.getX());
        double dy = Math.abs(to.getY() - from.getY());
        double dz = Math.abs(to.getZ() - from.getZ());

        // 3D Euclidean distance as base
        double h = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Tie-breaking: prefer paths closer to goal center
        double cross = Math.abs(dx * (to.getZ() - from.getZ()) - dz * (to.getX() - from.getX()));
        h += cross * 0.001; // Small tie-breaker

        // Adjust for vertical movement difficulty
        h += Math.abs(dy) * 0.5; // Vertical movement is more expensive

        return h;
    }

    public double calculateWithTerrain(BlockPos from, BlockPos to, World world) {
        double baseHeuristic = calculate(from, to);

        // Add terrain-based adjustments
        double terrainPenalty = 0;

        // Sample terrain along the path
        Vec3d direction = Vec3d.ofCenter(to).subtract(Vec3d.ofCenter(from)).normalize();
        int samples = Math.min(10, (int) Vec3d.ofCenter(from).distanceTo(Vec3d.ofCenter(to)) / 5);

        for (int i = 1; i <= samples; i++) {
            Vec3d samplePoint = Vec3d.ofCenter(from).add(direction.multiply(i * 5));
            BlockPos samplePos = BlockPos.ofFloored(samplePoint);

            // Water penalty
            if (world.getFluidState(samplePos).isOf(Fluids.WATER)) {
                terrainPenalty += 2.0;
            }

            // Lava penalty
            if (world.getFluidState(samplePos).isOf(Fluids.LAVA)) {
                terrainPenalty += 5.0;
            }

            // Height variation penalty
            int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, samplePos.getX(), samplePos.getZ());
            if (Math.abs(samplePos.getY() - surfaceY) > 5) {
                terrainPenalty += 1.0;
            }
        }

        return baseHeuristic + terrainPenalty;
    }
}