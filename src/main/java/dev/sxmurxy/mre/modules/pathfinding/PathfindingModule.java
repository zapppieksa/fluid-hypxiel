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

    // Initialization state
    private InitializationState initState = InitializationState.DISABLED;
    private String lastError = null;
    private int initializationAttempts = 0;
    private long lastInitAttempt = 0;
    private static final int MAX_INIT_ATTEMPTS = 5;
    private static final long INIT_RETRY_DELAY = 2000; // 2 seconds

    // Pending pathfinding request (for when module isn't ready yet)
    private BlockPos pendingTarget = null;

    private enum InitializationState {
        DISABLED,           // Module is off
        PENDING,           // Module is on but not yet initialized
        INITIALIZING,      // Currently attempting initialization
        READY,             // Fully initialized and ready
        FAILED             // Initialization failed permanently
    }

    public PathfindingModule() {
        super("AdvancedPathfinding", "Advanced pathfinding with humanization and 3D navigation", ModuleCategory.MOVEMENT);
        instance = this;
        resetPathfindingState();
        System.out.println("[PathfindingModule] Module created successfully");
    }

    public static PathfindingModule getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        System.out.println("[PathfindingModule] Module enabled - will initialize on next update");
        initState = InitializationState.PENDING;
        lastError = null;
        initializationAttempts = 0;
    }

    @Override
    public void onDisable() {
        System.out.println("[PathfindingModule] Disabling module...");

        // Stop any active pathfinding
        stopPathfinding();

        // Clean up systems
        cleanupSystems();

        // Reset state
        initState = InitializationState.DISABLED;
        lastError = null;
        initializationAttempts = 0;
        pendingTarget = null;

        sendMessage("§cAdvanced pathfinding disabled");
    }

    @Override
    public void onUpdate() {
        if (!this.isToggled()) {
            return;
        }

        try {
            // Handle initialization states
            switch (initState) {
                case PENDING:
                    attemptInitialization();
                    break;
                case INITIALIZING:
                    // Wait for initialization to complete
                    break;
                case READY:
                    // Normal operation
                    handleNormalOperation();
                    break;
                case FAILED:
                    // Try to reinitialize after delay
                    handleFailedState();
                    break;
                case DISABLED:
                    // Should not happen when toggled on
                    initState = InitializationState.PENDING;
                    break;
            }

        } catch (Exception e) {
            System.err.println("[PathfindingModule] Critical error in onUpdate: " + e.getMessage());
            e.printStackTrace();
            handleCriticalError(e);
        }
    }

    private void attemptInitialization() {
        long currentTime = System.currentTimeMillis();

        // Check retry delay
        if (currentTime - lastInitAttempt < INIT_RETRY_DELAY) {
            return;
        }

        // Check max attempts
        if (initializationAttempts >= MAX_INIT_ATTEMPTS) {
            initState = InitializationState.FAILED;
            lastError = "Maximum initialization attempts exceeded";
            sendMessage("§cPathfinding initialization failed after " + MAX_INIT_ATTEMPTS + " attempts");
            return;
        }

        lastInitAttempt = currentTime;
        initializationAttempts++;
        initState = InitializationState.INITIALIZING;

        System.out.println("[PathfindingModule] Initialization attempt #" + initializationAttempts);

        // Perform initialization in a separate thread to avoid blocking
        CompletableFuture.runAsync(() -> {
            try {
                performInitialization();
            } catch (Exception e) {
                handleInitializationError(e);
            }
        });
    }

    private void performInitialization() {
        System.out.println("[PathfindingModule] === INITIALIZATION START ===");

        try {
            // Validate basic requirements
            if (!validateRequirements()) {
                return; // Error already set in validateRequirements
            }

            System.out.println("[PathfindingModule] Requirements validated, initializing systems...");

            // Initialize pathfinding engine
            System.out.println("[PathfindingModule] Initializing pathfinding engine...");
            AdvancedPathfindingEngine tempEngine = new AdvancedPathfindingEngine(mc.world);

            // Initialize movement executor
            System.out.println("[PathfindingModule] Initializing movement executor...");
            AdvancedMovementExecutor tempExecutor = new AdvancedMovementExecutor();

            // Initialize movement predictor
            System.out.println("[PathfindingModule] Initializing movement predictor...");
            MovementPredictor tempPredictor = new MovementPredictor();

            // If we get here, all initialization succeeded
            // Update on main thread to avoid threading issues
            mc.execute(() -> {
                pathfindingEngine = tempEngine;
                movementExecutor = tempExecutor;
                movementPredictor = tempPredictor;

                resetPathfindingState();
                initState = InitializationState.READY;
                lastError = null;

                System.out.println("[PathfindingModule] === INITIALIZATION SUCCESS ===");
                sendMessage("§aAdvanced pathfinding ready - All systems initialized");

                // Handle pending pathfinding request
                if (pendingTarget != null) {
                    BlockPos target = pendingTarget;
                    pendingTarget = null;
                    startPathfinding(target);
                }
            });

        } catch (Exception e) {
            handleInitializationError(e);
        }
    }

    private boolean validateRequirements() {
        if (mc == null) {
            lastError = "MinecraftClient is null";
            System.err.println("[PathfindingModule] " + lastError);
            mc.execute(() -> initState = InitializationState.FAILED);
            return false;
        }

        if (mc.world == null) {
            lastError = "World is not loaded";
            System.err.println("[PathfindingModule] " + lastError);
            mc.execute(() -> {
                initState = InitializationState.PENDING; // Try again when world loads
                sendMessage("§eWaiting for world to load...");
            });
            return false;
        }

        if (mc.player == null) {
            lastError = "Player is not available";
            System.err.println("[PathfindingModule] " + lastError);
            mc.execute(() -> {
                initState = InitializationState.PENDING; // Try again when player loads
                sendMessage("§eWaiting for player to load...");
            });
            return false;
        }

        System.out.println("[PathfindingModule] ✓ All requirements validated");
        return true;
    }

    private void handleInitializationError(Exception e) {
        lastError = "Initialization failed: " + e.getMessage();
        System.err.println("[PathfindingModule] " + lastError);
        e.printStackTrace();

        mc.execute(() -> {
            if (initializationAttempts >= MAX_INIT_ATTEMPTS) {
                initState = InitializationState.FAILED;
                sendMessage("§cPathfinding initialization failed permanently: " + e.getMessage());
            } else {
                initState = InitializationState.PENDING; // Try again
                sendMessage("§ePathfinding initialization failed, will retry... (" +
                        initializationAttempts + "/" + MAX_INIT_ATTEMPTS + ")");
            }
        });
    }

    private void handleNormalOperation() {
        if (mc.player == null || mc.world == null) {
            // Lost connection to game world, need to reinitialize
            System.out.println("[PathfindingModule] Lost game connection, reinitializing...");
            initState = InitializationState.PENDING;
            cleanupSystems();
            return;
        }

        // Update movement predictor
        if (movementPredictor != null) {
            movementPredictor.update(mc.player);
        }

        // Handle active pathfinding
        if (isPathing && !currentPath.isEmpty()) {
            updatePathfinding();
        }

        // Check for completed async pathfinding
        checkAsyncPathfinding();
    }

    private void handleFailedState() {
        long currentTime = System.currentTimeMillis();

        // Try to reinitialize after a longer delay
        if (currentTime - lastInitAttempt > INIT_RETRY_DELAY * 3) {
            System.out.println("[PathfindingModule] Attempting recovery from failed state...");
            initState = InitializationState.PENDING;
            initializationAttempts = 0; // Reset attempts for recovery
            lastError = null;
        }
    }

    private void handleCriticalError(Exception e) {
        System.err.println("[PathfindingModule] Critical error occurred, attempting recovery...");

        // Stop pathfinding and cleanup
        stopPathfinding();
        cleanupSystems();

        // Reset to pending state for reinitialization
        initState = InitializationState.PENDING;
        lastError = "Critical error: " + e.getMessage();

        sendMessage("§cPathfinding encountered an error and is reinitializing...");
    }

    private void cleanupSystems() {
        try {
            // Stop all movement
            if (mc.options != null) {
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
            }

            // Cancel async tasks
            if (currentPathfindingTask != null) {
                currentPathfindingTask.cancel(true);
                currentPathfindingTask = null;
            }

            // Clear systems
            pathfindingEngine = null;
            movementExecutor = null;
            movementPredictor = null;

            System.out.println("[PathfindingModule] Systems cleaned up");
        } catch (Exception e) {
            System.err.println("[PathfindingModule] Error during cleanup: " + e.getMessage());
        }
    }

    // Rest of the pathfinding logic (unchanged)
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

        stopPathfinding();
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
                    stopPathfinding();
                }
            } catch (Exception e) {
                System.err.println("[PathfindingModule] Error in checkAsyncPathfinding: " + e.getMessage());
                sendMessage("§cPathfinding error: " + e.getMessage());
                stopPathfinding();
            } finally {
                currentPathfindingTask = null;
            }
        }
    }

    // Public API methods
    public static void walkTo(BlockPos targetPos) {
        PathfindingModule module = getInstance();
        if (module == null) {
            System.err.println("[PathfindingModule] Module instance not available for walkTo");
            return;
        }

        module.requestPathfinding(targetPos);
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
        return module != null && module.isPathing && module.initState == InitializationState.READY;
    }

    public static List<PathNode> getCurrentPath() {
        PathfindingModule module = getInstance();
        return module != null ? module.currentPath : null;
    }

    // Implementation methods
    private void requestPathfinding(BlockPos targetPos) {
        if (initState != InitializationState.READY) {
            // Store the request for when initialization completes
            pendingTarget = targetPos;

            if (initState == InitializationState.DISABLED) {
                sendMessage("§ePathfinding module is disabled. Enable it first.");
                return;
            }

            sendMessage("§ePathfinding is initializing... Your request will be processed shortly.");
            System.out.println("[PathfindingModule] Pathfinding request queued until initialization completes");
            return;
        }

        startPathfinding(targetPos);
    }

    private void startPathfinding(BlockPos targetPos) {
        try {
            if (initState != InitializationState.READY) {
                sendMessage("§cPathfinding system not ready");
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
            pendingTarget = null; // Clear any pending requests

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
        switch (initState) {
            case DISABLED:
                return "§cDisabled";
            case PENDING:
                return "§eInitializing... (" + initializationAttempts + "/" + MAX_INIT_ATTEMPTS + ")";
            case INITIALIZING:
                return "§eInitializing systems...";
            case FAILED:
                return "§cInitialization failed: " + (lastError != null ? lastError : "Unknown error");
            case READY:
                if (!isPathing) return "§aReady";
                if (currentPath.isEmpty()) return "§eCalculating path...";
                return String.format("§aActive §7(%d/%d nodes, %.1f%% complete)",
                        pathIndex, currentPath.size(), getProgressPercentage());
            default:
                return "§7Unknown state";
        }
    }

    public boolean isInitialized() {
        return initState == InitializationState.READY;
    }

    public String getLastError() {
        return lastError;
    }

    public InitializationState getInitState() {
        return initState;
    }

    public boolean hasPendingRequest() {
        return pendingTarget != null;
    }
}