package com.mcpmc.strategy;

import com.mcpmc.tools.ToolRegistry;

import java.util.Map;

public interface Strategy {
    String id();
    String description();
    Map<String,Object> argsSchema();
    void run(StrategyRun run, Map<String,Object> args, ToolRegistry tools) throws Exception;
}
