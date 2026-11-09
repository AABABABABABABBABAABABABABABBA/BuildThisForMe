package com.mcpmc.tools;

import com.mcpmc.hooks.GuiCapture;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;

import java.util.*;

final class GuiTools {
    private final ToolRegistry r;
    GuiTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("tablist_read",
                "Read the player tab list (Tab key) with every entry's name, uuid, latency, gamemode, display name.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    Collection<PlayerListEntry> entries = Mc.net().getPlayerList();
                    List<Map<String,Object>> list = new ArrayList<>();
                    for (PlayerListEntry e : entries) {
                        Map<String,Object> m = new LinkedHashMap<>();
                        m.put("name", e.getProfile().getName());
                        m.put("uuid", e.getProfile().getId().toString());
                        m.put("latency_ms", e.getLatency());
                        m.put("gamemode", e.getGameMode() != null ? e.getGameMode().getName() : null);
                        if (e.getDisplayName() != null) m.put("display_name", e.getDisplayName().getString());
                        list.add(m);
                    }
                    return Map.of("count", list.size(), "players", list);
                })));

        r.register(new Simple("tablist_header_footer",
                "Return the server's tab list header/footer text (scoreboard/announcements live here on many servers).",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    PlayerListHud hud = Mc.client().inGameHud.getPlayerListHud();
                    return Map.of(
                            "header", GuiCapture.tablistHeader(),
                            "footer", GuiCapture.tablistFooter());
                })));

        r.register(new Simple("scoreboard_read",
                "Read the sidebar scoreboard (server scoreboards, DonutSMP balance readouts, team info).",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    var sb = Mc.world().getScoreboard();
                    var sidebar = sb.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);
                    if (sidebar == null) return Map.of("open", false);
                    List<Map<String,Object>> lines = new ArrayList<>();
                    for (var score : sb.getScoreboardEntries(sidebar)) {
                        lines.add(Map.of("name", score.owner(), "value", score.value()));
                    }
                    return Map.of(
                            "open", true,
                            "title", sidebar.getDisplayName().getString(),
                            "lines", lines);
                })));

        r.register(new Simple("bossbars_read",
                "Return active boss bar titles and progress.",
                Map.of(),
                args -> Map.of("bossbars", GuiCapture.bossbars())));

        r.register(new Simple("screen_info",
                "Report the currently open screen class name + title (tells you which GUI is up: chest, anvil, custom shop screen).",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    var s = Mc.client().currentScreen;
                    if (s == null) return Map.of("open", false);
                    return Map.of(
                            "open", true,
                            "class", s.getClass().getName(),
                            "title", s.getTitle().getString());
                })));

        r.register(new Simple("overlay_set",
                "Write a short line of text to the MCP's in-game overlay (shows above the hotbar). Pass empty to clear.",
                Map.of("text", Tool.prop("string")),
                args -> {
                    GuiCapture.setOverlay(Simple.str(args, "text", ""));
                    return Map.of("ok", true);
                }));

        r.register(new Simple("close_screen",
                "Close whatever screen is open (including inventory).",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    if (Mc.client().currentScreen != null) Mc.player().closeHandledScreen();
                    return Map.of("ok", true);
                })));

        r.register(new Simple("open_inventory",
                "Open the player inventory screen.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    Mc.client().setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(Mc.player()));
                    return Map.of("ok", true);
                })));
    }
}
