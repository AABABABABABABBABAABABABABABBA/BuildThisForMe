package com.mcpmc.strategy;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class StrategyRun {
    public enum State { RUNNING, DONE, CANCELLED, FAILED }

    private final String id;
    private final String strategyId;
    private final long startedAt = System.currentTimeMillis();
    private volatile State state = State.RUNNING;
    private volatile String message;
    private volatile int progress;
    private volatile Thread thread;
    private final Deque<String> log = new ConcurrentLinkedDeque<>();
    private final Map<String,Object> result = new LinkedHashMap<>();

    public StrategyRun(String id, String strategyId) {
        this.id = id;
        this.strategyId = strategyId;
    }

    public String id() { return id; }
    public String strategyId() { return strategyId; }
    public void bindThread(Thread t) { this.thread = t; }
    public void setProgress(int pct) { this.progress = Math.max(0, Math.min(100, pct)); }
    public void setMessage(String m) { this.message = m; }
    public void info(String line) {
        log.addLast("[" + System.currentTimeMillis() + "] " + line);
        while (log.size() > 200) log.pollFirst();
    }
    public void done() { state = State.DONE; progress = 100; }
    public void fail(String m) { state = State.FAILED; message = m; }
    public void cancel() {
        state = State.CANCELLED;
        if (thread != null) thread.interrupt();
    }
    public boolean cancelled() { return state == State.CANCELLED || Thread.currentThread().isInterrupted(); }
    public void put(String key, Object val) { result.put(key, val); }

    public Map<String,Object> snapshot() {
        return Map.of(
                "run_id", id,
                "strategy", strategyId,
                "state", state.name(),
                "progress", progress,
                "message", message,
                "started_at", startedAt,
                "log", new ArrayList<>(log),
                "result", new LinkedHashMap<>(result));
    }
}
