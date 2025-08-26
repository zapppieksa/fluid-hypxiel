package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class PathfindingCache {
    private final Map<String, CachedSegment> segments = new ConcurrentHashMap<>();
    private final long CACHE_EXPIRY_MS = 300000; // 5 minutes
    private final int MAX_CACHE_SIZE = 1000;

    private static class CachedSegment {
        final List<PathNode> path;
        final long timestamp;
        final BlockPos start, end;

        CachedSegment(BlockPos start, BlockPos end, List<PathNode> path) {
            this.start = start;
            this.end = end;
            this.path = new ArrayList<>(path);
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 300000;
        }
    }

    public void cachePath(BlockPos start, BlockPos goal, List<PathNode> path) {
        if (path == null || path.isEmpty()) return;

        String key = createKey(start, goal);
        segments.put(key, new CachedSegment(start, goal, path));

        // Cleanup if too large
        if (segments.size() > MAX_CACHE_SIZE) {
            cleanupExpired();
        }
    }

    public List<PathNode> getCachedPath(BlockPos start, BlockPos goal) {
        String key = createKey(start, goal);
        CachedSegment segment = segments.get(key);

        if (segment != null && !segment.isExpired()) {
            return new ArrayList<>(segment.path);
        }

        // Try reverse path
        String reverseKey = createKey(goal, start);
        segment = segments.get(reverseKey);
        if (segment != null && !segment.isExpired()) {
            List<PathNode> reversed = new ArrayList<>(segment.path);
            Collections.reverse(reversed);
            return reversed;
        }

        return null;
    }

    private String createKey(BlockPos start, BlockPos end) {
        return start.getX() + "," + start.getY() + "," + start.getZ() +
                "->" + end.getX() + "," + end.getY() + "," + end.getZ();
    }

    private void cleanupExpired() {
        segments.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}