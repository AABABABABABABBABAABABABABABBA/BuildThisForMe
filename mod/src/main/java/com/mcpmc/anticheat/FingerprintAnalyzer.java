package com.mcpmc.anticheat;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public final class FingerprintAnalyzer {

    private static final Map<String, String[]> FINGERPRINTS = Map.ofEntries(
            Map.entry("Grim",     new String[]{"grim", "[grim]", "gac-"}),
            Map.entry("Vulcan",   new String[]{"vulcan", "[vulcan]"}),
            Map.entry("Matrix",   new String[]{"matrix", "[matrix]"}),
            Map.entry("Verus",    new String[]{"verus", "[verus]"}),
            Map.entry("NCP",      new String[]{"nocheatplus", "ncp"}),
            Map.entry("AAC",      new String[]{"advanced anti cheat", "aac"}),
            Map.entry("Spartan",  new String[]{"spartan"}),
            Map.entry("Themis",   new String[]{"themis"}),
            Map.entry("Horizon",  new String[]{"horizon"}),
            Map.entry("Hawk",     new String[]{"hawk"}),
            Map.entry("Karhu",    new String[]{"karhu"}),
            Map.entry("Witherac", new String[]{"witherac"}),
            Map.entry("Intave",   new String[]{"intave"}),
            Map.entry("Polar",    new String[]{"polar"}));

    private static final String[] FLAG_HINTS = {
            "flagged","violation","suspicious","moving too fast","cheating",
            "reach","killaura","fly","speed","scaffold","aimbot",
            "anti knockback","nofall","phase","autoclick","illegal action"
    };

    private volatile String name;
    private volatile double confidence;
    private final AtomicInteger rubberBands = new AtomicInteger();
    private volatile long lastRubberBandAt;
    private final Deque<Map<String,Object>> signals = new ConcurrentLinkedDeque<>();
    private final Deque<Map<String,Object>> corrections = new ConcurrentLinkedDeque<>();
    private volatile Map<String,Object> lastKick;

    public synchronized void reset() {
        name = null; confidence = 0;
        rubberBands.set(0); lastRubberBandAt = 0;
        signals.clear(); corrections.clear();
        lastKick = null;
    }

    public void onChat(String text) {
        if (text == null) return;
        String t = text.toLowerCase();
        matchFingerprint(t, "chat", 0.7);
        scanFlags(t, "chat");
    }

    public void onKick(String text) {
        lastKick = Map.of("at", System.currentTimeMillis(), "reason", text == null ? "" : text);
        if (text == null) return;
        String t = text.toLowerCase();
        matchFingerprint(t, "kick", 0.95);
        scanFlags(t, "kick");
    }

    public void onPluginChannel(String channel) {
        String c = channel.toLowerCase();
        for (var e : FINGERPRINTS.entrySet()) {
            for (String kw : e.getValue()) {
                if (c.contains(kw)) {
                    mark(e.getKey(), "plugin_channel:" + channel, 0.85);
                    return;
                }
            }
        }
    }

    public void onServerCorrection(double x, double y, double z) {
        rubberBands.incrementAndGet();
        lastRubberBandAt = System.currentTimeMillis();
        pushCapped(corrections, Map.of("at", lastRubberBandAt, "x", x, "y", y, "z", z), 50);
        if (rubberBands.get() > 5 && name == null) {
            signal(Map.of("kind", "rubberband_burst", "at", lastRubberBandAt));
        }
    }

    private void matchFingerprint(String text, String source, double conf) {
        for (var e : FINGERPRINTS.entrySet()) {
            for (String kw : e.getValue()) {
                if (text.contains(kw)) { mark(e.getKey(), source + ":" + kw, conf); return; }
            }
        }
    }

    private void scanFlags(String text, String source) {
        for (String h : FLAG_HINTS) {
            if (text.contains(h)) {
                signal(Map.of("kind", "flag", "hint", h, "source", source, "at", System.currentTimeMillis()));
                return;
            }
        }
    }

    private void mark(String n, String source, double conf) {
        if (conf <= confidence && n.equals(name)) return;
        name = n;
        confidence = Math.max(confidence, conf);
        signal(Map.of("kind", "fingerprint", "name", n, "source", source, "at", System.currentTimeMillis()));
    }

    private void signal(Map<String,Object> s) { pushCapped(signals, s, 30); }

    private static <T> void pushCapped(Deque<T> dq, T v, int cap) {
        dq.addLast(v);
        while (dq.size() > cap) dq.pollFirst();
    }

    public Map<String,Object> snapshot() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("detected", name);
        m.put("confidence", confidence);
        m.put("rubber_bands", rubberBands.get());
        m.put("last_rubber_band_at", lastRubberBandAt);
        m.put("signals", new ArrayList<>(signals));
        m.put("recent_corrections", new ArrayList<>(corrections));
        m.put("last_kick", lastKick);
        m.put("mitigations", mitigations());
        return m;
    }

    private List<String> mitigations() {
        List<String> tips = new ArrayList<>();
        if (name == null) tips.add("no fingerprint yet — give it a few minutes of play, or trigger flags on a dev server");
        if (rubberBands.get() > 0) tips.add("server is correcting your position — keep per-tick deltas inside vanilla caps (~0.6 walk, ~0.28 air)");
        tips.add("always send on_ground truthfully; false ground states flag fly/nofall checks");
        tips.add("swing before use_entity for attack packets");
        tips.add("respect teleport_confirm packets; silent drops flag timer checks");
        tips.add("cap click cadence at ~8-12 CPS with jitter; perfect intervals flag auto-click detectors");
        if ("Grim".equals(name) || "Vulcan".equals(name))
            tips.add(name + " runs server-side prediction — any physics desync flags instantly, no client-side trick evades it");
        return tips;
    }
}
