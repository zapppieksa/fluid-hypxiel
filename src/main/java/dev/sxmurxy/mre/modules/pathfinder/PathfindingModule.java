package dev.sxmurxy.mre.modules.pathfinder;

import dev.sxmurxy.mre.client.pathfinding.*;
import dev.sxmurxy.mre.modules.Module;
import dev.sxmurxy.mre.modules.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.concurrent.atomic.AtomicLong;

public class PathfindingModule extends Module {

    private static final PathfindingModule INSTANCE = new PathfindingModule();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final AtomicLong pathsCompleted = new AtomicLong(0);
    private final AtomicLong pathsFailed = new AtomicLong(0);
    private long lastPathfindTime = 0;

    public PathfindingModule() {
        super("Pathfinding", "Automatyczny sprint.", ModuleCategory.MISCELLANEOUS);
    }

    public static PathfindingModule getInstance() {
        return INSTANCE;
    }

    public void pathfindTo(BlockPos target) {
        long startTime = System.currentTimeMillis();
        PathfinderAPI.findAndFollowPath(target, success -> {
            if (success) {
                pathsCompleted.incrementAndGet();
            } else {
                pathsFailed.incrementAndGet();
            }
            lastPathfindTime = System.currentTimeMillis() - startTime;
        });
    }

    public void stopPathfinding() {
        PathfinderAPI.stop();
    }

    public boolean isPathfinding() {
        return PathfinderAPI.isActive();
    }

    public void setAotvEnabled(boolean enabled) { PathfinderAPI.setAotvEnabled(enabled); }
    public void setEtherwarpEnabled(boolean enabled) { PathfinderAPI.setEtherwarpEnabled(enabled); }
    public void setDebugMode(boolean enabled) { PathfinderAPI.setDebugMode(enabled); }
    public void setPathfindingMode(Pathfinder.PathfindingMode mode) { PathfinderAPI.setPathfindingMode(mode); }

    public long getPathsCompleted() { return pathsCompleted.get(); }
    public long getPathsFailed() { return pathsFailed.get(); }
    public long getLastPathfindTime() { return lastPathfindTime; }
}

