package com.mcpmc.tools;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import java.util.Map;

final class ServerTools {
    private final ToolRegistry r;
    ServerTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("server_join",
                "Connect to a Minecraft server by address (host:port). Disconnects from the current one if connected. The game must be on the title screen or already in a server.",
                Map.of("address", Tool.prop("string")),
                args -> Mc.onClientThread(() -> {
                    String addr = Simple.str(args, "address");
                    MinecraftClient c = Mc.client();
                    ServerInfo si = new ServerInfo("mcp", addr, ServerInfo.ServerType.OTHER);
                    ServerAddress sa = ServerAddress.parse(addr);
                    if (c.world != null) c.world.disconnect();
                    c.setScreen(new TitleScreen());
                    ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), c, sa, si, false, null);
                    return Map.of("joining", addr);
                })));

        r.register(new Simple("server_disconnect",
                "Disconnect from the current server and return to the title screen.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    MinecraftClient c = Mc.client();
                    if (c.world != null) c.world.disconnect();
                    c.disconnect();
                    c.setScreen(new TitleScreen());
                    return Map.of("ok", true);
                })));

        r.register(new Simple("server_info",
                "Report the current server address, latency, motd, player count.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    MinecraftClient c = Mc.client();
                    var si = c.getCurrentServerEntry();
                    if (si == null) return Map.of("connected", false);
                    return Map.of(
                            "connected", true,
                            "address", si.address,
                            "name", si.name,
                            "latency", si.ping,
                            "players_online", si.playerCountLabel != null ? si.playerCountLabel.getString() : null,
                            "motd", si.label != null ? si.label.getString() : null);
                })));

        r.register(new Simple("servers_saved",
                "List saved servers from the multiplayer list.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    var list = Mc.client().getServerList();
                    list.loadFile();
                    java.util.List<Map<String,Object>> out = new java.util.ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        var e = list.get(i);
                        out.add(Map.of("index", i, "name", e.name, "address", e.address));
                    }
                    return Map.of("servers", out);
                })));
    }
}
