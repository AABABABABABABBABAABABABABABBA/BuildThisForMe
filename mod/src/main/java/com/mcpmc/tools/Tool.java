package com.mcpmc.tools;

import java.util.Map;

public interface Tool {
    String name();
    String description();
    Map<String,Object> inputSchema();
    Object call(Map<String,Object> args) throws Exception;

    static Map<String,Object> obj(Object... kv) {
        var m = new java.util.LinkedHashMap<String,Object>();
        for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    static Map<String,Object> prop(String type) { return Map.of("type", type); }
    static Map<String,Object> prop(String type, String desc) { return Map.of("type", type, "description", desc); }
}
