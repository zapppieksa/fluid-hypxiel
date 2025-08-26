package dev.sxmurxy.mre.modules.pathfinding.engine;

import dev.sxmurxy.mre.modules.pathfinding.utils.PathNode;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class PathSmoother {
    public static List<Vec3d> smoothPath(List<PathNode> path, int pointsPerSegment) {
        if (path.size() < 4) {
            List<Vec3d> simplePath = new ArrayList<>();
            for(PathNode n : path) simplePath.add(Vec3d.ofCenter(n.position));
            return simplePath;
        }
        List<Vec3d> smoothedPath = new ArrayList<>();
        List<Vec3d> controlPoints = new ArrayList<>();
        for(PathNode n : path) controlPoints.add(Vec3d.ofCenter(n.position));
        controlPoints.add(0, controlPoints.get(0));
        controlPoints.add(controlPoints.get(controlPoints.size() - 1));
        for (int i = 0; i < controlPoints.size() - 3; i++) {
            for (int j = 0; j < pointsPerSegment; j++) {
                smoothedPath.add(catmullRom(controlPoints.get(i), controlPoints.get(i + 1), controlPoints.get(i + 2), controlPoints.get(i + 3), (float) j / pointsPerSegment));
            }
        }
        smoothedPath.add(controlPoints.get(controlPoints.size() - 2));
        return smoothedPath;
    }

    private static Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.x - 3 * p2.z + p3.z) * t3);
        return new Vec3d(x, y, z);
    }
}