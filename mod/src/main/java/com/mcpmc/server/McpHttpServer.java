package com.mcpmc.server;

import com.mcpmc.tools.Tool;
import com.mcpmc.tools.ToolRegistry;
import com.mcpmc.util.Log;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public final class McpHttpServer {
    private final int port;
    private final ToolRegistry tools;
    private HttpServer http;

    public McpHttpServer(int port, ToolRegistry tools) {
        this.port = port;
        this.tools = tools;
    }

    public void start() throws IOException {
        http = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 16);
        http.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "mcp-http");
            t.setDaemon(true);
            return t;
        }));

        http.createContext("/health", ex -> {
            byte[] body = ("{\"ok\":true,\"tools\":" + tools.size() + "}").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("content-type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        http.createContext("/", ex -> {
            if (!"POST".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String reply = dispatch(body);
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("content-type", "application/json");
            ex.sendResponseHeaders(200, out.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(out); }
        });

        http.start();
    }

    public void stop() {
        if (http != null) http.stop(0);
    }

    @SuppressWarnings("unchecked")
    private String dispatch(String body) {
        Object msgObj;
        try { msgObj = Json.parse(body); }
        catch (Exception e) { return error(null, -32700, "bad json: " + e.getMessage()); }

        if (!(msgObj instanceof Map)) return error(null, -32600, "expected object");
        Map<String,Object> msg = (Map<String,Object>) msgObj;
        Object id = msg.get("id");
        String method = String.valueOf(msg.get("method"));

        try {
            if ("initialize".equals(method)) {
                return reply(id, Map.of(
                        "protocolVersion", "2024-11-05",
                        "serverInfo", Map.of("name", "minecraft-mcp", "version", "1.0.0"),
                        "capabilities", Map.of("tools", Map.of())
                ));
            }
            if ("tools/list".equals(method)) {
                List<Map<String,Object>> list = new ArrayList<>();
                for (Tool t : tools.all()) {
                    list.add(Map.of(
                            "name", t.name(),
                            "description", t.description(),
                            "inputSchema", t.inputSchema()
                    ));
                }
                return reply(id, Map.of("tools", list));
            }
            if ("tools/call".equals(method)) {
                Map<String,Object> params = (Map<String,Object>) msg.getOrDefault("params", Map.of());
                String name = String.valueOf(params.get("name"));
                Map<String,Object> args = (Map<String,Object>) params.getOrDefault("arguments", Map.of());
                Tool tool = tools.get(name);
                if (tool == null) return error(id, -32601, "unknown tool: " + name);
                try {
                    Object result = tool.call(args);
                    return reply(id, Map.of(
                            "content", List.of(Map.of("type", "text", "text", Json.stringify(result)))
                    ));
                } catch (Exception e) {
                    Log.error("tool " + name + " failed", e);
                    return reply(id, Map.of(
                            "isError", true,
                            "content", List.of(Map.of("type", "text", "text", e.getMessage()))
                    ));
                }
            }
            if (method != null && method.startsWith("notifications/")) {
                return "{}";
            }
            return error(id, -32601, "method not found: " + method);
        } catch (Exception e) {
            Log.error("dispatch failed", e);
            return error(id, -32603, e.getMessage());
        }
    }

    private static String reply(Object id, Object result) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("result", result);
        return Json.stringify(m);
    }

    private static String error(Object id, int code, String message) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("error", Map.of("code", code, "message", message));
        return Json.stringify(m);
    }
}
