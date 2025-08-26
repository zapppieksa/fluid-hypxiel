package dev.sxmurxy.mre.modules.command;

import dev.sxmurxy.mre.modules.pathfinding.PathfindingModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Enhanced pathfinding command with comprehensive options and settings
 *
 * Features:
 * - Basic pathfinding to coordinates
 * - Status checking and monitoring
 * - Real-time configuration changes
 * - Performance statistics
 * - Emergency controls
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
            
            §6Settings:
            §b.pathfind smooth <0.0-1.0> §7- Set path smoothness
            §b.pathfind humanize <on|off> §7- Toggle humanization
            §b.pathfind show <on|off> §7- Toggle path visualization
            §b.pathfind speed <0.1-2.0> §7- Set movement speed
            §b.pathfind fall <1-10> §7- Set max fall distance
            
            §6Advanced:
            §b.pathfind stats §7- Show performance statistics
            §b.pathfind debug §7- Toggle debug information
            §b.pathfind reset §7- Reset all settings to default""";
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

        String command = args[0].toLowerCase();

        switch (command) {
            case "stop", "cancel" -> handleStopCommand();
            case "status" -> handleStatusCommand(pathfinder);
            case "recalc", "recalculate" -> handleRecalcCommand(pathfinder);
            case "here" -> handleHereCommand();
            case "smooth", "smoothness" -> handleSmoothCommand(pathfinder, args);
            case "humanize", "human" -> handleHumanizeCommand(pathfinder, args);
            case "show", "render" -> handleShowCommand(pathfinder, args);
            case "speed" -> handleSpeedCommand(pathfinder, args);
            case "fall", "falldist" -> handleFallCommand(pathfinder, args);
            case "stats", "performance" -> handleStatsCommand(pathfinder);
            case "debug" -> handleDebugCommand(pathfinder);
            case "reset" -> handleResetCommand(pathfinder);
            case "help" -> sendUsage();
            default -> handleCoordinateCommand(args);
        }
    }

    private void handleStopCommand() {
        if (PathfindingModule.isPathing()) {
            PathfindingModule.stopPathfinding();
            sendMessage("§a[Pathfinder] §7Pathfinding stopped.");
        } else {
            sendMessage("§c[Pathfinder] §7No active pathfinding to stop.");
        }
    }

    private void handleStatusCommand(PathfindingModule pathfinder) {
        PathfindingModule.PathfindingStats stats = pathfinder.getStats();

        sendMessage("§6╭─── Pathfinder Status ───╮");
        sendMessage("§6│ §7Status: " + stats.getStatusString());

        if (stats.isActive()) {
            sendMessage("§6│ §7Progress: §f" + String.format("%.1f%%", stats.getProgress() * 100));
            sendMessage("§6│ §7Waypoints: §f" + stats.currentWaypointIndex() + "§7/§f" + stats.totalWaypoints());

            if (stats.destination() != null) {
                sendMessage("§6│ §7Destination: §f" + stats.destination().toShortString());
            }

            sendMessage("§6│ §7Duration: §f" + stats.getDurationString());

            if (stats.isStuck()) {
                sendMessage("§6│ §c⚠ Player stuck for " + (stats.stuckTicks() / 20) + "s");
            }
        } else {
            sendMessage("§6│ §7Pathfinder is inactive");
        }

        // Show current settings
        sendMessage("§6│");
        sendMessage("§6│ §7Settings:");

        sendMessage("§6╰─────────────────────────╯");
    }

    private void handleRecalcCommand(PathfindingModule pathfinder) {
        if (PathfindingModule.isPathing()) {
            pathfinder.recalculatePath();
            sendMessage("§a[Pathfinder] §7Recalculating path...");
        } else {
            sendMessage("§c[Pathfinder] §7No active pathfinding to recalculate.");
        }
    }

    private void handleHereCommand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            sendMessage("§c[Pathfinder] §7Player not found.");
            return;
        }

        BlockPos currentPos = mc.player.getBlockPos();
        sendMessage("§7[Pathfinder] Testing pathfinding to current position: §f" + currentPos.toShortString());
        PathfindingModule.walkTo(currentPos);
    }

    private void handleSmoothCommand(PathfindingModule pathfinder, String[] args) {
        if (args.length < 2) {
            sendMessage("§c[Pathfinder] Usage: §7.pathfind smooth <0.0-1.0>");
            return;
        }

        try {
            double smoothness = Double.parseDouble(args[1]);
            if (smoothness < 0.0 || smoothness > 1.0) {
                sendMessage("§c[Pathfinder] Smoothness must be between 0.0 (angular) and 1.0 (very smooth)");
                return;
            }

            pathfinder.setPathSmoothness(smoothness);
            sendMessage("§a[Pathfinder] §7Path smoothness set to §f" + String.format("%.1f", smoothness));

            if (PathfindingModule.isPathing()) {
                sendMessage("§7Changes will apply to new path calculations.");
            }
        } catch (NumberFormatException e) {
            sendMessage("§c[Pathfinder] Invalid number. Use a decimal between 0.0 and 1.0");
        }
    }

    private void handleHumanizeCommand(PathfindingModule pathfinder, String[] args) {
        if (args.length < 2) {
            sendMessage("§c[Pathfinder] Usage: §7.pathfind humanize <on|off>");
            return;
        }

        boolean enable = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("1");
        pathfinder.setHumanizeMovement(enable);

        sendMessage("§a[Pathfinder] §7Movement humanization " + (enable ? "§aenabled" : "§cdisabled"));

        if (enable) {
            sendMessage("§7• Added random variations and delays");
            sendMessage("§7• Natural-looking rotations and movement");
        } else {
            sendMessage("§7• Direct, efficient movement");
            sendMessage("§7• Faster but more robotic");
        }
    }

    private void handleShowCommand(PathfindingModule pathfinder, String[] args) {
        if (args.length < 2) {
            sendMessage("§c[Pathfinder] Usage: §7.pathfind show <on|off>");
            return;
        }

        boolean show = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("1");
        pathfinder.setShowPath(show);

        sendMessage("§a[Pathfinder] §7Path visualization " + (show ? "§aenabled" : "§cdisabled"));

        if (show) {
            sendMessage("§7• Blue semi-transparent nodes");
            sendMessage("§7• Connecting lines between waypoints");
        }
    }

    private void handleSpeedCommand(PathfindingModule pathfinder, String[] args) {
        if (args.length < 2) {
            sendMessage("§c[Pathfinder] Usage: §7.pathfind speed <0.1-2.0>");
            return;
        }

        try {
            double speed = Double.parseDouble(args[1]);
            if (speed < 0.1 || speed > 2.0) {
                sendMessage("§c[Pathfinder] Speed must be between 0.1 (slow) and 2.0 (fast)");
                return;
            }

            pathfinder.setMovementSpeed(speed);
            sendMessage("§a[Pathfinder] §7Movement speed set to §f" + String.format("%.1fx", speed));

            if (speed > 1.5) {
                sendMessage("§e[Pathfinder] §7High speeds may look less natural");
            }
        } catch (NumberFormatException e) {
            sendMessage("§c[Pathfinder] Invalid number. Use a decimal between 0.1 and 2.0");
        }
    }

    private void handleFallCommand(PathfindingModule pathfinder, String[] args) {
        if (args.length < 2) {
            sendMessage("§c[Pathfinder] Usage: §7.pathfind fall <1-10>");
            return;
        }

        try {
            int fallDistance = Integer.parseInt(args[1]);
            if (fallDistance < 1 || fallDistance > 10) {
                sendMessage("§c[Pathfinder] Fall distance must be between 1 and 10 blocks");
                return;
            }

            pathfinder.setMaxFallDistance(fallDistance);
            sendMessage("§a[Pathfinder] §7Max fall distance set to §f" + fallDistance + " blocks");

            if (fallDistance > 4) {
                sendMessage("§e[Pathfinder] §7High fall distances may cause damage");
            }
        } catch (NumberFormatException e) {
            sendMessage("§c[Pathfinder] Invalid number. Use a whole number between 1 and 10");
        }
    }

    private void handleStatsCommand(PathfindingModule pathfinder) {
        var stats = pathfinder.getStats();
        var engineStats = pathfinder.engine.getPerformanceStats();

        sendMessage("§6╭─── Performance Statistics ───╮");
        sendMessage("§6│ §7Pathfinding Performance:");
        sendMessage("§6│ §7• " + engineStats.getPerformanceString());
        sendMessage("§6│ §7• Status: " + (engineStats.isPerformanceGood() ? "§aGood" : "§eNeeds optimization"));

        if (stats.totalWaypoints() > 0) {
            sendMessage("§6│");
            sendMessage("§6│ §7Current Path:");
            sendMessage("§6│ §7• Total waypoints: §f" + stats.totalWaypoints());
            sendMessage("§6│ §7• Completed: §f" + stats.currentWaypointIndex() + " §7(§f" + String.format("%.1f%%", stats.getProgress() * 100) + "§7)");
            sendMessage("§6│ §7• Time elapsed: §f" + stats.getDurationString());
        }

        sendMessage("§6│");
        sendMessage("§6│ §7Engine Information:");
        sendMessage("§6│ §7• Total calculations: §f" + engineStats.totalCalculations());
        sendMessage("§6│ §7• Avg calc time: §f" + String.format("%.0fms", engineStats.averageCalculationTime()));

        if (engineStats.totalCalculations() > 0) {
            sendMessage("§6│ §7• Calc rate: §f" + String.format("%.1f/sec", engineStats.getCalculationsPerSecond()));
        }

        sendMessage("§6╰───────────────────────────────╯");
    }

    private void handleDebugCommand(PathfindingModule pathfinder) {
        // Toggle debug mode (you'd need to add this to PathfindingModule)
        sendMessage("§7[Pathfinder] Debug information:");

        if (PathfindingModule.isPathing()) {
            var currentPath = PathfindingModule.getCurrentPath();
            if (currentPath != null) {
                sendMessage("§7• Current path has §f" + currentPath.size() + "§7 waypoints");

                if (MinecraftClient.getInstance().player != null) {
                    var playerPos = MinecraftClient.getInstance().player.getPos();
                    var closestWaypoint = currentPath.get(0);
                    double distance = playerPos.distanceTo(closestWaypoint);
                    sendMessage("§7• Distance to next waypoint: §f" + String.format("%.2f", distance) + "§7 blocks");
                }
            }
        } else {
            sendMessage("§7• No active pathfinding");
        }

        sendMessage("§7• Module settings:");
    }

    private void handleResetCommand(PathfindingModule pathfinder) {
        // Reset all settings to default
        pathfinder.setPathSmoothness(0.3);
        pathfinder.setMovementSpeed(1.0);
        pathfinder.setRotationSpeed(2.5);
        pathfinder.setMaxFallDistance(4.0);
        pathfinder.setHumanizeMovement(true);
        pathfinder.setShowPath(true);
        pathfinder.setAvoidHazards(true);
        pathfinder.setAllowParkour(true);
        pathfinder.setAllowClimbing(true);

        sendMessage("§a[Pathfinder] §7All settings reset to default values.");
        sendMessage("§7• Path smoothness: §f0.3");
        sendMessage("§7• Movement speed: §f1.0x");
        sendMessage("§7• Max fall distance: §f4 blocks");
        sendMessage("§7• All features enabled");
    }

    private void handleCoordinateCommand(String[] args) {
        if (args.length < 3) {
            sendUsage();
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);

            BlockPos targetPos = new BlockPos(x, y, z);

            // Validate coordinates
            if (!isValidCoordinate(targetPos)) {
                return;
            }

            // Show distance and time estimate
            showPathfindingInfo(targetPos);

            sendMessage("§a[Pathfinder] §7Starting pathfinding to §f" + targetPos.toShortString() + "§7...");
            PathfindingModule.walkTo(targetPos);

        } catch (NumberFormatException e) {
            sendMessage("§c[Pathfinder] Invalid coordinates. Use whole numbers.");
            sendMessage("§7Example: §b.pathfind 100 64 -200");
        } catch (Exception e) {
            sendMessage("§c[Pathfinder] Error: " + e.getMessage());
        }
    }

    private boolean isValidCoordinate(BlockPos pos) {
        // Check world boundaries
        if (Math.abs(pos.getX()) > 30000000 || Math.abs(pos.getZ()) > 30000000) {
            sendMessage("§c[Pathfinder] Coordinates too far from spawn. Maximum: ±30,000,000");
            return false;
        }

        // Check Y boundaries
        if (pos.getY() < -64 || pos.getY() > 320) {
            sendMessage("§c[Pathfinder] Y coordinate out of range. Valid range: -64 to 320");
            return false;
        }

        return true;
    }

    private void showPathfindingInfo(BlockPos target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        double distance = Math.sqrt(playerPos.getSquaredDistance(target));

        sendMessage("§7[Pathfinder] Distance: §f" + String.format("%.1f", distance) + "§7 blocks");

        if (distance > 100) {
            int estimatedTime = (int) (distance / 4.3); // Rough walking speed estimate
            sendMessage("§7[Pathfinder] Estimated time: §f~" + estimatedTime + "§7 seconds");
        }

        if (distance > 500) {
            sendMessage("§e[Pathfinder] Long distance pathfinding may take time to calculate.");
        }

        if (distance > 1000) {
            sendMessage("§c[Pathfinder] Very long distance! Consider waypoints for better performance.");
        }
    }
}