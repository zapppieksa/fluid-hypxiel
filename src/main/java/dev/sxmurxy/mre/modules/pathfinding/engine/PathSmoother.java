package dev.sxmurxy.mre.modules.pathfinding.engine;

import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced path smoother that creates natural, human-like movement paths
 *
 * Features:
 * - Catmull-Rom spline interpolation for smooth curves
 * - Line-of-sight optimization
 * - Adaptive smoothing based on path complexity
 * - Humanization variations
 * - Obstacle-aware curve generation
 */
public class PathSmoother {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Smooth the path to create more natural movement
     */
    public static List<Vec3d> smoothPath(List<PathNode> rawPath, double smoothness) {
        if (rawPath == null || rawPath.size() < 2) {
            return convertNodesToVec3d(rawPath);
        }

        // Convert PathNodes to Vec3d positions
        List<Vec3d> positions = convertNodesToVec3d(rawPath);

        // Apply multi-stage smoothing
        positions = optimizeLineOfSight(positions);
        positions = applyCurveSmoothing(positions, smoothness);
        positions = addHumanizationVariations(positions);
        positions = validateAndAdjustPath(positions);

        return positions;
    }

    /**
     * Convert PathNode list to Vec3d list
     */
    private static List<Vec3d> convertNodesToVec3d(List<PathNode> nodes) {
        if (nodes == null) return new ArrayList<>();

        List<Vec3d> positions = new ArrayList<>();
        for (PathNode node : nodes) {
            positions.add(node.getBottomCenterVec3d());
        }
        return positions;
    }

    /**
     * Optimize path using line-of-sight to remove unnecessary waypoints
     */
    private static List<Vec3d> optimizeLineOfSight(List<Vec3d> path) {
        if (path.size() < 3) return path;

        List<Vec3d> optimized = new ArrayList<>();
        optimized.add(path.get(0)); // Always keep start

        int current = 0;

        while (current < path.size() - 1) {
            int farthest = current + 1;

            // Find farthest reachable point
            for (int i = current + 2; i < path.size(); i++) {
                if (hasLineOfSight(path.get(current), path.get(i))) {
                    farthest = i;
                } else {
                    break;
                }
            }

            optimized.add(path.get(farthest));
            current = farthest;
        }

        return optimized;
    }

    /**
     * Check if there's clear line of sight between two points
     */
    private static boolean hasLineOfSight(Vec3d from, Vec3d to) {
        if (mc.world == null) return false;

        // Don't optimize very long segments
        if (from.distanceTo(to) > 15) return false;

        Vec3d adjustedFrom = from.add(0, 1.6, 0); // Player eye height
        Vec3d adjustedTo = to.add(0, 1.6, 0);

        RaycastContext context = new RaycastContext(
                adjustedFrom,
                adjustedTo,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );

        HitResult result = mc.world.raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }

    /**
     * Apply curve smoothing using Catmull-Rom spline interpolation
     */
    private static List<Vec3d> applyCurveSmoothing(List<Vec3d> path, double smoothness) {
        if (path.size() < 3) return path;

        List<Vec3d> smoothed = new ArrayList<>();
        smoothed.add(path.get(0)); // Keep start point

        for (int i = 1; i < path.size() - 1; i++) {
            // Get control points for spline
            Vec3d p0 = i > 1 ? path.get(i - 2) : path.get(i - 1);
            Vec3d p1 = path.get(i - 1);
            Vec3d p2 = path.get(i);
            Vec3d p3 = i < path.size() - 2 ? path.get(i + 2) : path.get(i + 1);

            // Calculate number of intermediate points based on distance
            double segmentLength = p1.distanceTo(p2);
            int intermediatePoints = Math.max(1, (int) (segmentLength * 2 * smoothness));

            // Generate smooth curve points
            for (int j = 1; j <= intermediatePoints; j++) {
                double t = (double) j / (intermediatePoints + 1);
                Vec3d interpolated = catmullRomInterpolation(p0, p1, p2, p3, t);

                // Blend with original point based on smoothness
                Vec3d blended = p2.lerp(interpolated, smoothness);
                smoothed.add(blended);
            }
        }

        smoothed.add(path.get(path.size() - 1)); // Keep end point
        return smoothed;
    }

    /**
     * Catmull-Rom spline interpolation for smooth curves
     */
    private static Vec3d catmullRomInterpolation(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;

        // Catmull-Rom formula for each component
        double x = 0.5 * ((2 * p1.x) +
                (-p0.x + p2.x) * t +
                (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
                (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);

        double y = 0.5 * ((2 * p1.y) +
                (-p0.y + p2.y) * t +
                (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
                (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);

        double z = 0.5 * ((2 * p1.z) +
                (-p0.z + p2.z) * t +
                (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 +
                (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);

        return new Vec3d(x, y, z);
    }

    /**
     * Add human-like variations to the path
     */
    private static List<Vec3d> addHumanizationVariations(List<Vec3d> path) {
        if (path.size() < 2) return path;

        List<Vec3d> varied = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            Vec3d point = path.get(i);

            // Don't vary start and end points too much
            if (i == 0 || i == path.size() - 1) {
                // Small variation for start/end
                double smallVariation = 0.05;
                double xOffset = (Math.random() - 0.5) * smallVariation;
                double zOffset = (Math.random() - 0.5) * smallVariation;
                varied.add(point.add(xOffset, 0, zOffset));
                continue;
            }

            // Calculate variation intensity based on local path complexity
            double complexity = calculateLocalComplexity(path, i);
            double variationIntensity = 0.1 + (complexity * 0.1);

            // Add random offsets
            double xOffset = (Math.random() - 0.5) * variationIntensity;
            double zOffset = (Math.random() - 0.5) * variationIntensity;
            double yOffset = (Math.random() - 0.5) * 0.05; // Less Y variation

            // Add slight lateral drift for natural walking
            Vec3d drift = calculateLateralDrift(path, i);

            Vec3d variedPoint = point.add(xOffset + drift.x, yOffset + drift.y, zOffset + drift.z);
            varied.add(variedPoint);
        }

        return varied;
    }

    /**
     * Calculate local path complexity for adaptive variation
     */
    private static double calculateLocalComplexity(List<Vec3d> path, int index) {
        if (index == 0 || index == path.size() - 1) return 0.0;

        Vec3d prev = path.get(index - 1);
        Vec3d current = path.get(index);
        Vec3d next = path.get(index + 1);

        // Calculate angle between segments
        Vec3d dir1 = current.subtract(prev).normalize();
        Vec3d dir2 = next.subtract(current).normalize();

        double dotProduct = dir1.dotProduct(dir2);
        dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct)); // Clamp for acos

        double angle = Math.acos(dotProduct);
        return angle / Math.PI; // Normalize to [0, 1]
    }

    /**
     * Calculate lateral drift for natural walking patterns
     */
    private static Vec3d calculateLateralDrift(List<Vec3d> path, int index) {
        if (index == 0 || index == path.size() - 1) return Vec3d.ZERO;

        Vec3d prev = path.get(index - 1);
        Vec3d next = path.get(index + 1);
        Vec3d direction = next.subtract(prev).normalize();

        // Create perpendicular vector for lateral movement
        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x);

        // Slight random lateral drift
        double driftAmount = (Math.random() - 0.5) * 0.08;
        return perpendicular.multiply(driftAmount);
    }

    /**
     * Validate and adjust path to ensure it's walkable
     */
    private static List<Vec3d> validateAndAdjustPath(List<Vec3d> path) {
        if (path.size() < 2) return path;

        List<Vec3d> adjusted = new ArrayList<>();

        for (Vec3d point : path) {
            // Ensure points are at walkable Y levels
            Vec3d adjustedPoint = adjustHeightForWalkability(point);
            adjusted.add(adjustedPoint);
        }

        return adjusted;
    }

    /**
     * Adjust point height to ensure it's walkable
     */
    private static Vec3d adjustHeightForWalkability(Vec3d point) {
        if (mc.world == null) return point;

        // Try to find solid ground near the point
        for (int yOffset = -2; yOffset <= 3; yOffset++) {
            Vec3d testPoint = point.add(0, yOffset, 0);
            if (isWalkableHeight(testPoint)) {
                return testPoint;
            }
        }

        return point; // Return original if no adjustment found
    }

    /**
     * Check if a height is walkable (has solid ground below and air above)
     */
    private static boolean isWalkableHeight(Vec3d point) {
        if (mc.world == null) return false;

        // Check for solid ground below
        if (!mc.world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(point.add(0, -1, 0))).isSolidBlock(mc.world, null)) {
            return false;
        }

        // Check for clear space above
        if (!mc.world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(point)).isAir() ||
                !mc.world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(point.add(0, 1, 0))).isAir()) {
            return false;
        }

        return true;
    }

    /**
     * Create curved path around obstacle
     */
    public static List<Vec3d> createCurvedPathAroundObstacle(Vec3d start, Vec3d end, Vec3d obstacle) {
        List<Vec3d> path = new ArrayList<>();

        Vec3d midpoint = start.lerp(end, 0.5);
        Vec3d toObstacle = obstacle.subtract(midpoint);
        Vec3d perpendicular = new Vec3d(-toObstacle.z, 0, toObstacle.x).normalize();

        // Determine which side to curve around
        double curveDirection = Math.random() > 0.5 ? 1.0 : -1.0;
        perpendicular = perpendicular.multiply(curveDirection);

        // Create curve control points
        double curveDistance = Math.max(2.0, start.distanceTo(end) * 0.3);
        Vec3d curvePoint1 = start.lerp(midpoint, 0.33).add(perpendicular.multiply(curveDistance * 0.5));
        Vec3d curvePoint2 = midpoint.add(perpendicular.multiply(curveDistance));
        Vec3d curvePoint3 = end.lerp(midpoint, 0.33).add(perpendicular.multiply(curveDistance * 0.5));

        // Build curved path using bezier interpolation
        int curvePoints = Math.max(5, (int) (start.distanceTo(end) / 3));
        path.add(start);

        for (int i = 1; i < curvePoints; i++) {
            double t = (double) i / curvePoints;
            Vec3d curvePoint = bezierCurveInterpolation(start, curvePoint1, curvePoint2, curvePoint3, end, t);
            path.add(curvePoint);
        }

        path.add(end);
        return path;
    }

    /**
     * Bezier curve interpolation for smooth obstacle avoidance
     */
    private static Vec3d bezierCurveInterpolation(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;
        double uuuu = uuu * u;
        double tttt = ttt * t;

        // Fifth-order Bezier curve
        Vec3d point = p0.multiply(uuuu);
        point = point.add(p1.multiply(4 * uuu * t));
        point = point.add(p2.multiply(6 * uu * tt));
        point = point.add(p3.multiply(4 * u * ttt));
        point = point.add(p4.multiply(tttt));

        return point;
    }

    /**
     * Adaptive smoothing that varies intensity based on path characteristics
     */
    public static List<Vec3d> adaptiveSmooth(List<PathNode> rawPath) {
        if (rawPath == null || rawPath.size() < 3) {
            return convertNodesToVec3d(rawPath);
        }

        List<Vec3d> positions = convertNodesToVec3d(rawPath);
        List<Vec3d> adaptive = new ArrayList<>();

        for (int i = 0; i < positions.size(); i++) {
            Vec3d current = positions.get(i);

            if (i == 0 || i == positions.size() - 1) {
                adaptive.add(current);
                continue;
            }

            // Calculate adaptive smoothness based on local conditions
            double complexity = calculateLocalComplexity(positions, i);
            double localSmoothness = Math.max(0.1, 0.6 - complexity * 0.4);

            Vec3d prev = positions.get(i - 1);
            Vec3d next = positions.get(i + 1);
            Vec3d smoothed = current.lerp(prev.lerp(next, 0.5), localSmoothness);

            adaptive.add(smoothed);
        }

        return adaptive;
    }

    /**
     * Create natural walking rhythm by adjusting point spacing
     */
    public static List<Vec3d> addWalkingRhythm(List<Vec3d> path) {
        if (path.size() < 3) return path;

        List<Vec3d> rhythmic = new ArrayList<>();
        rhythmic.add(path.get(0));

        double walkingPhase = 0;
        double baseStepLength = 0.8; // Average step length

        for (int i = 1; i < path.size(); i++) {
            Vec3d current = path.get(i);
            Vec3d prev = rhythmic.get(rhythmic.size() - 1);

            // Calculate natural step variation
            walkingPhase += 0.3;
            double stepVariation = Math.sin(walkingPhase) * 0.2;
            double adjustedStepLength = baseStepLength + stepVariation;

            // Adjust point position based on step rhythm
            Vec3d direction = current.subtract(prev).normalize();
            Vec3d adjustedPoint = prev.add(direction.multiply(adjustedStepLength));

            rhythmic.add(adjustedPoint);
        }

        return rhythmic;
    }

    /**
     * Get smoothing statistics for debugging
     */
    public static SmoothingStats getSmoothingStats(List<PathNode> original, List<Vec3d> smoothed) {
        if (original == null || smoothed == null) {
            return new SmoothingStats(0, 0, 0, 0, 0);
        }

        double originalDistance = calculatePathDistance(convertNodesToVec3d(original));
        double smoothedDistance = calculatePathDistance(smoothed);
        double compressionRatio = smoothed.size() > 0 ? (double) original.size() / smoothed.size() : 1.0;

        return new SmoothingStats(
                original.size(),
                smoothed.size(),
                originalDistance,
                smoothedDistance,
                compressionRatio
        );
    }

    /**
     * Calculate total path distance
     */
    private static double calculatePathDistance(List<Vec3d> path) {
        if (path.size() < 2) return 0;

        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += path.get(i - 1).distanceTo(path.get(i));
        }
        return total;
    }

    /**
     * Smoothing statistics record
     */
    public record SmoothingStats(
            int originalWaypoints,
            int smoothedWaypoints,
            double originalDistance,
            double smoothedDistance,
            double compressionRatio
    ) {
        public double getDistanceReduction() {
            return originalDistance > 0 ? (originalDistance - smoothedDistance) / originalDistance : 0;
        }

        public double getWaypointReduction() {
            return originalWaypoints > 0 ? (originalWaypoints - smoothedWaypoints) / (double) originalWaypoints : 0;
        }

        public String getSummary() {
            return String.format("Waypoints: %d→%d (%.1f%%), Distance: %.1f→%.1f (%.1f%%)",
                    originalWaypoints, smoothedWaypoints, getWaypointReduction() * 100,
                    originalDistance, smoothedDistance, getDistanceReduction() * 100);
        }
    }
}