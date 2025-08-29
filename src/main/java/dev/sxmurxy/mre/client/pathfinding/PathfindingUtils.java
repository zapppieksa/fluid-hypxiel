package dev.sxmurxy.mre.client.pathfinding;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.Random;

/**
 * Utility class for pathfinding calculations and helper functions.
 */
public class PathfindingUtils {
    private static final Random random = new Random();

    /**
     * Calculate 3D Euclidean distance between two positions.
     */
    public static double distance3D(Vec3d pos1, Vec3d pos2) {
        double dx = pos2.x - pos1.x;
        double dy = pos2.y - pos1.y;
        double dz = pos2.z - pos1.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculate Manhattan distance (grid distance) between two block positions.
     */
    public static int manhattanDistance(BlockPos pos1, BlockPos pos2) {
        return Math.abs(pos1.getX() - pos2.getX()) +
                Math.abs(pos1.getY() - pos2.getY()) +
                Math.abs(pos1.getZ() - pos2.getZ());
    }

    /**
     * Calculate octile distance (diagonal-aware) for better pathfinding heuristics.
     */
    public static double octileDistance(BlockPos pos1, BlockPos pos2) {
        double dx = Math.abs(pos1.getX() - pos2.getX());
        double dy = Math.abs(pos1.getY() - pos2.getY());
        double dz = Math.abs(pos1.getZ() - pos2.getZ());

        double dMin = Math.min(dx, Math.min(dy, dz));
        double dMed = Math.max(Math.min(dx, dy), Math.min(Math.max(dx, dy), dz));
        double dMax = Math.max(dx, Math.max(dy, dz));

        return (Math.sqrt(3) - Math.sqrt(2)) * dMin +
                (Math.sqrt(2) - 1) * dMed + dMax;
    }

    /**
     * Linear interpolation between two Vec3d points.
     */
    public static Vec3d lerp(Vec3d start, Vec3d end, double t) {
        t = Math.max(0.0, Math.min(1.0, t)); // Clamp t to [0, 1]
        return start.add(end.subtract(start).multiply(t));
    }

    /**
     * Smooth step interpolation for more natural curves.
     */
    public static double smoothStep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Add humanized randomness to a value.
     */
    public static double addHumanizedNoise(double value, double noiseFactor) {
        double noise = (random.nextGaussian() - 0.5) * noiseFactor;
        return value + noise;
    }

    /**
     * Add humanized randomness to a Vec3d.
     */
    public static Vec3d addHumanizedNoise(Vec3d vec, double noiseFactor) {
        return new Vec3d(
                addHumanizedNoise(vec.x, noiseFactor),
                addHumanizedNoise(vec.y, noiseFactor * 0.5), // Less Y noise
                addHumanizedNoise(vec.z, noiseFactor)
        );
    }

    /**
     * Calculate angle between two vectors in degrees.
     */
    public static double angleBetweenVectors(Vec3d v1, Vec3d v2) {
        double dot = v1.normalize().dotProduct(v2.normalize());
        dot = Math.max(-1.0, Math.min(1.0, dot)); // Clamp to avoid NaN
        return Math.toDegrees(Math.acos(dot));
    }

    /**
     * Check if a position is within a reasonable range for pathfinding.
     */
    public static boolean isValidPosition(Vec3d pos) {
        return pos.y >= -64 && pos.y <= 320 && // Y bounds
                Math.abs(pos.x) <= 30000000 && // X bounds
                Math.abs(pos.z) <= 30000000;   // Z bounds
    }

    /**
     * Clamp a value between min and max.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp a float between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Generate a random float between min and max.
     */
    public static float randomFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    /**
     * Generate a random double between min and max.
     */
    public static double randomDouble(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /**
     * Check if two positions are approximately equal within tolerance.
     */
    public static boolean approximatelyEqual(Vec3d pos1, Vec3d pos2, double tolerance) {
        return distance3D(pos1, pos2) <= tolerance;
    }

    /**
     * Get the horizontal distance (ignoring Y) between two positions.
     */
    public static double horizontalDistance(Vec3d pos1, Vec3d pos2) {
        double dx = pos2.x - pos1.x;
        double dz = pos2.z - pos1.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Convert ticks to milliseconds.
     */
    public static long ticksToMillis(int ticks) {
        return ticks * 50L; // 20 TPS = 50ms per tick
    }

    /**
     * Convert milliseconds to ticks.
     */
    public static int millisToTicks(long millis) {
        return (int) (millis / 50L);
    }

    /**
     * Format a position as a readable string.
     */
    public static String formatPosition(Vec3d pos) {
        return String.format("(%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z);
    }

    /**
     * Format a block position as a readable string.
     */
    public static String formatBlockPos(BlockPos pos) {
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
}