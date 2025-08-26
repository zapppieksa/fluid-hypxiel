package dev.sxmurxy.mre.modules.command;

import dev.sxmurxy.mre.modules.pathfinding.PathfindingModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class PathfindCommand extends Command {
    public PathfindCommand() {
        super("pathfind", "Advanced pathfinding with humanization and 3D navigation",
                ".pathfind <x> <y> <z> | .pathfind stop | .pathfind status | .pathfind here | .pathfind test | .pathfind debug");
    }

    @Override
    public void execute(String[] args) {
        PathfindingModule pathModule = PathfindingModule.getInstance();

        if (pathModule == null) {
            sendMessage("§cPathfinding module not found. Enable it first!");
            return;
        }

        if (args.length == 0) {
            handleDebug(); // Show debug info by default
            return;
        }

        switch (args[0].toLowerCase()) {
            case "stop":
                handleStop();
                break;
            case "status":
                handleStatus();
                break;
            case "here":
                handleHere();
                break;
            case "test":
                handleTest();
                break;
            case "debug":
                handleDebug();
                break;
            default:
                handleCoordinates(args);
                break;
        }
    }

    private void handleDebug() {
        PathfindingModule pathModule = PathfindingModule.getInstance();

        if (pathModule == null) {
            sendMessage("§c❌ PathfindingModule instance is NULL!");
            return;
        }

        sendMessage("§f=== PATHFINDING DEBUG ===");
        sendMessage("§7Module found: §a✓");
        sendMessage("§7Is toggled: §b" + pathModule.isToggled());
        sendMessage("§7Is initialized: §b" + pathModule.isInitialized());
        sendMessage("§7Status: " + pathModule.getStatusMessage());

        if (pathModule.getLastError() != null) {
            sendMessage("§c❌ Last Error: " + pathModule.getLastError());
        }

        // Try to enable if not enabled
        if (!pathModule.isToggled()) {
            sendMessage("§e⚠️ Module is disabled, trying to enable...");
            pathModule.setToggled(true);

            // Wait a moment and check again
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            sendMessage("§7After enable attempt:");
            sendMessage("§7Is toggled: §b" + pathModule.isToggled());
            sendMessage("§7Is initialized: §b" + pathModule.isInitialized());
        }

        // If still not initialized, show detailed info
        if (!pathModule.isInitialized()) {
            sendMessage("§c❌ Module not initialized!");
            sendMessage("§7Try manually enabling it in your GUI first");

            MinecraftClient mc = MinecraftClient.getInstance();
            sendMessage("§7World: " + (mc.world != null ? "✓" : "❌"));
            sendMessage("§7Player: " + (mc.player != null ? "✓" : "❌"));
        } else {
            sendMessage("§a✅ Module is ready!");
        }
    }

    private void handleStop() {
        PathfindingModule.stop();
        sendMessage("§fFluid §7» §cPathfinding stopped");
    }

    private void handleStatus() {
        PathfindingModule pathModule = PathfindingModule.getInstance();

        if (!PathfindingModule.isActive()) {
            sendMessage("§fFluid §7» §7Pathfinding is §cidle");
            return;
        }

        BlockPos target = pathModule.getTargetPosition();
        String status = pathModule.getStatusMessage();
        boolean stuck = pathModule.isStuck();

        sendMessage("§fFluid §7» §fPathfinding Status:");
        sendMessage("§7  Target: §b" + (target != null ?
                target.getX() + " " + target.getY() + " " + target.getZ() : "None"));
        sendMessage("§7  Status: " + status);

        if (stuck) {
            sendMessage("§7  Warning: §ePlayer may be stuck");
        }
    }

    private void handleHere() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            sendMessage("§cPlayer not found");
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos testTarget = playerPos.add(
                (int) (Math.random() * 20) - 10,  // Random X offset
                (int) (Math.random() * 6) - 3,    // Random Y offset
                (int) (Math.random() * 20) - 10   // Random Z offset
        );

        startPathfinding(testTarget);
        sendMessage("§fFluid §7» §fTest pathfinding to nearby location");
    }

    private void handleTest() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            sendMessage("§cPlayer not found");
            return;
        }

        // Create a challenging test scenario
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos[] testTargets = {
                playerPos.add(50, 10, 50),   // High platform
                playerPos.add(-30, -5, 25),  // Lower area
                playerPos.add(0, 20, 100),   // Distant high point
                playerPos.add(-100, 0, -100) // Far diagonal
        };

        BlockPos selectedTarget = testTargets[(int) (Math.random() * testTargets.length)];
        startPathfinding(selectedTarget);

        sendMessage("§fFluid §7» §fStarting challenging pathfinding test");
        sendMessage("§7Target: " + selectedTarget.getX() + " " + selectedTarget.getY() + " " + selectedTarget.getZ());
    }

    private void handleCoordinates(String[] args) {
        if (args.length < 3) {
            sendUsage();
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);

            BlockPos targetPos = new BlockPos(x, y, z);
            startPathfinding(targetPos);

            sendMessage(String.format("§fFluid §7» §fPathfinding to §b%d %d %d", x, y, z));

        } catch (NumberFormatException e) {
            sendMessage("§cInvalid coordinates! Use integers only");
        }
    }

    private void startPathfinding(BlockPos targetPos) {
        PathfindingModule pathModule = PathfindingModule.getInstance();

        // Check if module is ready
        if (!pathModule.isInitialized()) {
            sendMessage("§c❌ Pathfinding module not initialized!");
            sendMessage("§7Try running: §b.pathfind debug §7to troubleshoot");
            return;
        }

        // Auto-enable module if disabled
        if (!pathModule.isToggled()) {
            sendMessage("§eEnabling pathfinding module...");
            pathModule.setToggled(true);

            // Wait for initialization
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}

            if (!pathModule.isInitialized()) {
                sendMessage("§cFailed to initialize module. Check console for errors.");
                return;
            }
        }

        // Start pathfinding
        PathfindingModule.walkTo(targetPos);
    }

    @Override
    public void sendUsage() {
        sendMessage("§fFluid §7» §fAdvanced Pathfinding Usage:");
        sendMessage("§7  §b.pathfind <x> <y> <z> §7- Navigate to coordinates");
        sendMessage("§7  §b.pathfind stop §7- Stop current pathfinding");
        sendMessage("§7  §b.pathfind status §7- Check pathfinding status");
        sendMessage("§7  §b.pathfind here §7- Test pathfinding nearby");
        sendMessage("§7  §b.pathfind test §7- Run challenging pathfinding test");
        sendMessage("§7  §b.pathfind debug §7- Show debug information");
        sendMessage("§7");
        sendMessage("§7Features: 3D navigation, parkour jumps, climbing,");
        sendMessage("§7humanized movement, hazard avoidance, caching");
        sendMessage("§7");
        sendMessage("§c⚠️ If not working, try: §b.pathfind debug");
    }
}