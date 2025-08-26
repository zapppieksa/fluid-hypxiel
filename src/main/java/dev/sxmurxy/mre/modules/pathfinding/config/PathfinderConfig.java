package dev.sxmurxy.mre.modules.pathfinding.config;

import net.minecraft.client.network.ClientPlayerEntity;

public class PathfinderConfig {

    // --- Engine Settings ---
    public int MAX_PATHFINDING_NODES = 35000;
    public int PATH_SMOOTHNESS = 10; // Points per segment

    // --- Movement Settings ---
    public double MOVEMENT_WOBBLE_INTENSITY = 0.2;
    public int BHOP_CHANCE_PERCENT = 70;
    public int BHOP_COOLDOWN_TICKS = 10;
    public float STRAFE_ANGLE_THRESHOLD = 5.0f;
    public double JUMP_HEIGHT_THRESHOLD = 0.8;
    public float SPRINT_MAX_ANGLE_CURRENT = 25.0f;
    public float SPRINT_MAX_ANGLE_NEXT = 40.0f;

    // --- Rotation Settings ---
    public float ROTATION_SMOOTH_FACTOR = 0.15f;
    public double MIN_PITCH = -25.0;
    public double MAX_PITCH = 30.0;
    public double ROTATION_JITTER_YAW = 2.0;
    public double ROTATION_JITTER_PITCH = 1.5;
    public int SACCADE_CHANCE_PERCENT = 5;
    public int SACCADE_DURATION_TICKS = 20;

    // --- Stuck Detection ---
    public int STUCK_HISTORY_SIZE = 40;
    public double STUCK_DISTANCE_THRESHOLD = 0.1;
    public int RECALCULATE_STUCK_TICKS = 60;

    // --- Dynamic Settings ---
    public double getReachDistance(ClientPlayerEntity player) {
        double baseDistance = 1.2;
        double speed = player.getVelocity().horizontalLength();
        return baseDistance + speed * 3.5;
    }
}
