package com.mcpmc.tools;

import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class RegistryTools {
    private final ToolRegistry r;
    RegistryTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("registry_list",
                "List registry entries. kind = items|blocks|entities|enchantments|effects|biomes. Supports match substring and limit.",
                Map.of("kind", Tool.prop("string"), "match", Tool.prop("string"), "limit", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    String kind = Simple.str(args, "kind");
                    String match = Simple.str(args, "match", "").toLowerCase();
                    int limit = Math.min(2000, Simple.intArg(args, "limit", 200));
                    Iterable<? extends Identifier> ids = switch (kind) {
                        case "items" -> Registries.ITEM.getIds();
                        case "blocks" -> Registries.BLOCK.getIds();
                        case "entities" -> Registries.ENTITY_TYPE.getIds();
                        case "enchantments" -> List.of();
                        case "effects" -> Registries.STATUS_EFFECT.getIds();
                        case "biomes" -> Registries.BIOME_SOURCE.getIds();
                        default -> throw new IllegalArgumentException("kind must be items|blocks|entities|enchantments|effects|biomes");
                    };
                    List<String> out = new ArrayList<>();
                    for (Identifier id : ids) {
                        String s = id.toString();
                        if (match.isEmpty() || s.toLowerCase().contains(match)) out.add(s);
                        if (out.size() >= limit) break;
                    }
                    return Map.of("count", out.size(), "entries", out);
                })));

        r.register(new Simple("resource_read",
                "Read a Minecraft asset resource (lang file, model json, texture path). Pass `path` like 'minecraft:lang/en_us.json'. Returns UTF-8 text (first 32KB).",
                Map.of("path", Tool.prop("string")),
                args -> Mc.onClientThread(() -> {
                    String raw = Simple.str(args, "path");
                    Identifier id = Identifier.tryParse(raw);
                    if (id == null) throw new IllegalArgumentException("bad identifier");
                    ResourceManager rm = Mc.client().getResourceManager();
                    Optional<net.minecraft.resource.Resource> res = rm.getResource(id);
                    if (res.isEmpty()) return Map.of("found", false);
                    try (InputStream is = res.get().getInputStream()) {
                        byte[] bytes = is.readAllBytes();
                        boolean truncated = bytes.length > 32 * 1024;
                        int len = Math.min(bytes.length, 32 * 1024);
                        return Map.of(
                                "found", true,
                                "size", bytes.length,
                                "truncated", truncated,
                                "text", new String(bytes, 0, len, StandardCharsets.UTF_8));
                    }
                })));

        r.register(new Simple("class_dump",
                "Dump the raw bytes of a loaded JVM class (base64). Pass fully-qualified name like 'net.minecraft.client.MinecraftClient'. Lets you feed bytes to CFR/Vineflower/JADX for decompilation offline.",
                Map.of("class", Tool.prop("string")),
                args -> {
                    String name = Simple.str(args, "class").replace('.', '/') + ".class";
                    ClassLoader cl = Mc.class.getClassLoader();
                    try (InputStream is = cl.getResourceAsStream(name)) {
                        if (is == null) return Map.of("found", false);
                        byte[] b = is.readAllBytes();
                        return Map.of(
                                "found", true,
                                "size", b.length,
                                "base64", Base64.getEncoder().encodeToString(b));
                    }
                }));

        r.register(new Simple("mods_list",
                "List all Fabric mods loaded (for figuring out what's installed on your client, and where the mcpmc mod sits).",
                Map.of(),
                args -> {
                    List<Map<String,Object>> out = new ArrayList<>();
                    for (var m : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods()) {
                        out.add(Map.of(
                                "id", m.getMetadata().getId(),
                                "name", m.getMetadata().getName(),
                                "version", m.getMetadata().getVersion().getFriendlyString()));
                    }
                    return Map.of("mods", out);
                }));
    }
}
