package com.mcpmc.anticheat;

import java.util.concurrent.ThreadLocalRandom;

public final class CadenceLimiter {
    private CadenceLimiter() {}

    private static volatile double maxCps = 8.0;
    private static volatile double rotationCap = 35.0;
    private static volatile int jitter = 15;
    private static volatile long lastAttackAt = 0;

    public static double maxCps() { return maxCps; }
    public static double rotationCap() { return rotationCap; }
    public static int jitter() { return jitter; }

    public static void setMaxCps(double v) { maxCps = Math.max(0.5, Math.min(30, v)); }
    public static void setRotationCap(double v) { rotationCap = Math.max(1, Math.min(180, v)); }
    public static void setJitter(int ms) { jitter = Math.max(0, Math.min(500, ms)); }

    public static void gateAttack() throws InterruptedException {
        long now = System.currentTimeMillis();
        long minGapMs = (long) (1000.0 / maxCps);
        long delta = now - lastAttackAt;
        if (delta < minGapMs) {
            long sleep = (minGapMs - delta) + ThreadLocalRandom.current().nextInt(jitter + 1);
            Thread.sleep(sleep);
        }
        lastAttackAt = System.currentTimeMillis();
    }

    public static float smoothYaw(float current, float target) {
        return clampDelta(current, wrap(target), (float) rotationCap);
    }

    public static float smoothPitch(float current, float target) {
        float t = Math.max(-90f, Math.min(90f, target));
        return clampDelta(current, t, (float) rotationCap);
    }

    private static float clampDelta(float cur, float tgt, float cap) {
        float d = wrap(tgt - cur);
        if (d > cap) d = cap;
        if (d < -cap) d = -cap;
        return cur + d;
    }

    private static float wrap(float a) {
        a = a % 360f;
        if (a >= 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }
}
