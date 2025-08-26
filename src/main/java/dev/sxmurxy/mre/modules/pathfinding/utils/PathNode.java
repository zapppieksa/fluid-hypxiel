// File: utils/PathNode.java
package dev.sxmurxy.mre.modules.pathfinding.utils;

import net.minecraft.util.math.BlockPos;

public class PathNode implements Comparable<PathNode> {
    public final BlockPos position;
    public PathNode parent;
    public double gCost = Double.MAX_VALUE;
    public double hCost = 0;
    public double fCost = Double.MAX_VALUE;

    public PathNode(BlockPos position, PathNode parent) {
        this.position = position;
        this.parent = parent;
    }

    public void calculateFCost() {
        this.fCost = this.gCost + this.hCost;
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost, other.fCost);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PathNode && position.equals(((PathNode) obj).position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }
}