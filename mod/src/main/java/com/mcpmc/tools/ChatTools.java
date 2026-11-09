package com.mcpmc.tools;

import com.mcpmc.hooks.ChatCapture;

import java.util.List;
import java.util.Map;

final class ChatTools {
    private final ToolRegistry r;
    ChatTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("chat_read",
                "Return recent in-game chat/system messages the player received. Pass limit for how many.",
                Map.of("limit", Tool.prop("number")),
                args -> Map.of("messages", ChatCapture.recent(Simple.intArg(args, "limit", 50)))));

        r.register(new Simple("chat_send",
                "Send a chat line or slash command. Whatever you'd type in the chat box.",
                Map.of("message", Tool.prop("string")),
                args -> Mc.onClientThread(() -> {
                    String msg = Simple.str(args, "message");
                    if (msg.startsWith("/")) Mc.net().sendChatCommand(msg.substring(1));
                    else Mc.net().sendChatMessage(msg);
                    return Map.of("sent", msg);
                })));

        r.register(new Simple("chat_search",
                "Search recent chat for a substring (case-insensitive). Useful for grep-ing shop prices, kick reasons, etc.",
                Map.of("pattern", Tool.prop("string"), "limit", Tool.prop("number")),
                args -> {
                    String pat = Simple.str(args, "pattern").toLowerCase();
                    int limit = Simple.intArg(args, "limit", 20);
                    List<Map<String,Object>> hits = ChatCapture.search(pat, limit);
                    return Map.of("matches", hits);
                }));

        r.register(new Simple("chat_clear",
                "Clear the captured chat buffer.",
                Map.of(),
                args -> { ChatCapture.clear(); return Map.of("ok", true); }));
    }
}
