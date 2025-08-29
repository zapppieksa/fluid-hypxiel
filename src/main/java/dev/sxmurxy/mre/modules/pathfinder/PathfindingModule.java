package dev.sxmurxy.mre.modules.pathfinder;

import dev.sxmurxy.mre.client.pathfinding.PathfinderAPI;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced pathfinding module with comprehensive features and statistics tracking.
 * Integrates with the new PathfinderAPI for humanized movement and teleportation support.
 */
public class PathfindingModule extends Module {

    private static PathfindingModule instance;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Statistics tracking
    private final AtomicLong pathsCompleted = new AtomicLong(0);
    private final AtomicLong pathsFailed = new AtomicLong(0);
    private long lastPathfindTime = 0;
    private long totalPathfindingTime = 0;

    // Configuration
    private boolean aotvEnabled = true;
    private boolean etherwarpEnabled = true;
    private boolean debugMode = true;
    private PathfindingMode mode = PathfindingMode.OPTIMIZED;

    public enum PathfindingMode {
        WALK_ONLY("Walk Only - No teleportation"),
        OPTIMIZED("Optimized - Smart teleportation"),
        AGGRESSIVE("Aggressive - Maximum speed");

        private final String description;

        PathfindingMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public PathfindingModule() {
        super("Pathfinding", "Advanced pathfinding with humanized movement and teleportation support.", ModuleCategory.MISCELLANEOUS);
        instance = this;
    }

    public static PathfindingModule getInstance() {
        if (instance == null) {
            instance = new PathfindingModule();
        }
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Apply current configuration to PathfinderAPI
        configurePathfinderAPI();

        if (debugMode) {
            System.out.println("Pathfinding module enabled with mode: " + mode.getDescription());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // Stop any active pathfinding
        stopPathfinding();

        if (debugMode) {
            System.out.println("Pathfinding module disabled. Stopped all active pathfinding.");
        }
    }

    @Override
    public void onUpdate() {
        // The PathfinderAPI handles its own ticking through events
        // This method can be used for additional module-specific logic if needed
    }

    /**
     * Primary pathfinding method with full statistics tracking.
     */
    public void pathfindTo(BlockPos target) {
        if (!isToggled()) {
            System.out.println("Pathfinding module is disabled!");
            return;
        }

        if (mc.player == null) {
            System.out.println("Cannot pathfind - player is null!");
            return;
        }

        long startTime = System.currentTimeMillis();

        if (debugMode) {
            BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
            System.out.printf("Starting pathfinding from %s to %s (Mode: %s)%n",
                    playerPos.toShortString(), target.toShortString(), mode.name());
        }

        // Configure PathfinderAPI based on current settings
        configurePathfinderAPI();

        // Execute pathfinding with callback for statistics
        PathfinderAPI.findAndFollowPath(target, success -> {
            long endTime = System.currentTimeMillis();
            lastPathfindTime = endTime - startTime;
            totalPathfindingTime += lastPathfindTime;

            if (success) {
                pathsCompleted.incrementAndGet();
                if (debugMode) {
                    System.out.printf("✓ Pathfinding successful in %dms%n", lastPathfindTime);
                }
            } else {
                pathsFailed.incrementAndGet();
                if (debugMode) {
                    System.out.printf("✗ Pathfinding failed after %dms%n", lastPathfindTime);
                }
            }
        });
    }

    /**
     * Pathfind to player's cursor target.
     */
    public void pathfindToCursor() {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            net.minecraft.util.hit.BlockHitResult blockHit = (net.minecraft.util.hit.BlockHitResult) mc.crosshairTarget;
            BlockPos target = blockHit.getBlockPos().up(); // Stand on top of the block
            pathfindTo(target);
        } else {
            System.out.println("No valid block target found at cursor!");
        }
    }

    /**
     * Stop all pathfinding operations.
     */
    public void stopPathfinding() {
        PathfinderAPI.stop();
        if (debugMode) {
            System.out.println("Pathfinding stopped by user request.");
        }
    }

    /**
     * Check if pathfinding is currently active.
     */
    public boolean isPathfinding() {
        return PathfinderAPI.isActive();
    }

    /**
     * Configure PathfinderAPI based on current module settings.
     */
    private void configurePathfinderAPI() {
        // Apply mode-specific configurations
        switch (mode) {
            case WALK_ONLY -> {
                PathfinderAPI.setAotvEnabled(false);
                PathfinderAPI.setEtherwarpEnabled(false);
                PathfinderAPI.setPathfindingSpeed(0.8); // Slightly slower for precision
            }
            case OPTIMIZED -> {
                PathfinderAPI.setAotvEnabled(aotvEnabled);
                PathfinderAPI.setEtherwarpEnabled(etherwarpEnabled);
                PathfinderAPI.setPathfindingSpeed(1.0); // Normal speed
            }
            case AGGRESSIVE -> {
                PathfinderAPI.setAotvEnabled(true);
                PathfinderAPI.setEtherwarpEnabled(true);
                PathfinderAPI.setPathfindingSpeed(1.5); // Faster pathfinding
            }
        }

        PathfinderAPI.setDebugMode(debugMode);
    }

    /**
     * Get comprehensive pathfinding statistics.
     */
    public PathfindingStats getStatistics() {
        long totalPaths = pathsCompleted.get() + pathsFailed.get();
        double successRate = totalPaths > 0 ? (pathsCompleted.get() / (double) totalPaths) * 100.0 : 0.0;
        long averageTime = totalPaths > 0 ? totalPathfindingTime / totalPaths : 0;

        PathfinderAPI.PathfindingStats apiStats = PathfinderAPI.getStats();

        return new PathfindingStats(
                pathsCompleted.get(),
                pathsFailed.get(),
                totalPaths,
                successRate,
                averageTime,
                lastPathfindTime,
                apiStats.isActive(),
                mode,
                apiStats
        );
    }

    /**
     * Print detailed statistics to console.
     */
    public void printStatistics() {
        PathfindingStats stats = getStatistics();

        System.out.println("§b=== Pathfinding Statistics ===");
        System.out.printf("§7Paths Completed: §a%d%n", stats.pathsCompleted);
        System.out.printf("§7Paths Failed: §c%d%n", stats.pathsFailed);
        System.out.printf("§7Total Paths: §f%d%n", stats.totalPaths);
        System.out.printf("§7Success Rate: §e%.1f%%%n", stats.successRate);
        System.out.printf("§7Average Time: §b%d ms%n", stats.averageTime);
        System.out.printf("§7Last Path Time: §f%d ms%n", stats.lastPathTime);
        System.out.printf("§7Currently Active: %s%n", stats.isActive ? "§aYES" : "§cNO");
        System.out.printf("§7Current Mode: §d%s%n", stats.mode.getDescription());
        System.out.printf("§7AOTV Enabled: %s%n", stats.apiStats.aotvEnabled() ? "§aYES" : "§cNO");
        System.out.printf("§7Etherwarp Enabled: %s%n", stats.apiStats.etherwarpEnabled() ? "§aYES" : "§cNO");
        System.out.printf("§7Debug Mode: %s%n", stats.apiStats.debugMode() ? "§aYES" : "§cNO");
        System.out.println("§b===============================");
    }

    /**
     * Reset all statistics.
     */
    public void resetStatistics() {
        pathsCompleted.set(0);
        pathsFailed.set(0);
        totalPathfindingTime = 0;
        lastPathfindTime = 0;

        if (debugMode) {
            System.out.println("Pathfinding statistics reset.");
        }
    }

    /**
     * Toggle between pathfinding modes.
     */
    public void cycleMode() {
        PathfindingMode[] modes = PathfindingMode.values();
        int currentIndex = mode.ordinal();
        int nextIndex = (currentIndex + 1) % modes.length;
        setPathfindingMode(modes[nextIndex]);
    }

    // Configuration methods
    public void setAotvEnabled(boolean enabled) {
        this.aotvEnabled = enabled;
        PathfinderAPI.setAotvEnabled(enabled);
        if (debugMode) {
            System.out.println("AOTV teleportation: " + (enabled ? "ENABLED" : "DISABLED"));
        }
    }

    public void setEtherwarpEnabled(boolean enabled) {
        this.etherwarpEnabled = enabled;
        PathfinderAPI.setEtherwarpEnabled(enabled);
        if (debugMode) {
            System.out.println("Etherwarp teleportation: " + (enabled ? "ENABLED" : "DISABLED"));
        }
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        PathfinderAPI.setDebugMode(enabled);
        System.out.println("Pathfinding debug mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public void setPathfindingMode(PathfindingMode mode) {
        this.mode = mode;
        configurePathfinderAPI(); // Apply new mode settings
        if (debugMode) {
            System.out.println("Pathfinding mode changed to: " + mode.getDescription());
        }
    }

    // Getters
    public long getPathsCompleted() { return pathsCompleted.get(); }
    public long getPathsFailed() { return pathsFailed.get(); }
    public long getLastPathfindTime() { return lastPathfindTime; }
    public boolean isAotvEnabled() { return aotvEnabled; }
    public boolean isEtherwarpEnabled() { return etherwarpEnabled; }
    public boolean isDebugMode() { return debugMode; }
    public PathfindingMode getPathfindingMode() { return mode; }

    /**
     * Enhanced statistics data class with API integration.
     */
    public record PathfindingStats(
            long pathsCompleted,
            long pathsFailed,
            long totalPaths,
            double successRate,
            long averageTime,
            long lastPathTime,
            boolean isActive,
            PathfindingMode mode,
            PathfinderAPI.PathfindingStats apiStats
    ) {}
}