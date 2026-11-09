package com.mcpmc.tools;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;

final class WorldTools {
    private final ToolRegistry r;
    WorldTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("block_at",
                "Get the block at integer x,y,z: id (e.g. minecraft:diamond_ore), hardness, light.",
                Map.of("x", Tool.prop("number"), "y", Tool.prop("number"), "z", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    BlockPos pos = new BlockPos(Simple.intArg(args, "x"), Simple.intArg(args, "y"), Simple.intArg(args, "z"));
                    BlockState bs = Mc.world().getBlockState(pos);
                    return Map.of(
                            "position", Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ()),
                            "block", Registries.BLOCK.getId(bs.getBlock()).toString(),
                            "solid", bs.isSolid(),
                            "opaque", bs.isOpaque(),
                            "light_level", Mc.world().getLightLevel(pos));
                })));

        r.register(new Simple("find_blocks",
                "Scan a cube around the player for blocks whose id contains `match`. Returns positions sorted by distance. radius default 32, max_results default 64.",
                Map.of("match", Tool.prop("string"), "radius", Tool.prop("number"), "max_results", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    String needle = Simple.str(args, "match").toLowerCase();
                    int rad = Math.min(64, Simple.intArg(args, "radius", 32));
                    int max = Math.min(512, Simple.intArg(args, "max_results", 64));
                    var p = Mc.player();
                    BlockPos origin = p.getBlockPos();
                    List<Map<String,Object>> hits = new ArrayList<>();
                    for (int dx = -rad; dx <= rad; dx++)
                        for (int dz = -rad; dz <= rad; dz++)
                            for (int dy = -rad; dy <= rad; dy++) {
                                BlockPos bp = origin.add(dx, dy, dz);
                                BlockState bs = Mc.world().getBlockState(bp);
                                String id = Registries.BLOCK.getId(bs.getBlock()).toString();
                                if (id.contains(needle)) {
                                    double d = bp.getSquaredDistance(p.getPos());
                                    hits.add(Map.of("x", bp.getX(), "y", bp.getY(), "z", bp.getZ(),
                                            "block", id, "distance", Math.sqrt(d)));
                                }
                            }
                    hits.sort(Comparator.comparingDouble(m -> (double) m.get("distance")));
                    return Map.of("found", hits.size(), "blocks", hits.subList(0, Math.min(max, hits.size())));
                })));

        r.register(new Simple("raycast",
                "Cast a ray from the player's eyes in their facing direction, up to max_distance blocks. Returns what was hit (block or entity).",
                Map.of("max_distance", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    double max = Simple.num(args, "max_distance", 20);
                    var p = Mc.player();
                    Vec3d eye = p.getCameraPosVec(1f);
                    Vec3d rot = p.getRotationVec(1f);
                    Vec3d end = eye.add(rot.multiply(max));
                    HitResult br = Mc.world().raycast(new RaycastContext(eye, end,
                            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
                    EntityHitResult er = ProjectileUtil.raycast(p, eye, end, new Box(eye, end).expand(1.0),
                            e -> !e.isSpectator() && e.canHit(), max * max);
                    if (er != null && (br == null || eye.squaredDistanceTo(er.getPos()) < eye.squaredDistanceTo(br.getPos()))) {
                        Entity e = er.getEntity();
                        return Map.of("kind", "entity",
                                "id", e.getId(),
                                "type", Registries.ENTITY_TYPE.getId(e.getType()).toString(),
                                "position", Map.of("x", e.getX(), "y", e.getY(), "z", e.getZ()));
                    }
                    if (br instanceof BlockHitResult bhr && bhr.getType() != HitResult.Type.MISS) {
                        BlockPos bp = bhr.getBlockPos();
                        return Map.of("kind", "block",
                                "position", Map.of("x", bp.getX(), "y", bp.getY(), "z", bp.getZ()),
                                "block", Registries.BLOCK.getId(Mc.world().getBlockState(bp).getBlock()).toString(),
                                "face", bhr.getSide().getName());
                    }
                    return Map.of("kind", "miss");
                })));

        r.register(new Simple("dimension_info",
                "Return dimension id, biome at player, time of day, weather, difficulty, spawn point.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    var w = Mc.world();
                    var p = Mc.player();
                    var biome = w.getBiome(p.getBlockPos()).getKey().map(k -> k.getValue().toString()).orElse("unknown");
                    return Map.of(
                            "dimension", w.getRegistryKey().getValue().toString(),
                            "biome", biome,
                            "time", w.getTimeOfDay(),
                            "raining", w.isRaining(),
                            "thundering", w.isThundering(),
                            "difficulty", w.getDifficulty().getName(),
                            "spawn", Map.of("x", w.getSpawnPos().getX(), "y", w.getSpawnPos().getY(), "z", w.getSpawnPos().getZ()));
                })));

        r.register(new Simple("chunk_info",
                "Report chunk load status near the player: how many chunks loaded, render distance.",
                Map.of(),
                args -> Mc.onClientThread(() -> Map.of(
                        "render_distance", Mc.client().options.getViewDistance().getValue(),
                        "loaded_chunks", Mc.world().getChunkManager().getLoadedChunkCount(),
                        "simulation_distance", Mc.client().options.getSimulationDistance().getValue()))));
    }
}
