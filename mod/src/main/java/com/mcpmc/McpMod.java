package com.mcpmc;

import com.mcpmc.anticheat.FingerprintAnalyzer;
import com.mcpmc.hooks.ChatCapture;
import com.mcpmc.hooks.GuiCapture;
import com.mcpmc.hooks.PacketCapture;
import com.mcpmc.server.McpHttpServer;
import com.mcpmc.server.StickyPort;
import com.mcpmc.strategy.StrategyRegistry;
import com.mcpmc.terminal.TerminalWindow;
import com.mcpmc.tools.ToolRegistry;
import com.mcpmc.util.Log;
import net.fabricmc.api.ClientModInitializer;

public final class McpMod implements ClientModInitializer {
    public static final String MOD_ID = "mcpmc";
    public static final String GITHUB = "https://github.com/AABABABABABABBABAABABABABABBA/BuildThisForMe";

    private static McpHttpServer server;
    private static ToolRegistry tools;
    private static StrategyRegistry strategies;
    private static FingerprintAnalyzer anticheat;
    private static TerminalWindow terminal;

    @Override
    public void onInitializeClient() {
        anticheat = new FingerprintAnalyzer();
        tools = new ToolRegistry();
        strategies = new StrategyRegistry(tools);
        tools.registerCore(anticheat, strategies);
        strategies.registerDefaults();

        terminal = TerminalWindow.open();
        terminal.status("Starting");
        terminal.info("Build " + MOD_ID + " v1.0.0");

        ChatCapture.install(anticheat);
        GuiCapture.install();
        PacketCapture.install(anticheat);

        try {
            int port = StickyPort.pick("mcp");
            server = new McpHttpServer(port, tools);
            server.start();
            terminal.status("Connected");
            terminal.info("MCP endpoint:    http://localhost:" + port);
            terminal.info("Tools:           " + tools.size());
            terminal.info("Strategies:      " + strategies.size());
            terminal.info("GitHub:          " + GITHUB);
            terminal.info("Config dir:      " + StickyPort.configDir());
            Log.info("MCP server up on :" + port);
        } catch (java.net.BindException e) {
            terminal.status("Network error");
            terminal.error("Port bind failed: " + e.getMessage());
            Log.error("bind failed", e);
        } catch (Exception e) {
            terminal.status("Failed");
            terminal.error(e.getClass().getSimpleName() + ": " + e.getMessage());
            Log.error("startup failed", e);
        }
    }

    public static ToolRegistry tools() { return tools; }
    public static StrategyRegistry strategies() { return strategies; }
    public static FingerprintAnalyzer anticheat() { return anticheat; }
    public static TerminalWindow terminal() { return terminal; }
}
