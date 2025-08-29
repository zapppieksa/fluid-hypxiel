package dev.sxmurxy.mre.modules.command;

import dev.sxmurxy.mre.modules.pathfinding.PathfindingModule;
import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Enhanced pathfinding command with comprehensive options and settings
 * Uses only PathfinderConfig constants (no GUI settings dependency)
 *
 * Features:
 * - Basic pathfinding to coordinates
 * - Status checking and monitoring
 * - Performance statistics display
 * - Emergency controls and debugging
 * - Configuration information display
 * - Real-time pathfinding status
 */
public class PathfindCommand extends Command {

    public PathfindCommand() {
        super("pathfind", "Advanced 3D pathfinding with humanization", getUsageString());
    }

    private static String getUsageString() {
        return """
            §6Pathfinding Commands:
            §b.pathfind <x> <y> <z> §7- Navigate to coordinates
            §b.pathfind stop §7- Stop current pathfinding
            §b.pathfind status §7- Show detailed status
            §b.pathfind recalc §7- Recalculate current path
            §b.pathfind here §7- Pathfind to current position (test)
            
            §6Information:
            §b.pathfind stats §7- Show performance statistics
            §b.pathfind config §7- Display current configuration
            §b.pathfind debug §7- Show debug information
            §b.pathfind help §7- Show this help message
            
            §6Emergency:
            §b.pathfind emergency §7- Emergency stop with key reset
            §b.pathfind reset §7- Reset engine and clear cache
            
            §7Note: Settings are configured via PathfinderConfig constants""";
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            sendUsage();
            return;
        }

        PathfindingModule pathfinder = PathfindingModule.getInstance();
        if (pathfinder == null) {
            sendMessage("§c[Pathfinder] Module not initialized. Enable the pathfinder module first.");
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> sendUsage();
            case "stop" -> handleStop(pathfinder);
            case "status" -> handleStatus(pathfinder);
            case "recalc" -> handleRecalculate(pathfinder);
            case "here" -> handlePathToHere(pathfinder);
            case "stats" -> handleStats(pathfinder);
            case "config" -> handleConfig();
            case "debug" -> handleDebug(pathfinder);
            case "emergency" -> handleEmergency(pathfinder);
            case "reset" -> handleReset(pathfinder);
            default -> handleCoordinatePathfind(args, pathfinder);
        }
    }

    /**
     * Handle pathfinding to specific coordinates
     */
    private void handleCoordinatePathfind(String[] args, PathfindingModule pathfinder) {
        if (args.length < 3) {
            sendMessage("§c[Pathfinder] Usage: .pathfind <x> <y> <z>");
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);

            BlockPos destination = new BlockPos(x, y, z);

            sendMessage("§a[Pathfinder] §7Starting pathfinding to §f" + destination.toShortString());
            sendMessage("§7Using configuration: Max Distance=" + PathfinderConfig.MAX_SEARCH_DISTANCE +
                    ", Max Fall=" + PathfinderConfig.MAX_FALL_DISTANCE +
                    ", Humanization=§aEnabled");

            PathfindingModule.walkTo(destination);

        } catch (NumberFormatException e) {
            sendMessage("§c[Pathfinder] Invalid coordinates. Use integers only.");
        }
    }

    /**
     * Handle stop command
     */
    private void handleStop(PathfindingModule pathfinder) {
        if (PathfindingModule.isPathing()) {
            PathfindingModule.stopPathfinding();
            sendMessage("§c[Pathfinder] §7Pathfinding stopped.");
        } else {
            sendMessage("§e[Pathfinder] §7No active pathfinding to stop.");
        }
    }

    /**
     * Handle status command
     */
    private void handleStatus(PathfindingModule pathfinder) {
        sendMessage("§6[Pathfinder] Status Report:");

        if (!pathfinder.isToggled()) {
            sendMessage("§c  Module: Disabled");
            sendMessage("§7  Enable the module to start pathfinding.");
            return;
        }

        sendMessage("§a  Module: Enabled");

        if (PathfindingModule.isPathing()) {
            sendMessage("§a  Status: Active Pathfinding");

            var currentPath = PathfindingModule.getCurrentPath();
            if (currentPath != null && !currentPath.isEmpty()) {
                sendMessage("§7  Current path: " + currentPath.size() + " waypoints");

                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null && !currentPath.isEmpty()) {
                    double distanceToNext = mc.player.getPos().distanceTo(currentPath.get(0));
                    sendMessage("§7  Distance to next waypoint: " + String.format("%.1f", distanceToNext) + " blocks");
                }
            }

            sendMessage("§7  Path rendering: " + (pathfinder.isToggled() ? "§aEnabled" : "§cDisabled"));
        } else {
            sendMessage("§e  Status: Idle");
            sendMessage("§7  Use .pathfind <x> <y> <z> to start pathfinding");
        }

        // Show configuration summary
        sendMessage("§6  Configuration (from PathfinderConfig):");
        sendMessage("§7    Max Search Distance: §f" + PathfinderConfig.MAX_SEARCH_DISTANCE);
        sendMessage("§7    Max Fall Distance: §f" + PathfinderConfig.MAX_FALL_DISTANCE);
        sendMessage("§7    Path Smoothness: §f" + PathfinderConfig.PATH_SMOOTHNESS);
        sendMessage("§7    Humanization: §aEnabled");
    }

    /**
     * Handle recalculate command
     */
    private void handleRecalculate(PathfindingModule pathfinder) {
        if (!PathfindingModule.isPathing()) {
            sendMessage("§e[Pathfinder] §7No active path to recalculate.");
            return;
        }

        sendMessage("§e[Pathfinder] §7Recalculating path...");
        // Note: In the actual implementation, you'd need to store the destination
        // This is a simplified version
        sendMessage("§7Use §b.pathfind stop §7and set a new destination to recalculate.");
    }

    /**
     * Handle pathfind to current position (for testing)
     */
    private void handlePathToHere(PathfindingModule pathfinder) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            sendMessage("§c[Pathfinder] Player not found.");
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos testDestination = playerPos.add(10, 0, 10);

        sendMessage("§e[Pathfinder] §7Testing pathfinding to nearby location: " + testDestination.toShortString());
        PathfindingModule.walkTo(testDestination);
    }

    /**
     * Handle statistics command
     */
    private void handleStats(PathfindingModule pathfinder) {
        sendMessage("§6[Pathfinder] Performance Statistics:");

        // Module-level statistics
        String moduleStats = PathfindingModule.getStatistics();
        String[] lines = moduleStats.split("\n");
        for (String line : lines) {
            sendMessage("  " + line);
        }

        // Engine-level statistics
        if (pathfinder.engine != null) {
            sendMessage("§6  Engine Performance:");
            String engineStats = pathfinder.engine.getPerformanceStats();
            String[] engineLines = engineStats.split("\n");
            for (String line : engineLines) {
                sendMessage("    §7" + line);
            }
        }
    }

    /**
     * Handle configuration display
     */
    private void handleConfig() {
        sendMessage("§6[Pathfinder] Current Configuration (PathfinderConfig constants):");

        // Core pathfinding settings
        sendMessage("§e  Core Settings:");
        sendMessage("§7    Max Fall Distance: §f" + PathfinderConfig.MAX_FALL_DISTANCE);
        sendMessage("§7    Path Smoothness: §f" + PathfinderConfig.PATH_SMOOTHNESS);
        sendMessage("§7    Max Search Distance: §f" + PathfinderConfig.MAX_SEARCH_DISTANCE);
        sendMessage("§7    Recalculate Stuck Ticks: §f" + PathfinderConfig.RECALCULATE_STUCK_TICKS);
        sendMessage("§7    Max Cached Nodes: §f" + PathfinderConfig.MAX_CACHED_NODES);

        // Movement humanization
        sendMessage("§e  Movement Humanization:");
        sendMessage("§7    Base Variation: §f" + PathfinderConfig.MOVEMENT_VARIATION_BASE);
        sendMessage("§7    Wiggle Intensity: §f" + PathfinderConfig.MOVEMENT_WIGGLE_INTENSITY);
        sendMessage("§7    Base Delay: §f" + PathfinderConfig.MOVEMENT_BASE_DELAY_MS + "ms");
        sendMessage("§7    Delay Variation: §f" + PathfinderConfig.MOVEMENT_DELAY_VARIATION_MS + "ms");

        // Rotation humanization
        sendMessage("§e  Rotation Humanization:");
        sendMessage("§7    Base Speed: §f" + PathfinderConfig.ROTATION_BASE_SPEED);
        sendMessage("§7    Look Ahead Distance: §f" + PathfinderConfig.ROTATION_LOOK_AHEAD_DISTANCE);
        sendMessage("§7    Noise Intensity: §f" + PathfinderConfig.ROTATION_NOISE_INTENSITY);
        sendMessage("§7    Base Delay: §f" + PathfinderConfig.ROTATION_BASE_DELAY_MS + "ms");

        // Special movement
        sendMessage("§e  Special Movement:");
        sendMessage("§7    Jump Chance: §f" + (PathfinderConfig.JUMP_CHANCE_PER_TICK * 100) + "%");
        sendMessage("§7    Sprint Distance: §f" + PathfinderConfig.SPRINT_DISTANCE_THRESHOLD);
        sendMessage("§7    Sneak Distance: §f" + PathfinderConfig.SNEAK_DISTANCE_THRESHOLD);

        sendMessage("§7Note: These settings are configured via PathfinderConfig constants");
        sendMessage("§7and cannot be changed from commands. Modify the config file to adjust.");
    }

    /**
     * Handle debug information
     */
    private void handleDebug(PathfindingModule pathfinder) {
        sendMessage("§6[Pathfinder] Debug Information:");

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            BlockPos playerPos = mc.player.getBlockPos();
            sendMessage("§7  Player Position: " + playerPos.toShortString());
        }

        sendMessage("§7  Module State: " + (pathfinder.isToggled() ? "§aEnabled" : "§cDisabled"));
        sendMessage("§7  Pathfinding Active: " + (PathfindingModule.isPathing() ? "§aYes" : "§cNo"));

        var currentPath = PathfindingModule.getCurrentPath();
        if (currentPath != null) {
            sendMessage("§7  Current Path: " + currentPath.size() + " waypoints");
            if (!currentPath.isEmpty()) {
                sendMessage("§7  Next Waypoint: " +
                        BlockPos.ofFloored(currentPath.get(0)).toShortString());
            }
        } else {
            sendMessage("§7  Current Path: None");
        }

        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        sendMessage("§7  Memory Usage: " + usedMemory + "MB / " + maxMemory + "MB");
    }

    /**
     * Handle emergency stop
     */
    private void handleEmergency(PathfindingModule pathfinder) {
        sendMessage("§c[Pathfinder] §lEMERGENCY STOP ACTIVATED");

        // Stop pathfinding
        PathfindingModule.stopPathfinding();

        // Reset movement keys
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options != null) {
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                mc.options.sneakKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
            }
        } catch (Exception e) {
            sendMessage("§c[Pathfinder] Error resetting keys: " + e.getMessage());
        }

        // Cancel any running pathfinding operations
        if (pathfinder.engine != null) {
            pathfinder.engine.cancelPathfinding();
        }

        sendMessage("§c[Pathfinder] All movement stopped and keys reset.");
        sendMessage("§7You can now move manually or restart pathfinding.");
    }

    /**
     * Handle reset command
     */
    private void handleReset(PathfindingModule pathfinder) {
        sendMessage("§e[Pathfinder] Resetting pathfinding engine...");

        // Stop current pathfinding
        PathfindingModule.stopPathfinding();

        // Reset engine statistics and cache
        if (pathfinder.engine != null) {
            pathfinder.engine.reset();
        }

        sendMessage("§a[Pathfinder] Engine reset complete.");
        sendMessage("§7  - Cache cleared");
        sendMessage("§7  - Statistics reset");
        sendMessage("§7  - All paths cleared");
    }

    /**
     * Send usage information
     */
    public void sendUsage() {
        sendMessage(getUsageString());
        sendMessage("\n§7Current PathfinderConfig settings:");
        sendMessage("§7  Max Distance: §f" + PathfinderConfig.MAX_SEARCH_DISTANCE);
        sendMessage("§7  Humanization: §aEnabled §7(via config constants)");
    }

    /**
     * Send message to player
     */
    public void sendMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        } else {
            System.out.println(message); // Fallback for debugging
        }
    }
}