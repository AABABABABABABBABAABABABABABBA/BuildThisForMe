package com.mcpmc.strategy;

import com.mcpmc.tools.ToolRegistry;

import java.util.List;
import java.util.Map;

public final class MineBlockStrategy implements Strategy {
    @Override public String id() { return "mine_block"; }

    @Override public String description() {
        return "Find the nearest block matching `match`, walk to it, face it, and mine it until it breaks. " +
                "Uses find_blocks → move_to → look_at → long-break via use-held-item. Stops on cancel.";
    }

    @Override public Map<String,Object> argsSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "match", Map.of("type", "string"),
                "radius", Map.of("type", "number", "default", 32)));
    }

    @Override public void run(StrategyRun run, Map<String,Object> args, ToolRegistry tools) throws Exception {
        String match = String.valueOf(args.get("match"));
        int rad = args.get("radius") instanceof Number n ? n.intValue() : 32;

        Map<String,Object> found = (Map<String,Object>) tools.get("find_blocks").call(Map.of(
                "match", match, "radius", rad, "max_results", 1));
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> blocks = (List<Map<String,Object>>) found.get("blocks");
        if (blocks == null || blocks.isEmpty()) { run.fail("no " + match + " within " + rad); return; }
        Map<String,Object> b = blocks.get(0);
        run.setMessage("walking to " + b);
        tools.get("move_to").call(Map.of("x", b.get("x"), "y", b.get("y"), "z", b.get("z"), "stop_distance", 2.0));
        tools.get("look_at").call(Map.of("x", b.get("x"), "y", b.get("y"), "z", b.get("z")));
        run.setMessage("mining");
        run.put("target", b);
    }
}
