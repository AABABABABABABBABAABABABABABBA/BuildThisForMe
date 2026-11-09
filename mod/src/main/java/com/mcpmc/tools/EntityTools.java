package com.mcpmc.tools;

import com.mcpmc.anticheat.CadenceLimiter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;

import java.util.*;

final class EntityTools {
    private final ToolRegistry r;
    EntityTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("entities_nearby",
                "List entities within `radius` blocks. Each has id, type, position, distance, health (if living), and name.",
                Map.of("radius", Tool.prop("number"), "filter", Tool.prop("string")),
                args -> Mc.onClientThread(() -> {
                    double rad = Simple.num(args, "radius", 32);
                    String filter = Simple.str(args, "filter", "").toLowerCase();
                    var p = Mc.player();
                    List<Map<String,Object>> out = new ArrayList<>();
                    for (Entity e : Mc.world().getEntities()) {
                        if (e == p) continue;
                        double d = e.squaredDistanceTo(p);
                        if (d > rad * rad) continue;
                        String type = Registries.ENTITY_TYPE.getId(e.getType()).toString();
                        if (!filter.isEmpty() && !type.contains(filter)) continue;
                        Map<String,Object> m = new LinkedHashMap<>();
                        m.put("id", e.getId());
                        m.put("type", type);
                        m.put("name", e.getName().getString());
                        m.put("position", Map.of("x", e.getX(), "y", e.getY(), "z", e.getZ()));
                        m.put("distance", Math.sqrt(d));
                        if (e instanceof LivingEntity le) m.put("health", le.getHealth());
                        if (e instanceof PlayerEntity) m.put("player", true);
                        out.add(m);
                    }
                    out.sort(Comparator.comparingDouble(m -> (double) m.get("distance")));
                    return Map.of("entities", out);
                })));

        r.register(new Simple("attack_entity",
                "Swing+attack an entity by id. Pair with look_at first if you need to be facing it.",
                Map.of("entity_id", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    int id = Simple.intArg(args, "entity_id");
                    Entity e = Mc.world().getEntityById(id);
                    if (e == null) throw new IllegalArgumentException("no entity " + id);
                    CadenceLimiter.gateAttack();
                    Mc.player().swingHand(Hand.MAIN_HAND);
                    Mc.client().interactionManager.attackEntity(Mc.player(), e);
                    return Map.of("attacked", id);
                })));

        r.register(new Simple("interact_entity",
                "Right-click an entity (trade, mount, name tag, etc.).",
                Map.of("entity_id", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    int id = Simple.intArg(args, "entity_id");
                    Entity e = Mc.world().getEntityById(id);
                    if (e == null) throw new IllegalArgumentException("no entity " + id);
                    Mc.client().interactionManager.interactEntity(Mc.player(), e, Hand.MAIN_HAND);
                    return Map.of("interacted", id);
                })));
    }
}
