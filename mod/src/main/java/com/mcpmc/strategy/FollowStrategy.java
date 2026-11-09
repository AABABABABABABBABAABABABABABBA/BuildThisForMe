package com.mcpmc.strategy;

import com.mcpmc.tools.ToolRegistry;

import java.util.List;
import java.util.Map;

public final class FollowStrategy implements Strategy {
    @Override public String id() { return "follow_player"; }

    @Override public String description() {
        return "Follow a named player until cancelled. Stays at `distance` blocks, sprints if they get further than `sprint_at`.";
    }

    @Override public Map<String,Object> argsSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "player_name", Map.of("type", "string"),
                "distance", Map.of("type", "number", "default", 3),
                "sprint_at", Map.of("type", "number", "default", 8)));
    }

    @Override public void run(StrategyRun run, Map<String,Object> args, ToolRegistry tools) throws Exception {
        String name = String.valueOf(args.get("player_name"));
        double distance = args.get("distance") instanceof Number n ? n.doubleValue() : 3;
        double sprintAt = args.get("sprint_at") instanceof Number s ? s.doubleValue() : 8;

        while (!run.cancelled()) {
            Map<String,Object> near = (Map<String,Object>) tools.get("entities_nearby").call(Map.of("radius", 64, "filter", "player"));
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> list = (List<Map<String,Object>>) near.get("entities");
            Map<String,Object> target = null;
            for (var e : list) {
                if (name.equalsIgnoreCase(String.valueOf(e.get("name")))) { target = e; break; }
            }
            if (target == null) { run.setMessage("waiting for " + name); Thread.sleep(500); continue; }
            Map<String,Object> pos = (Map<String,Object>) target.get("position");
            double d = (double) target.get("distance");
            if (d < distance) { Thread.sleep(200); continue; }
            tools.get("move_to").call(Map.of(
                    "x", pos.get("x"), "y", pos.get("y"), "z", pos.get("z"),
                    "sprint", d > sprintAt, "stop_distance", distance));
        }
    }
}
