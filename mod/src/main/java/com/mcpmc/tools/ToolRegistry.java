package com.mcpmc.tools;

import com.mcpmc.anticheat.FingerprintAnalyzer;
import com.mcpmc.strategy.StrategyRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool t) {
        if (tools.containsKey(t.name())) throw new IllegalStateException("duplicate tool: " + t.name());
        tools.put(t.name(), t);
    }

    public Tool get(String name) { return tools.get(name); }
    public Collection<Tool> all() { return tools.values(); }
    public int size() { return tools.size(); }

    public void registerCore(FingerprintAnalyzer ac, StrategyRegistry strategies) {
        new CoreTools(this).register();
        new ChatTools(this).register();
        new MovementTools(this).register();
        new WorldTools(this).register();
        new EntityTools(this).register();
        new InventoryTools(this).register();
        new ContainerTools(this).register();
        new GuiTools(this).register();
        new ServerTools(this).register();
        new RegistryTools(this).register();
        new AnticheatTools(this, ac).register();
        new StrategyTools(this, strategies).register();
    }
}
