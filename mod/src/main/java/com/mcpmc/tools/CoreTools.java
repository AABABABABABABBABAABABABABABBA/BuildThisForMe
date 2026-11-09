package com.mcpmc.tools;

import net.minecraft.client.MinecraftClient;

import java.util.LinkedHashMap;
import java.util.Map;

final class CoreTools {
    private final ToolRegistry r;
    CoreTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("status",
                "Full snapshot: player pos/health/food/gamemode, server address, dimension, fps, tick lag, held item, whether we're on a screen (inventory/chest/etc).",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    MinecraftClient c = Mc.client();
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("connected", Mc.connected());
                    m.put("fps", c.getCurrentFps());
                    m.put("server", c.getCurrentServerEntry() != null ? c.getCurrentServerEntry().address : null);
                    m.put("world_loaded", c.world != null);
                    if (c.player != null) {
                        var p = c.player;
                        m.put("username", p.getGameProfile().getName());
                        m.put("uuid", p.getUuid().toString());
                        m.put("position", Map.of("x", p.getX(), "y", p.getY(), "z", p.getZ()));
                        m.put("yaw", p.getYaw());
                        m.put("pitch", p.getPitch());
                        m.put("velocity", Map.of("x", p.getVelocity().x, "y", p.getVelocity().y, "z", p.getVelocity().z));
                        m.put("health", p.getHealth());
                        m.put("food", p.getHungerManager().getFoodLevel());
                        m.put("saturation", p.getHungerManager().getSaturationLevel());
                        m.put("xp_level", p.experienceLevel);
                        m.put("xp_progress", p.experienceProgress);
                        m.put("gamemode", c.interactionManager != null ? c.interactionManager.getCurrentGameMode().getName() : null);
                        m.put("dimension", p.getWorld().getRegistryKey().getValue().toString());
                        m.put("on_ground", p.isOnGround());
                        m.put("sprinting", p.isSprinting());
                        m.put("sneaking", p.isSneaking());
                        m.put("held_slot", p.getInventory().selectedSlot);
                    }
                    m.put("screen_open", c.currentScreen != null ? c.currentScreen.getClass().getSimpleName() : null);
                    return m;
                })));

        r.register(new Simple("version",
                "Minecraft version and mod version info.",
                Map.of(),
                args -> Map.of(
                        "minecraft", Mc.client().getGameVersion(),
                        "mod", "1.0.0",
                        "java", System.getProperty("java.version"))));
    }
}
