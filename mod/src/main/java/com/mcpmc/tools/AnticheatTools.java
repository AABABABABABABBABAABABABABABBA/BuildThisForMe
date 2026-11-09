package com.mcpmc.tools;

import com.mcpmc.anticheat.CadenceLimiter;
import com.mcpmc.anticheat.FingerprintAnalyzer;
import com.mcpmc.anticheat.VersionProber;
import com.mcpmc.anticheat.VulnerabilityDatabase;
import com.mcpmc.anticheat.VulnerabilityDatabase.Finding;

import java.util.*;

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

        r.register(new Simple("anticheat_vulnerabilities",
                "Return publicly documented bypass findings against the detected anti-cheat (or against `name` if provided). Each entry has id, affected_versions, fixed_in, severity, title, category, description, reference. Research data, not exploit code.",
                Map.of("name", Tool.prop("string"), "version", Tool.prop("string")),
                args -> {
                    String name = Simple.str(args, "name", ac.snapshot().get("detected") == null ? null
                            : String.valueOf(ac.snapshot().get("detected")));
                    String version = Simple.str(args, "version", null);
                    List<Map<String,Object>> hits = new ArrayList<>();
                    for (Finding f : VulnerabilityDatabase.findFor(name)) {
                        if (VulnerabilityDatabase.isLikelyAffected(f, version)) {
                            hits.add(VulnerabilityDatabase.asMap(f));
                        }
                    }
                    return Map.of(
                            "anti_cheat", name,
                            "assumed_version", version,
                            "finding_count", hits.size(),
                            "findings", hits);
                }));

        r.register(new Simple("anticheat_check",
                "Full report: fingerprint + extracted version (from chat scan) + likely applicable vulnerabilities + recommended probe commands (e.g. /plugins, /grim version) to get a confirmed version string.",
                Map.of(),
                args -> {
                    Map<String,Object> snap = ac.snapshot();
                    String detected = snap.get("detected") == null ? null : String.valueOf(snap.get("detected"));
                    List<String> names = detected == null
                            ? List.of("Grim","Vulcan","Matrix","Verus","NCP","AAC","Spartan","Themis","Horizon","Hawk","Karhu","Witherac","Intave","Polar")
                            : List.of(detected);
                    Map<String,String> versions = VersionProber.scanChatForVersions(names);
                    String version = detected == null ? null : versions.get(detected);

                    List<Map<String,Object>> vulns = new ArrayList<>();
                    for (Finding f : VulnerabilityDatabase.findFor(detected))
                        if (VulnerabilityDatabase.isLikelyAffected(f, version))
                            vulns.add(VulnerabilityDatabase.asMap(f));

                    Map<String,Object> out = new LinkedHashMap<>();
                    out.put("fingerprint", snap);
                    out.put("version_from_chat", versions);
                    out.put("likely_vulnerabilities", vulns);
                    out.put("probe_commands", VersionProber.probeCommands(detected));
                    out.put("note", "This is curated public research against the named plugin's release notes and public bypass reports. For authorized testing only.");
                    return out;
                }));

        r.register(new Simple("anticheat_database",
                "Return the full curated vulnerability database (all anti-cheats, every known finding). For offline review.",
                Map.of(),
                args -> {
                    List<Map<String,Object>> all = new ArrayList<>();
                    for (Finding f : VulnerabilityDatabase.all()) all.add(VulnerabilityDatabase.asMap(f));
                    return Map.of("count", all.size(), "findings", all);
                }));
    }
}
