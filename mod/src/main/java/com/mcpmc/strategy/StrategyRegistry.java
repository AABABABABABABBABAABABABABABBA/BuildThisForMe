package com.mcpmc.strategy;

import com.mcpmc.tools.ToolRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class StrategyRegistry {
    private final ToolRegistry tools;
    private final Map<String, Strategy> strategies = new LinkedHashMap<>();
    private final Map<String, StrategyRun> runs = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    public StrategyRegistry(ToolRegistry tools) { this.tools = tools; }

    public void register(Strategy s) {
        if (strategies.containsKey(s.id())) throw new IllegalStateException("duplicate: " + s.id());
        strategies.put(s.id(), s);
    }

    public void registerDefaults() {
        register(new BaseFinderStrategy());
        register(new ShopReplayStrategy());
        register(new FollowStrategy());
        register(new MineBlockStrategy());
    }

    public int size() { return strategies.size(); }

    public List<Map<String,Object>> describe() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (Strategy s : strategies.values())
            out.add(Map.of("id", s.id(), "description", s.description(), "args_schema", s.argsSchema()));
        return out;
    }

    public String start(String id, Map<String,Object> args) {
        Strategy s = strategies.get(id);
        if (s == null) throw new IllegalArgumentException("no strategy: " + id);
        String runId = "run_" + seq.incrementAndGet();
        StrategyRun run = new StrategyRun(runId, id);
        runs.put(runId, run);
        Thread t = new Thread(() -> {
            try {
                s.run(run, args, tools);
                if (run.snapshot().get("state").equals("RUNNING")) run.done();
            } catch (InterruptedException e) {
                run.info("cancelled");
            } catch (Exception e) {
                run.info("ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
                run.fail(e.getMessage());
            }
        }, "strategy-" + id);
        t.setDaemon(true);
        run.bindThread(t);
        t.start();
        return runId;
    }

    public Map<String,Object> status(String runId) {
        StrategyRun r = runs.get(runId);
        if (r == null) throw new IllegalArgumentException("no run: " + runId);
        return r.snapshot();
    }

    public void cancel(String runId) {
        StrategyRun r = runs.get(runId);
        if (r == null) throw new IllegalArgumentException("no run: " + runId);
        r.cancel();
    }

    public List<Map<String,Object>> runs() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var r : runs.values()) out.add(r.snapshot());
        return out;
    }
}
