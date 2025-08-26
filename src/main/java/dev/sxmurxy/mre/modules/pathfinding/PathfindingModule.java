package dev.sxmurxy.mre.modules.pathfinding;

import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import dev.sxmurxy.mre.modules.pathfinding.engine.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;

public class PathfindingModule extends Module {
    private static PathfindingModule instance;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Core systems
    private AdvancedPathfindingEngine pathfindingEngine;
    private AdvancedMovementExecutor movementExecutor;
    private MovementPredictor movementPredictor;

    // Path state
    private final List<PathNode> currentPath = new CopyOnWriteArrayList<>();
    private int pathIndex = 0;
    private boolean isPathing = false;
    private BlockPos targetPosition;
    private CompletableFuture<List<PathNode>> currentPathfindingTask;

    // Performance tracking
    private long pathStartTime = 0;
    private double totalDistanceTraveled = 0;
    private Vec3d lastPlayerPos = null;
    private int nodesTraversed = 0;

    // Movement timing
    private int ticksSinceLastNode = 0;
    private int stuckCounter = 0;
    private long lastProgressTime = System.currentTimeMillis();

    // Constants
    private static final double NODE_REACH_DISTANCE = 1.5;
    private static final double FINAL_NODE_REACH_DISTANCE = 0.4;
    private static final int MAX_STUCK_TICKS = 100;
    private static final int RECALCULATION_INTERVAL = 200; // 10 seconds

    // Safety flags
    private boolean isInitialized = false;
    private String lastError = null;

    public PathfindingModule() {
        super("AdvancedPathfinding", "Advanced pathfinding with humanization and 3D navigation", ModuleCategory.MOVEMENT);
        instance = this;

        // Safe initialization
        try {
            // Initialize basic state
            resetPathfindingState();
            System.out.println("[PathfindingModule] Module created successfully");
        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error in constructor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static PathfindingModule getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        try {
            System.out.println("[PathfindingModule] Attempting to enable...");

            // Check basic requirements first
            if (mc == null) {
                lastError = "MinecraftClient is null";
                System.err.println("[PathfindingModule] " + lastError);
                this.setToggled(false);
                return;
            }

            if (mc.world == null) {
                lastError = "World is not loaded";
                System.err.println("[PathfindingModule] " + lastError);
                sendMessage("§cCannot enable pathfinding: No world loaded");
                this.setToggled(false);
                return;
            }

            if (mc.player == null) {
                lastError = "Player is null";
                System.err.println("[PathfindingModule] " + lastError);
                sendMessage("§cCannot enable pathfinding: Player not found");
                this.setToggled(false);
                return;
            }

            System.out.println("[PathfindingModule] Basic checks passed, initializing systems...");

            // Initialize systems with error handling
            try {
                pathfindingEngine = new AdvancedPathfindingEngine(mc.world);
                System.out.println("[PathfindingModule] Pathfinding engine initialized");
            } catch (Exception e) {
                lastError = "Failed to initialize pathfinding engine: " + e.getMessage();
                System.err.println("[PathfindingModule] " + lastError);
                e.printStackTrace();
                this.setToggled(false);
                return;
            }

            try {
                movementExecutor = new AdvancedMovementExecutor();
                System.out.println("[PathfindingModule] Movement executor initialized");
            } catch (Exception e) {
                lastError = "Failed to initialize movement executor: " + e.getMessage();
                System.err.println("[PathfindingModule] " + lastError);
                e.printStackTrace();
                this.setToggled(false);
                return;
            }

            try {
                movementPredictor = new MovementPredictor();
                System.out.println("[PathfindingModule] Movement predictor initialized");
            } catch (Exception e) {
                lastError = "Failed to initialize movement predictor: " + e.getMessage();
                System.err.println("[PathfindingModule] " + lastError);
                e.printStackTrace();
                this.setToggled(false);
                return;
            }

            // Reset state
            resetPathfindingState();
            isInitialized = true;
            lastError = null;

            System.out.println("[PathfindingModule] Successfully enabled!");
            sendMessage("§aAdvanced pathfinding enabled - Ready for navigation");

        } catch (Exception e) {
            lastError = "Unexpected error during enable: " + e.getMessage();
            System.err.println("[PathfindingModule] " + lastError);
            e.printStackTrace();

            // Clean up and disable
            isInitialized = false;
            this.setToggled(false);

            if (mc.player != null) {
                sendMessage("§cFailed to enable pathfinding: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        try {
            System.out.println("[PathfindingModule] Disabling...");
            stop();
            isInitialized = false;
            sendMessage("§cAdvanced pathfinding disabled");
        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error during disable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdate() {
        if (!isInitialized || !this.isToggled()) {
            return;
        }

        try {
            if (mc.player == null || mc.world == null) return;

            // Update movement predictor safely
            if (movementPredictor != null) {
                movementPredictor.update(mc.player);
            }

            // Handle active pathfinding
            if (isPathing && !currentPath.isEmpty()) {
                updatePathfinding();
            }

            // Check for completed async pathfinding
            checkAsyncPathfinding();

        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error in onUpdate: " + e.getMessage());
            e.printStackTrace();

            // Disable module on critical error
            if (e instanceof NullPointerException || e instanceof IllegalStateException) {
                System.err.println("[PathfindingModule] Critical error, disabling module");
                this.setToggled(false);
            }
        }
    }

    private void updatePathfinding() {
        try {
            ClientPlayerEntity player = mc.player;
            Vec3d playerPos = player.getPos();

            // Update performance metrics
            updatePerformanceMetrics(playerPos);

            // Check if we've reached the current target node
            if (pathIndex < currentPath.size()) {
                PathNode targetNode = currentPath.get(pathIndex);
                double distanceToNode = playerPos.distanceTo(targetNode.exactPosition);

                boolean isFinalNode = pathIndex >= currentPath.size() - 1;
                double reachDistance = isFinalNode ? FINAL_NODE_REACH_DISTANCE : NODE_REACH_DISTANCE;

                if (distanceToNode <= reachDistance) {
                    advanceToNextNode();
                    return;
                }
            }

            // Check if we need to recalculate path
            if (shouldRecalculatePath()) {
                recalculatePath();
                return;
            }

            // Execute movement
            executeCurrentMovement();

            ticksSinceLastNode++;

        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error in updatePathfinding: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePerformanceMetrics(Vec3d currentPos) {
        if (lastPlayerPos != null) {
            double frameDistance = currentPos.distanceTo(lastPlayerPos);
            totalDistanceTraveled += frameDistance;

            // Update stuck detection
            if (frameDistance < 0.05) { // Very little movement
                stuckCounter++;
            } else {
                stuckCounter = Math.max(0, stuckCounter - 2);
                lastProgressTime = System.currentTimeMillis();
            }
        }

        lastPlayerPos = currentPos;
    }

    private void advanceToNextNode() {
        pathIndex++;
        ticksSinceLastNode = 0;
        nodesTraversed++;
        stuckCounter = 0;

        // Check if we've completed the path
        if (pathIndex >= currentPath.size()) {
            completePathfinding();
        } else {
            // Optional: Send progress updates for long paths
            if (currentPath.size() > 50 && nodesTraversed % 10 == 0) {
                sendMessage(String.format("§7Progress: %d/%d nodes (%.1f%%)",
                        nodesTraversed, currentPath.size(),
                        (double) nodesTraversed / currentPath.size() * 100));
            }
        }
    }

    private void completePathfinding() {
        long totalTime = System.currentTimeMillis() - pathStartTime;
        double avgSpeed = totalTime > 0 ? totalDistanceTraveled / (totalTime / 1000.0) : 0;

        sendMessage(String.format("§aDestination reached! §7(%.1fm in %.1fs, %.1fm/s)",
                totalDistanceTraveled, totalTime / 1000.0, avgSpeed));

        stop();
    }

    private boolean shouldRecalculatePath() {
        // Recalculate if stuck
        if (stuckCounter > MAX_STUCK_TICKS) {
            return true;
        }

        // Recalculate if no progress for too long
        if (System.currentTimeMillis() - lastProgressTime > 15000) {
            return true;
        }

        // Periodic recalculation for long paths
        if (ticksSinceLastNode > RECALCULATION_INTERVAL && currentPath.size() > 20) {
            return true;
        }

        // Check if we're too far off the path
        if (pathIndex < currentPath.size()) {
            PathNode currentTarget = currentPath.get(pathIndex);
            double distanceToPath = mc.player.getPos().distanceTo(currentTarget.exactPosition);

            if (distanceToPath > 8.0) { // Significantly off path
                return true;
            }
        }

        return false;
    }

    private void recalculatePath() {
        try {
            sendMessage("§eRecalculating path...");
            BlockPos playerPos = mc.player.getBlockPos();

            // Start async pathfinding from current position
            currentPathfindingTask = CompletableFuture.supplyAsync(() ->
                    pathfindingEngine.findPath(playerPos, targetPosition));

            // Reset stuck counter
            stuckCounter = 0;
            ticksSinceLastNode = 0;
        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error in recalculatePath: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeCurrentMovement() {
        try {
            if (pathIndex >= currentPath.size() || movementExecutor == null) return;

            PathNode currentNode = currentPath.get(pathIndex);
            PathNode nextNode = pathIndex + 1 < currentPath.size() ? currentPath.get(pathIndex + 1) : null;

            movementExecutor.executeMovement(currentNode, nextNode);
        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error in executeCurrentMovement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkAsyncPathfinding() {
        if (currentPathfindingTask != null && currentPathfindingTask.isDone()) {
            try {
                List<PathNode> newPath = currentPathfindingTask.get();

                if (newPath != null && !newPath.isEmpty()) {
                    currentPath.clear();
                    currentPath.addAll(newPath);
                    pathIndex = 0;
                    ticksSinceLastNode = 0;
                    sendMessage(String.format("§aPath recalculated: %d nodes", newPath.size()));
                } else {
                    sendMessage("§cRecalculation failed - no path found");
                    stop();
                }
            } catch (Exception e) {
                System.err.println("[PathfindingModule] Error in checkAsyncPathfinding: " + e.getMessage());
                sendMessage("§cPathfinding error: " + e.getMessage());
                stop();
            } finally {
                currentPathfindingTask = null;
            }
        }
    }

    // Public API methods
    public static void walkTo(BlockPos targetPos) {
        PathfindingModule module = getInstance();
        if (module == null || !module.isInitialized) {
            System.err.println("[PathfindingModule] Module not initialized for walkTo");
            return;
        }

        module.startPathfinding(targetPos);
    }

    public static void walkTo(int x, int y, int z) {
        walkTo(new BlockPos(x, y, z));
    }

    public static void stop() {
        PathfindingModule module = getInstance();
        if (module == null) return;

        module.stopPathfinding();
    }

    public static boolean isActive() {
        PathfindingModule module = getInstance();
        return module != null && module.isPathing && module.isInitialized;
    }

    public static List<PathNode> getCurrentPath() {
        PathfindingModule module = getInstance();
        return module != null ? module.currentPath : null;
    }

    // Implementation methods
    private void startPathfinding(BlockPos targetPos) {
        try {
            if (!isInitialized) {
                sendMessage("§cPathfinding module not properly initialized");
                return;
            }

            if (mc.player == null || mc.world == null) {
                sendMessage("§cCannot start pathfinding: Player or world not available");
                return;
            }

            // Stop current pathfinding
            stopPathfinding();

            // Validate target
            if (targetPos.equals(mc.player.getBlockPos())) {
                sendMessage("§cAlready at target position");
                return;
            }

            // Set up pathfinding
            targetPosition = targetPos;
            BlockPos playerPos = mc.player.getBlockPos();

            sendMessage(String.format("§eStarting pathfinding to %d %d %d...",
                    targetPos.getX(), targetPos.getY(), targetPos.getZ()));

            // Start async pathfinding
            pathStartTime = System.currentTimeMillis();
            currentPathfindingTask = CompletableFuture.supplyAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    List<PathNode> path = pathfindingEngine.findPath(playerPos, targetPos);
                    long duration = System.currentTimeMillis() - startTime;

                    if (path != null && !path.isEmpty()) {
                        mc.execute(() -> sendMessage(String.format("§aPath found! %d nodes in %.1fs",
                                path.size(), duration / 1000.0)));
                    }

                    return path;
                } catch (Exception e) {
                    System.err.println("[PathfindingModule] Error in async pathfinding: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            });

            resetPathfindingState();
            isPathing = true;

        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error in startPathfinding: " + e.getMessage());
            e.printStackTrace();
            sendMessage("§cError starting pathfinding: " + e.getMessage());
        }
    }

    private void stopPathfinding() {
        try {
            isPathing = false;
            currentPath.clear();
            pathIndex = 0;
            targetPosition = null;

            // Cancel async task
            if (currentPathfindingTask != null) {
                currentPathfindingTask.cancel(true);
                currentPathfindingTask = null;
            }

            // Stop all movement
            if (mc.options != null) {
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
            }

            resetPathfindingState();
        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error in stopPathfinding: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetPathfindingState() {
        ticksSinceLastNode = 0;
        stuckCounter = 0;
        nodesTraversed = 0;
        totalDistanceTraveled = 0;
        lastPlayerPos = null;
        lastProgressTime = System.currentTimeMillis();
        pathStartTime = System.currentTimeMillis();
    }

    private void sendMessage(String message) {
        try {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(message), false);
            } else {
                System.out.println("[PathfindingModule] " + message);
            }
        } catch (Exception e) {
            System.out.println("[PathfindingModule] " + message);
        }
    }

    // Utility methods for external access
    public BlockPos getTargetPosition() {
        return targetPosition;
    }

    public int getCurrentPathIndex() {
        return pathIndex;
    }

    public double getProgressPercentage() {
        if (currentPath.isEmpty()) return 0.0;
        return (double) pathIndex / currentPath.size() * 100.0;
    }

    public boolean isStuck() {
        return stuckCounter > MAX_STUCK_TICKS / 2;
    }

    public String getStatusMessage() {
        if (!isInitialized) return "§cNot initialized";
        if (lastError != null) return "§cError: " + lastError;
        if (!isPathing) return "§7Idle";
        if (currentPath.isEmpty()) return "§eCalculating path...";

        return String.format("§aActive §7(%d/%d nodes, %.1f%% complete)",
                pathIndex, currentPath.size(), getProgressPercentage());
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public String getLastError() {
        return lastError;
    }
}