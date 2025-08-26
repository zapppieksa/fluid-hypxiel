package dev.sxmurxy.mre.modules.pathfinding.engine;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class MovementPredictor {
    private final Queue<Vec3d> positionHistory = new ArrayDeque<>();
    private final Queue<Long> timeHistory = new ArrayDeque<>();
    private static final int HISTORY_SIZE = 10;

    private Vec3d predictedVelocity = Vec3d.ZERO;
    private Vec3d acceleration = Vec3d.ZERO;

    public void update(ClientPlayerEntity player) {
        Vec3d currentPos = player.getPos();
        long currentTime = System.currentTimeMillis();

        positionHistory.offer(currentPos);
        timeHistory.offer(currentTime);

        // Maintain history size
        while (positionHistory.size() > HISTORY_SIZE) {
            positionHistory.poll();
            timeHistory.poll();
        }

        updatePredictions();
    }

    private void updatePredictions() {
        if (positionHistory.size() < 3) return;

        List<Vec3d> positions = new ArrayList<>(positionHistory);
        List<Long> times = new ArrayList<>(timeHistory);

        // Calculate velocity
        Vec3d recentVelocity = positions.get(positions.size() - 1)
                .subtract(positions.get(positions.size() - 2))
                .multiply(1000.0 / Math.max(1, times.get(times.size() - 1) - times.get(times.size() - 2)));

        // Smooth velocity calculation
        predictedVelocity = predictedVelocity.multiply(0.7).add(recentVelocity.multiply(0.3));

        // Calculate acceleration
        if (positions.size() >= 3) {
            Vec3d prevVelocity = positions.get(positions.size() - 2)
                    .subtract(positions.get(positions.size() - 3))
                    .multiply(1000.0 / Math.max(1, times.get(times.size() - 2) - times.get(times.size() - 3)));

            Vec3d recentAcceleration = recentVelocity.subtract(prevVelocity);
            acceleration = acceleration.multiply(0.8).add(recentAcceleration.multiply(0.2));
        }
    }

    public Vec3d predictPosition(double secondsAhead) {
        if (positionHistory.isEmpty()) return Vec3d.ZERO;

        Vec3d currentPos = positionHistory.peek();
        Vec3d predictedPos = currentPos
                .add(predictedVelocity.multiply(secondsAhead))
                .add(acceleration.multiply(secondsAhead * secondsAhead * 0.5));

        return predictedPos;
    }

    public Vec3d getPredictedVelocity() {
        return predictedVelocity;
    }

    public boolean isPlayerStuck() {
        if (positionHistory.size() < HISTORY_SIZE) return false;

        List<Vec3d> positions = new ArrayList<>(positionHistory);
        Vec3d center = positions.stream()
                .reduce(Vec3d.ZERO, Vec3d::add)
                .multiply(1.0 / positions.size());

        double maxDistance = positions.stream()
                .mapToDouble(pos -> pos.distanceTo(center))
                .max()
                .orElse(0);

        return maxDistance < 0.5; // Stuck if moving in less than 0.5 block radius
    }
}