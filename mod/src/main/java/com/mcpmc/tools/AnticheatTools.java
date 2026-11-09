package com.mcpmc.tools;

import com.mcpmc.anticheat.CadenceLimiter;
import com.mcpmc.anticheat.FingerprintAnalyzer;

import java.util.Map;

final class AnticheatTools {
    private final ToolRegistry r;
    private final FingerprintAnalyzer ac;
    AnticheatTools(ToolRegistry r, FingerprintAnalyzer ac) { this.r = r; this.ac = ac; }

    void register() {
        r.register(new Simple("anticheat_read",
                "Return the detected anti-cheat fingerprint, confidence, observed flag signals, recent server position corrections, and mitigation tips. Passive observation.",
                Map.of(),
                args -> ac.snapshot()));

        r.register(new Simple("anticheat_tune",
                "Tune the cadence limiter. max_cps caps attack clicks per second (default 8). rotation_speed_cap limits per-tick yaw delta in degrees (default 35). jitter_ms randomizes action delays (default 15).",
                Map.of("max_cps", Tool.prop("number"), "rotation_speed_cap", Tool.prop("number"), "jitter_ms", Tool.prop("number")),
                args -> {
                    CadenceLimiter.setMaxCps(Simple.num(args, "max_cps", CadenceLimiter.maxCps()));
                    CadenceLimiter.setRotationCap(Simple.num(args, "rotation_speed_cap", CadenceLimiter.rotationCap()));
                    CadenceLimiter.setJitter((int) Simple.num(args, "jitter_ms", CadenceLimiter.jitter()));
                    return Map.of(
                            "max_cps", CadenceLimiter.maxCps(),
                            "rotation_speed_cap", CadenceLimiter.rotationCap(),
                            "jitter_ms", CadenceLimiter.jitter());
                }));

        r.register(new Simple("anticheat_reset",
                "Reset the fingerprint analyzer state.",
                Map.of(),
                args -> { ac.reset(); return Map.of("ok", true); }));
    }
}
