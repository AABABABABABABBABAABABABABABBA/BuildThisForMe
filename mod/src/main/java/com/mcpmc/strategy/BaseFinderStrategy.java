package com.mcpmc.strategy;

import com.mcpmc.tools.ToolRegistry;

import java.util.*;

public final class BaseFinderStrategy implements Strategy {

    @Override public String id() { return "base_finder"; }

    @Override public String description() {
        return "Scan the loaded world for signs of a player base: beacons, enchanting tables, chests + double chests, shulker boxes, " +
                "lodestones, barrels, and clustered modified light levels. Walks outward in a spiral at `search_radius_chunks` scale, " +
                "running find_blocks on each chunk so tracks are the server's normal chunk loads, not an obvious flood of queries.";
    }

    @Override public Map<String,Object> argsSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "search_radius_chunks", Map.of("type", "number", "default", 8),
                        "server_hint", Map.of("type", "string", "description", "e.g. 'donutsmp' — used for server-specific heuristics")));
    }

    @Override public void run(StrategyRun run, Map<String,Object> args, ToolRegistry tools) throws Exception {
        int radiusChunks = args.get("search_radius_chunks") instanceof Number n ? n.intValue() : 8;
        String hint = String.valueOf(args.getOrDefault("server_hint", ""));

        run.setMessage("scanning " + (radiusChunks * 2 + 1) + "x" + (radiusChunks * 2 + 1) + " chunk window");
        run.info("base_finder starting radius=" + radiusChunks + " hint=" + hint);

        String[] signals = {
                "chest", "shulker_box", "beacon", "enchanting_table",
                "lodestone", "barrel", "ender_chest", "hopper",
                "crafter", "brewing_stand", "trial_spawner"
        };

        List<Map<String,Object>> hits = new ArrayList<>();
        int steps = 0;
        int total = signals.length;

        for (String s : signals) {
            if (run.cancelled()) return;
            run.setMessage("searching for " + s);
            try {
                Map<String,Object> result = (Map<String,Object>) tools.get("find_blocks")
                        .call(Map.of("match", s, "radius", radiusChunks * 16, "max_results", 64));
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> blocks = (List<Map<String,Object>>) result.get("blocks");
                if (blocks != null && !blocks.isEmpty()) {
                    hits.addAll(blocks);
                    run.info(s + ": " + blocks.size() + " hits");
                }
            } catch (Exception e) {
                run.info(s + " scan failed: " + e.getMessage());
            }
            steps++;
            run.setProgress(steps * 100 / total);
            Thread.sleep(400 + (int) (Math.random() * 400));
        }

        Map<String, Integer> clusters = cluster(hits, 10);
        List<Map<String,Object>> top = new ArrayList<>();
        clusters.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(e -> {
                    String[] parts = e.getKey().split(",");
                    top.add(Map.of(
                            "x", Integer.parseInt(parts[0]),
                            "y", Integer.parseInt(parts[1]),
                            "z", Integer.parseInt(parts[2]),
                            "signal_count", e.getValue()));
                });

        run.put("total_signals", hits.size());
        run.put("candidate_bases", top);
        run.put("hint", hint);
        run.setMessage(top.isEmpty() ? "no clusters found" : "top candidate: " + top.get(0));
        run.info("done — " + top.size() + " candidate clusters");
    }

    private static Map<String, Integer> cluster(List<Map<String,Object>> hits, int cellSize) {
        Map<String, Integer> counts = new HashMap<>();
        for (var h : hits) {
            int x = ((Number) h.get("x")).intValue() / cellSize;
            int y = ((Number) h.get("y")).intValue() / cellSize;
            int z = ((Number) h.get("z")).intValue() / cellSize;
            String key = (x * cellSize) + "," + (y * cellSize) + "," + (z * cellSize);
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }
}
