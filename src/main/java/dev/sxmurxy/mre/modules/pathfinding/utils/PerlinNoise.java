// File: utils/PerlinNoise.java
package dev.sxmurxy.mre.modules.pathfinding.utils;

import java.util.Random;

public class PerlinNoise {
    private final int[] p = new int[512];
    public PerlinNoise(int seed) {
        Random rand = new Random(seed);
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) permutation[i] = i;
        for (int i = 255; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            int temp = permutation[i]; permutation[i] = permutation[index]; permutation[index] = temp;
        }
        for (int i = 0; i < 256; i++) p[i] = p[i + 256] = permutation[i];
    }
    public double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255, Y = (int) Math.floor(y) & 255;
        x -= Math.floor(x); y -= Math.floor(y);
        double u = fade(x), v = fade(y);
        int a = p[X] + Y, b = p[X + 1] + Y;
        return lerp(v, lerp(u, grad(p[a], x, y), grad(p[b], x - 1, y)), lerp(u, grad(p[a + 1], x, y - 1), grad(p[b + 1], x - 1, y - 1)));
    }
    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double grad(int hash, double x, double y) {
        int h = hash & 15; double u = h < 8 ? x : y, v = h < 4 ? y : h == 12 || h == 14 ? x : 0;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}