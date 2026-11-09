package com.mcpmc.tools;

import com.mcpmc.strategy.StrategyRegistry;

import java.util.Map;

final class StrategyTools {
    private final ToolRegistry r;
    private final StrategyRegistry sr;
    StrategyTools(ToolRegistry r, StrategyRegistry sr) { this.r = r; this.sr = sr; }

    void register() {
        r.register(new Simple("strategy_list",
                "List registered high-level strategies (base_finder, shop_replay, ...). Each has an id, description, and args schema.",
                Map.of(),
                args -> Map.of("strategies", sr.describe())));

        r.register(new Simple("strategy_run",
                "Start a strategy. Pass id and its args map. Returns a run id you can poll with strategy_status.",
                Map.of("id", Tool.prop("string"), "args", Tool.prop("object")),
                args -> {
                    String id = Simple.str(args, "id");
                    @SuppressWarnings("unchecked")
                    Map<String,Object> params = (Map<String,Object>) args.getOrDefault("args", Map.of());
                    return Map.of("run_id", sr.start(id, params));
                }));

        r.register(new Simple("strategy_status",
                "Return the current state + progress + log of a running strategy.",
                Map.of("run_id", Tool.prop("string")),
                args -> sr.status(Simple.str(args, "run_id"))));

        r.register(new Simple("strategy_cancel",
                "Cancel a running strategy by id.",
                Map.of("run_id", Tool.prop("string")),
                args -> { sr.cancel(Simple.str(args, "run_id")); return Map.of("ok", true); }));

        r.register(new Simple("strategy_runs",
                "List every run with its state (running, done, cancelled, failed).",
                Map.of(),
                args -> Map.of("runs", sr.runs())));
    }
}
