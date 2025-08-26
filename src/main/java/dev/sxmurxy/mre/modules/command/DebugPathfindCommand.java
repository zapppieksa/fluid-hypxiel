package dev.sxmurxy.mre.modules.command;

import dev.sxmurxy.mre.modules.pathfinding.PathfindingModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class DebugPathfindCommand extends Command {
    public DebugPathfindCommand() {
        super("debugpath", "Debug pathfinding module",
                ".debugpath | .debugpath force | .debugpath test");
    }

    @Override
    public void execute(String[] args) {
        try {
            if (args.length > 0 && args[0].equalsIgnoreCase("force")) {
                handleForceEnable();
                return;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
                handleSystemTest();
                return;
            }

            handleDebugInfo();

        } catch (Exception e) {
            sendMessage("§cError in debug command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDebugInfo() {
        sendMessage("§f=== ADVANCED PATHFINDING DEBUG ===");

        // Check module instance
        PathfindingModule module = PathfindingModule.getInstance();
        if (module == null) {
            sendMessage("§c❌ PathfindingModule instance is NULL!");
            sendMessage("§7This means the module class wasn't loaded properly.");
            sendMessage("§7Check if the module is registered in your ModuleManager.");
            return;
        }

        sendMessage("§a✓ Module instance found");

        // Check basic state
        sendMessage("§f--- Module State ---");
        sendMessage("§7Toggled: " + (module.isToggled() ? "§a✓ ON" : "§c❌ OFF"));
        sendMessage("§7Initialized: " + (module.isInitialized() ? "§a✓ YES" : "§c❌ NO"));
        sendMessage("§7Status: " + module.getStatusMessage());

        if (module.getLastError() != null) {
            sendMessage("§c❌ Last Error: " + module.getLastError());
        }

        // Check environment
        sendMessage("§f--- Environment ---");
        MinecraftClient mc = MinecraftClient.getInstance();
        sendMessage("§7MinecraftClient: " + (mc != null ? "§a✓" : "§c❌"));
        sendMessage("§7World loaded: " + (mc != null && mc.world != null ? "§a✓" : "§c❌"));
        sendMessage("§7Player present: " + (mc != null && mc.player != null ? "§a✓" : "§c❌"));

        if (mc != null && mc.player != null) {
            BlockPos playerPos = mc.player.getBlockPos();
            sendMessage("§7Player position: §b" + playerPos.getX() + " " + playerPos.getY() + " " + playerPos.getZ());
        }

        // Check pathfinding state
        if (PathfindingModule.isActive()) {
            sendMessage("§f--- Active Pathfinding ---");
            sendMessage("§7Target: " + (module.getTargetPosition() != null ?
                    "§b" + module.getTargetPosition().getX() + " " +
                            module.getTargetPosition().getY() + " " +
                            module.getTargetPosition().getZ() : "§cNone"));
            sendMessage("§7Progress: §b" + String.format("%.1f%%", module.getProgressPercentage()));
            sendMessage("§7Stuck: " + (module.isStuck() ? "§c❌ YES" : "§a✓ NO"));
        }

        // Provide suggestions
        sendMessage("§f--- Suggestions ---");
        if (!module.isToggled()) {
            sendMessage("§e⚠️ Enable the module in your GUI or run: §b.debugpath force");
        } else if (!module.isInitialized()) {
            sendMessage("§e⚠️ Module failed to initialize. Check console for errors.");
            sendMessage("§7Common issues:");
            sendMessage("§7  - Missing engine files");
            sendMessage("§7  - World not loaded when enabling");
            sendMessage("§7  - Import/compilation errors");
        } else {
            sendMessage("§a✅ Module is ready! Try: §b.pathfind test");
        }
    }

    private void handleForceEnable() {
        sendMessage("§eForce enabling pathfinding module...");

        PathfindingModule module = PathfindingModule.getInstance();
        if (module == null) {
            sendMessage("§c❌ Module instance not found!");
            return;
        }

        // Force enable
        module.setToggled(true);

        // Give it time to initialize
        new Thread(() -> {
            try {
                Thread.sleep(500);
                MinecraftClient.getInstance().execute(() -> {
                    sendMessage("§f--- Force Enable Result ---");
                    sendMessage("§7Toggled: " + (module.isToggled() ? "§a✓" : "§c❌"));
                    sendMessage("§7Initialized: " + (module.isInitialized() ? "§a✓" : "§c❌"));

                    if (module.isInitialized()) {
                        sendMessage("§a✅ Success! Module is now ready.");
                        sendMessage("§7Try: §b.pathfind test");
                    } else {
                        sendMessage("§c❌ Failed to initialize.");
                        if (module.getLastError() != null) {
                            sendMessage("§cError: " + module.getLastError());
                        }
                        sendMessage("§7Check console for detailed error messages.");
                    }
                });
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void handleSystemTest() {
        sendMessage("§f=== PATHFINDING SYSTEM TEST ===");

        PathfindingModule module = PathfindingModule.getInstance();
        if (module == null) {
            sendMessage("§c❌ Module not found!");
            return;
        }

        // Test 1: Module State
        sendMessage("§7Test 1: Module State");
        boolean test1 = module.isToggled() && module.isInitialized();
        sendMessage(test1 ? "§a✓ PASS" : "§c❌ FAIL - Module not ready");

        // Test 2: Environment
        sendMessage("§7Test 2: Environment");
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean test2 = mc != null && mc.world != null && mc.player != null;
        sendMessage(test2 ? "§a✓ PASS" : "§c❌ FAIL - Environment not ready");

        // Test 3: Try Short Pathfind
        if (test1 && test2) {
            sendMessage("§7Test 3: Short Pathfind");
            BlockPos playerPos = mc.player.getBlockPos();
            BlockPos nearbyTarget = playerPos.add(3, 0, 3);

            try {
                PathfindingModule.walkTo(nearbyTarget);
                sendMessage("§a✓ PASS - Pathfind command executed");

                // Wait and check if path was found
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        MinecraftClient.getInstance().execute(() -> {
                            if (PathfindingModule.isActive()) {
                                sendMessage("§a✅ System test completed successfully!");
                                sendMessage("§7Pathfinding is working. You can now use:");
                                sendMessage("§b.pathfind <x> <y> <z>");
                            } else {
                                sendMessage("§e⚠️ Path calculation may have failed.");
                                sendMessage("§7This is normal for some locations.");
                            }
                        });
                    } catch (InterruptedException ignored) {}
                }).start();

            } catch (Exception e) {
                sendMessage("§c❌ FAIL - " + e.getMessage());
            }
        } else {
            sendMessage("§7Test 3: Skipped (prerequisites failed)");
        }

        // Summary
        sendMessage("§f--- Test Summary ---");
        if (test1 && test2) {
            sendMessage("§a✅ All basic tests passed!");
            sendMessage("§7Your pathfinding system is ready to use.");
        } else {
            sendMessage("§c❌ Some tests failed.");
            sendMessage("§7Run §b.debugpath §7for more information.");
        }
    }

    @Override
    public void sendUsage() {
        sendMessage("§fFluid §7» §fDebug Pathfinding Usage:");
        sendMessage("§7  §b.debugpath §7- Show detailed debug information");
        sendMessage("§7  §b.debugpath force §7- Force enable the module");
        sendMessage("§7  §b.debugpath test §7- Run system tests");
        sendMessage("§7");
        sendMessage("§7Use this command if pathfinding isn't working properly.");
    }
}