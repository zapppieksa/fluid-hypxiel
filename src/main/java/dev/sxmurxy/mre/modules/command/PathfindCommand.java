package dev.sxmurxy.mre.modules.command;

import dev.sxmurxy.mre.client.pathfinding.PathfinderAPI;
import dev.sxmurxy.mre.modules.pathfinder.PathfindingModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Comprehensive command interface for the advanced pathfinding system.
 * Provides full control over pathfinding operations, configuration, and statistics.
 */
public class PathfindCommand extends Command {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public PathfindCommand() {
        super("pathfind", "Advanced pathfinding with humanized movement and teleportation.",
                ".pathfind <x y z|here|stop|stats|config|help>");
    }

    @Override
    public void execute(String[] args) {
        PathfindingModule pathfinding = PathfindingModule.getInstance();

        if (args.length < 1) {
            handleHelp();
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "stop" -> handleStop(pathfinding);
            case "here", "cursor" -> handlePathfindToCursor(pathfinding);
            case "stats", "statistics" -> handleStats(pathfinding, args);
            case "config", "cfg" -> handleConfig(pathfinding, args);
            case "mode" -> handleMode(pathfinding, args);
            case "test" -> handleTest(pathfinding);
            case "help" -> handleHelp();
            default -> handleCoordinates(pathfinding, args);
        }
    }

    /**
     * Handle pathfinding to coordinates.
     */
    private void handleCoordinates(PathfindingModule pathfinding, String[] args) {
        if (args.length < 3) {
            sendMessage("§cUsage: .pathfind <x> <y> <z>");
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            BlockPos target = new BlockPos(x, y, z);

            sendMessage(String.format("§aPathfinding to %s", target.toShortString()));
            pathfinding.pathfindTo(target);

        } catch (NumberFormatException e) {
            sendMessage("§cInvalid coordinates. Use integers only.");
        }
    }

    /**
     * Handle stopping pathfinding.
     */
    private void handleStop(PathfindingModule pathfinding) {
        pathfinding.stopPathfinding();
        sendMessage("§ePathfinding stopped.");
    }

    /**
     * Handle pathfinding to cursor target.
     */
    private void handlePathfindToCursor(PathfindingModule pathfinding) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
            BlockPos target = blockHit.getBlockPos().up();

            sendMessage(String.format("§aPathfinding to cursor target: %s", target.toShortString()));
            pathfinding.pathfindTo(target);
        } else {
            sendMessage("§cYou are not looking at a block.");
        }
    }

    /**
     * Handle statistics display and management.
     */
    private void handleStats(PathfindingModule pathfinding, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
            pathfinding.resetStatistics();
            sendMessage("§aPathfinding statistics reset.");
            return;
        }

        PathfindingModule.PathfindingStats stats = pathfinding.getStatistics();
        PathfinderAPI.PathfindingStats apiStats = PathfinderAPI.getStats();

        sendMessage("§b=== Pathfinding Statistics ===");
        sendMessage(String.format("§7Paths Completed: §a%d", stats.pathsCompleted()));
        sendMessage(String.format("§7Paths Failed: §c%d", stats.pathsFailed()));
        sendMessage(String.format("§7Success Rate: §e%.1f%%", stats.successRate()));
        sendMessage(String.format("§7Average Time: §b%dms", stats.averageTime()));

        if (stats.lastPathTime() > 0) {
            sendMessage(String.format("§7Last Path Time: §f%dms", stats.lastPathTime()));
        }

        sendMessage(String.format("§7Status: %s",
                stats.isActive() ? "§aActive" : "§cInactive"));
        sendMessage(String.format("§7Current Mode: §d%s",
                stats.mode().getDescription()));

        // API Status
        sendMessage("§b--- Current Settings ---");
        sendMessage(String.format("§7AOTV: %s",
                apiStats.aotvEnabled() ? "§aEnabled" : "§cDisabled"));
        sendMessage(String.format("§7Etherwarp: %s",
                apiStats.etherwarpEnabled() ? "§aEnabled" : "§cDisabled"));
        sendMessage(String.format("§7Debug: %s",
                apiStats.debugMode() ? "§aEnabled" : "§cDisabled"));
        sendMessage(String.format("§7Rendering: %s",
                apiStats.renderEnabled() ? "§aEnabled" : "§cDisabled"));

        if (apiStats.isActive()) {
            sendMessage(String.format("§7Progress: §f%d/%d nodes",
                    apiStats.currentIndex(), apiStats.pathLength()));
        }
    }

    /**
     * Handle configuration commands.
     */
    private void handleConfig(PathfindingModule pathfinding, String[] args) {
        if (args.length < 3) {
            sendMessage("§bUsage: .pathfind config <setting> <value>");
            sendMessage("§7Available settings:");
            sendMessage("§7  aotv <true/false> - Enable/disable AOTV teleportation");
            sendMessage("§7  etherwarp <true/false> - Enable/disable Etherwarp");
            sendMessage("§7  debug <true/false> - Enable/disable debug output");
            sendMessage("§7  render <true/false> - Enable/disable path rendering");
            sendMessage("§7  speed <0.1-3.0> - Set pathfinding speed multiplier");
            return;
        }

        String setting = args[1].toLowerCase();
        String valueStr = args[2].toLowerCase();

        try {
            switch (setting) {
                case "aotv" -> {
                    boolean value = parseBoolean(valueStr);
                    pathfinding.setAotvEnabled(value);
                    sendMessage(String.format("§aAOTV teleportation: %s",
                            value ? "Enabled" : "Disabled"));
                }
                case "etherwarp", "ether" -> {
                    boolean value = parseBoolean(valueStr);
                    pathfinding.setEtherwarpEnabled(value);
                    sendMessage(String.format("§aEtherwarp teleportation: %s",
                            value ? "Enabled" : "Disabled"));
                }
                case "debug" -> {
                    boolean value = parseBoolean(valueStr);
                    pathfinding.setDebugMode(value);
                    sendMessage(String.format("§aDebug mode: %s",
                            value ? "Enabled" : "Disabled"));
                }
                case "render", "rendering" -> {
                    boolean value = parseBoolean(valueStr);
                    PathfinderAPI.setRenderPathEnabled(value);
                    sendMessage(String.format("§aPath rendering: %s",
                            value ? "Enabled" : "Disabled"));
                }
                case "speed" -> {
                    double speed = Double.parseDouble(valueStr);
                    if (speed < 0.1 || speed > 3.0) {
                        sendMessage("§cSpeed must be between 0.1 and 3.0");
                        return;
                    }
                    PathfinderAPI.setPathfindingSpeed(speed);
                    sendMessage(String.format("§aPathfinding speed: §f%.1f", speed));
                }
                default -> {
                    sendMessage("§cUnknown setting: " + setting);
                }
            }
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid value: " + valueStr);
        } catch (IllegalArgumentException e) {
            sendMessage("§cInvalid boolean value. Use: true/false, on/off, yes/no, 1/0");
        }
    }

    /**
     * Handle pathfinding mode commands.
     */
    private void handleMode(PathfindingModule pathfinding, String[] args) {
        if (args.length < 2) {
            sendMessage("§bCurrent mode: §d" + pathfinding.getPathfindingMode().getDescription());
            sendMessage("§7Available modes:");
            for (PathfindingModule.PathfindingMode mode : PathfindingModule.PathfindingMode.values()) {
                sendMessage("§7  " + mode.name().toLowerCase() + " - " + mode.getDescription());
            }
            sendMessage("§7Usage: .pathfind mode <mode|cycle>");
            return;
        }

        String modeStr = args[1].toLowerCase();

        if (modeStr.equals("cycle")) {
            pathfinding.cycleMode();
            sendMessage("§aMode changed to: §d" + pathfinding.getPathfindingMode().getDescription());
            return;
        }

        try {
            PathfindingModule.PathfindingMode mode = PathfindingModule.PathfindingMode.valueOf(modeStr.toUpperCase());
            pathfinding.setPathfindingMode(mode);
            sendMessage("§aMode changed to: §d" + mode.getDescription());
        } catch (IllegalArgumentException e) {
            sendMessage("§cInvalid mode: " + modeStr);
            sendMessage("§7Valid modes: walk_only, optimized, aggressive");
        }
    }

    /**
     * Handle test pathfinding (nearby test target).
     */
    private void handleTest(PathfindingModule pathfinding) {
        if (mc.player == null) {
            sendMessage("§cPlayer is null!");
            return;
        }

        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        // Test target 10 blocks in front of player
        double yaw = Math.toRadians(mc.player.getYaw());
        int offsetX = (int) Math.round(-Math.sin(yaw) * 10);
        int offsetZ = (int) Math.round(Math.cos(yaw) * 10);

        BlockPos testTarget = playerPos.add(offsetX, 0, offsetZ);

        sendMessage(String.format("§eTest pathfinding to %s", testTarget.toShortString()));
        pathfinding.pathfindTo(testTarget);
    }

    /**
     * Display help information.
     */
    private void handleHelp() {
        sendMessage("§b=== Pathfinder Commands ===");
        sendMessage("§7.pathfind <x> <y> <z> - Pathfind to coordinates");
        sendMessage("§7.pathfind here - Pathfind to cursor target");
        sendMessage("§7.pathfind stop - Stop current pathfinding");
        sendMessage("§7.pathfind stats [reset] - Show/reset statistics");
        sendMessage("§7.pathfind config <setting> <value> - Configure settings");
        sendMessage("§7.pathfind mode [mode|cycle] - Change pathfinding mode");
        sendMessage("§7.pathfind test - Test pathfinding (10 blocks ahead)");
        sendMessage("§7.pathfind help - Show this help");
        sendMessage("§b");
        sendMessage("§7Features:");
        sendMessage("§7  • Humanized movement with all keys (W,A,S,D)");
        sendMessage("§7  • AOTV random forward teleportation");
        sendMessage("§7  • Etherwarp stop-shift-aim-teleport sequence");
        sendMessage("§7  • Advanced jump prediction & path smoothing");
        sendMessage("§7  • Real-time path rendering and debugging");
    }

    /**
     * Parse boolean values with multiple formats.
     */
    private boolean parseBoolean(String value) {
        return switch (value.toLowerCase()) {
            case "true", "on", "yes", "1", "enable", "enabled" -> true;
            case "false", "off", "no", "0", "disable", "disabled" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean value: " + value);
        };
    }

    /**
     * Send a message to the player.
     */
}