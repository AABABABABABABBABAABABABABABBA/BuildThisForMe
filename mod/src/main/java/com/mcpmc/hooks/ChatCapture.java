package com.mcpmc.hooks;

import com.mcpmc.anticheat.FingerprintAnalyzer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class ChatCapture {
    private ChatCapture() {}

    private static final Deque<Map<String,Object>> buffer = new ConcurrentLinkedDeque<>();
    private static final int CAP = 500;
    private static volatile FingerprintAnalyzer ac;

    public static void install(FingerprintAnalyzer analyzer) {
        ac = analyzer;
        ClientReceiveMessageEvents.ALLOW_CHAT.register((msg, sig, sender, params, ts) -> {
            record("chat", msg.getString(), sender != null ? sender.getName() : null);
            return true;
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((msg, overlay) -> {
            record(overlay ? "overlay" : "system", msg.getString(), null);
            return true;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, c) -> {
            // kick reason captured in PacketCapture.onDisconnect via mixin or fallback
        });
    }

    private static void record(String kind, String text, String sender) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("at", System.currentTimeMillis());
        m.put("kind", kind);
        m.put("text", text);
        if (sender != null) m.put("sender", sender);
        buffer.addLast(m);
        while (buffer.size() > CAP) buffer.pollFirst();
        if (ac != null) ac.onChat(text);
    }

    public static List<Map<String,Object>> recent(int limit) {
        List<Map<String,Object>> list = new ArrayList<>(buffer);
        if (list.size() <= limit) return list;
        return list.subList(list.size() - limit, list.size());
    }

    public static List<Map<String,Object>> search(String needle, int limit) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var m : buffer) {
            String t = String.valueOf(m.get("text")).toLowerCase();
            if (t.contains(needle)) out.add(m);
            if (out.size() >= limit) break;
        }
        return out;
    }

    public static void clear() { buffer.clear(); }
}
