package com.mcpmc.strategy;

import com.mcpmc.tools.ToolRegistry;

import java.util.Map;

public final class ShopReplayStrategy implements Strategy {
    @Override public String id() { return "shop_replay"; }

    @Override public String description() {
        return "Replay a recorded container-click sequence (shop purchase, auction click path, etc.) a given number of times, " +
                "with jittered delays. Pair with container_record / container_stop_recording to capture the sequence first.";
    }

    @Override public Map<String,Object> argsSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "recording", Map.of("type", "string"),
                "times", Map.of("type", "number", "default", 1),
                "delay_ms", Map.of("type", "number", "default", 200)));
    }

    @Override public void run(StrategyRun run, Map<String,Object> args, ToolRegistry tools) throws Exception {
        String name = String.valueOf(args.get("recording"));
        int times = args.get("times") instanceof Number n ? n.intValue() : 1;
        long delay = args.get("delay_ms") instanceof Number d ? d.longValue() : 200;

        for (int i = 0; i < times; i++) {
            if (run.cancelled()) return;
            run.setMessage("replay " + (i + 1) + "/" + times);
            tools.get("container_replay").call(Map.of("name", name, "delay_ms", delay));
            run.setProgress(((i + 1) * 100) / times);
            Thread.sleep(delay + (long) (Math.random() * 200));
        }
        run.put("completed", times);
    }
}
