package com.mcpmc.hooks;

import com.mcpmc.anticheat.FingerprintAnalyzer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class PacketCapture {
    private PacketCapture() {}

    public static void install(FingerprintAnalyzer ac) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String brand = handler.getBrand();
            if (brand != null) ac.onPluginChannel("brand:" + brand);
        });
    }
}
