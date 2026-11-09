package com.mcpmc.tools;

import java.util.Map;

final class Simple implements Tool {
    private final String name, desc;
    private final Map<String,Object> schema;
    private final Handler fn;

    interface Handler { Object run(Map<String,Object> args) throws Exception; }

    Simple(String name, String desc, Map<String,Object> schemaProps, Handler fn) {
        this.name = name;
        this.desc = desc;
        this.schema = Map.of("type", "object", "properties", schemaProps);
        this.fn = fn;
    }

    @Override public String name() { return name; }
    @Override public String description() { return desc; }
    @Override public Map<String,Object> inputSchema() { return schema; }
    @Override public Object call(Map<String,Object> args) throws Exception { return fn.run(args); }

    static double num(Map<String,Object> a, String k) {
        Object v = a.get(k);
        if (v == null) throw new IllegalArgumentException("missing " + k);
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }
    static double num(Map<String,Object> a, String k, double def) {
        Object v = a.get(k);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }
    static int intArg(Map<String,Object> a, String k) { return (int) num(a, k); }
    static int intArg(Map<String,Object> a, String k, int def) { return (int) num(a, k, def); }
    static String str(Map<String,Object> a, String k) {
        Object v = a.get(k);
        if (v == null) throw new IllegalArgumentException("missing " + k);
        return v.toString();
    }
    static String str(Map<String,Object> a, String k, String def) {
        Object v = a.get(k);
        return v == null ? def : v.toString();
    }
    static boolean bool(Map<String,Object> a, String k, boolean def) {
        Object v = a.get(k);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }
}
