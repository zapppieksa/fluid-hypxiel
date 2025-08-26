package dev.sxmurxy.mre.modules.pathfinding.engine;

import dev.sxmurxy.mre.modules.pathfinding.config.PathfinderConfig;
import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class PathfinderEngine {

    private final PathfinderConfig config;
    private final AtomicBoolean cancellationToken = new AtomicBoolean(false);

    public PathfinderEngine(PathfinderConfig config) {
        this.config = config;
    }

    public CompletableFuture<List<Vec3d>> calculatePath(BlockPos target) {
        cancellationToken.set(false);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;

        return CompletableFuture.supplyAsync(() -> {
            AStarPathfinder pathfinder = new AStarPathfinder(world, config, cancellationToken);
            List<PathNode> rawPath = pathfinder.findPath(player.getBlockPos(), target);

            if (cancellationToken.get() || rawPath == null || rawPath.isEmpty()) {
                return new ArrayList<>();
            }

            List<PathNode> optimizedPath = optimizePath(rawPath, world);
            return PathSmoother.smoothPath(optimizedPath, config.PATH_SMOOTHNESS);
        });
    }

    public void cancel() {
        cancellationToken.set(true);
    }

    private List<PathNode> optimizePath(List<PathNode> oldPath, World world) {
        if (oldPath.size() < 3) return oldPath;
        List<PathNode> newPath = new ArrayList<>();
        newPath.add(oldPath.get(0));
        int currentIndex = 0;
        while (currentIndex < oldPath.size() - 1) {
            int lookAheadIndex = oldPath.size() - 1;
            while (lookAheadIndex > currentIndex + 1) {
                if (hasLineOfSight(oldPath.get(currentIndex).position, oldPath.get(lookAheadIndex).position, world)) {
                    break;
                }
                lookAheadIndex--;
            }
            newPath.add(oldPath.get(lookAheadIndex));
            currentIndex = lookAheadIndex;
        }
        return newPath;
    }

    public static boolean hasLineOfSight(BlockPos from, BlockPos to, World world) {
        RaycastContext context = new RaycastContext(
                Vec3d.ofCenter(from),
                Vec3d.ofCenter(to),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                MinecraftClient.getInstance().player
        );
        return world.raycast(context).getType() == HitResult.Type.MISS;
    }
}
