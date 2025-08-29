package dev.sxmurxy.mre.client.pathfinding;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class responsible for simplifying and smoothing raw path data. It
 * transforms a jagged, block-by-block path into a fluid, high-resolution
 * curve that the navigation agent can follow gracefully.
 */
public class PathSmoother {

    /**
     * A record to hold the final smoothed path and its corresponding move types.
     */
    public record SmoothedPath(List<Vec3d> path, List<Pathfinder.MoveType> moveTypes) {}

    /**
     * Simplifies a raw path by removing redundant nodes using a line-of-sight check.
     * This is a "string-pulling" algorithm that creates a much shorter list of key waypoints.
     * @param rawPath The original, blocky path from the A* search.
     * @param simplifier A functional interface providing the line-of-sight check.
     * @return A much shorter list of strategically important PathNodes.
     */
    public static List<Pathfinder.PathNode> simplify(List<Pathfinder.PathNode> rawPath, PathSimplifier simplifier) {
        if (rawPath.size() < 3) return rawPath;
        List<Pathfinder.PathNode> simplified = new ArrayList<>();
        simplified.add(rawPath.get(0));
        int lastNodeIdx = 0;
        for (int i = 2; i < rawPath.size(); i++) {
            // If we can no longer see the next node from our last waypoint,
            // it means the previous node was a necessary corner. Add it.
            if (!simplifier.canSee(rawPath.get(lastNodeIdx).pos, rawPath.get(i).pos)) {
                simplified.add(rawPath.get(i - 1));
                lastNodeIdx = i - 1;
            }
        }
        // Always add the very last node to ensure we reach the destination.
        simplified.add(rawPath.get(rawPath.size() - 1));
        return simplified;
    }

    /**
     * Smooths a simplified path by interpolating between its points.
     * @param points The simplified list of waypoints.
     * @param segments The number of interpolated points to generate between each waypoint.
     * @return A SmoothedPath record containing the high-resolution curve and move types.
     */
    public static SmoothedPath smooth(List<Pathfinder.PathNode> points, int segments) {
        List<Vec3d> path = new ArrayList<>();
        List<Pathfinder.MoveType> moveTypes = new ArrayList<>();
        if (points.size() < 2) return new SmoothedPath(path, moveTypes);

        for (int i = 0; i < points.size() - 1; i++) {
            Pathfinder.PathNode p1 = points.get(i);
            Pathfinder.PathNode p2 = points.get(i + 1);
            for (int j = 0; j < segments; j++) {
                float t = (float) j / segments;
                // Linearly interpolate between the center of the two waypoint blocks
                path.add(Vec3d.ofCenter(p1.pos).lerp(Vec3d.ofCenter(p2.pos), t));
                // The move type for the segment is determined by the starting waypoint
                moveTypes.add(p1.move);
            }
        }
        // Add the final point and its move type
        path.add(Vec3d.ofCenter(points.get(points.size() - 1).pos));
        moveTypes.add(points.get(points.size() - 1).move);
        return new SmoothedPath(path, moveTypes);
    }

    /**
     * A functional interface to allow the Pathfinder's `canSee` method to be
     * passed into the static `simplify` method.
     */
    @FunctionalInterface
    public interface PathSimplifier {
        boolean canSee(BlockPos a, BlockPos b);
    }
}
