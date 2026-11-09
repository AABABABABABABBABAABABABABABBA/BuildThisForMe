package com.mcpmc.tools;

import com.mcpmc.anticheat.CadenceLimiter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

final class MovementTools {
    private final ToolRegistry r;
    MovementTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("move_to",
                "Walk toward x,y,z using the real physics engine (vanilla speed caps apply). Pass sprint=true to sprint. Pass stop_distance (default 0.5).",
                Map.of(
                        "x", Tool.prop("number"), "y", Tool.prop("number"), "z", Tool.prop("number"),
                        "sprint", Tool.prop("boolean"),
                        "stop_distance", Tool.prop("number")),
                args -> {
                    Vec3d target = new Vec3d(Simple.num(args, "x"), Simple.num(args, "y"), Simple.num(args, "z"));
                    boolean sprint = Simple.bool(args, "sprint", false);
                    double stopAt = Simple.num(args, "stop_distance", 0.5);
                    long deadline = System.currentTimeMillis() + 15_000;
                    int ticks = 0;
                    while (System.currentTimeMillis() < deadline) {
                        var done = Mc.onClientThread(() -> {
                            var p = Mc.player();
                            Vec3d cur = p.getPos();
                            double dx = target.x - cur.x, dz = target.z - cur.z;
                            double d2 = dx*dx + dz*dz;
                            if (d2 < stopAt*stopAt) {
                                releaseAll();
                                return Boolean.TRUE;
                            }
                            float yaw = (float)(MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
                            p.setYaw(CadenceLimiter.smoothYaw(p.getYaw(), yaw));
                            MinecraftClient c = Mc.client();
                            c.options.forwardKey.setPressed(true);
                            c.options.sprintKey.setPressed(sprint);
                            return Boolean.FALSE;
                        });
                        if (done) break;
                        ticks++;
                        Thread.sleep(50);
                    }
                    Mc.onClientThread(() -> { releaseAll(); return null; });
                    return Map.of("ticks", ticks, "final", posMap());
                }));

        r.register(new Simple("look_at",
                "Face a world coord. Uses a smoothed rotation to avoid rotation-speed anticheat checks.",
                Map.of("x", Tool.prop("number"), "y", Tool.prop("number"), "z", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    var p = Mc.player();
                    double dx = Simple.num(args, "x") - p.getX();
                    double dy = Simple.num(args, "y") - (p.getY() + p.getStandingEyeHeight());
                    double dz = Simple.num(args, "z") - p.getZ();
                    double horiz = Math.sqrt(dx*dx + dz*dz);
                    float yaw = (float)(MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
                    float pitch = (float)(-MathHelper.atan2(dy, horiz) * 180.0 / Math.PI);
                    p.setYaw(CadenceLimiter.smoothYaw(p.getYaw(), yaw));
                    p.setPitch(CadenceLimiter.smoothPitch(p.getPitch(), pitch));
                    return Map.of("yaw", p.getYaw(), "pitch", p.getPitch());
                })));

        r.register(new Simple("jump",
                "Make the player jump (sends the jump keybind for one tick).",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    Mc.client().options.jumpKey.setPressed(true);
                    Mc.client().execute(() -> Mc.client().options.jumpKey.setPressed(false));
                    return Map.of("ok", true);
                })));

        r.register(new Simple("sneak",
                "Hold/release sneak. Pass hold=true to press, false to release.",
                Map.of("hold", Tool.prop("boolean")),
                args -> Mc.onClientThread(() -> {
                    Mc.client().options.sneakKey.setPressed(Simple.bool(args, "hold", true));
                    return Map.of("sneak", Simple.bool(args, "hold", true));
                })));

        r.register(new Simple("sprint",
                "Hold/release sprint.",
                Map.of("hold", Tool.prop("boolean")),
                args -> Mc.onClientThread(() -> {
                    Mc.client().options.sprintKey.setPressed(Simple.bool(args, "hold", true));
                    return Map.of("sprint", Simple.bool(args, "hold", true));
                })));

        r.register(new Simple("stop",
                "Release every movement key.",
                Map.of(),
                args -> Mc.onClientThread(() -> { releaseAll(); return Map.of("ok", true); })));

        r.register(new Simple("walk_direction",
                "Walk in a direction (forward|back|left|right) for N milliseconds, sprinting if requested.",
                Map.of("direction", Tool.prop("string"), "duration_ms", Tool.prop("number"), "sprint", Tool.prop("boolean")),
                args -> {
                    String dir = Simple.str(args, "direction");
                    long ms = (long) Simple.num(args, "duration_ms", 500);
                    boolean sprint = Simple.bool(args, "sprint", false);
                    Mc.onClientThread(() -> { press(dir, true); Mc.client().options.sprintKey.setPressed(sprint); return null; });
                    Thread.sleep(ms);
                    Mc.onClientThread(() -> { press(dir, false); Mc.client().options.sprintKey.setPressed(false); return null; });
                    return Map.of("direction", dir, "duration_ms", ms);
                }));
    }

    private static void press(String dir, boolean on) {
        var o = Mc.client().options;
        KeyBinding k = switch (dir) {
            case "forward" -> o.forwardKey;
            case "back" -> o.backKey;
            case "left" -> o.leftKey;
            case "right" -> o.rightKey;
            default -> throw new IllegalArgumentException("direction must be forward|back|left|right");
        };
        k.setPressed(on);
    }

    private static void releaseAll() {
        var o = Mc.client().options;
        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.sprintKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sneakKey.setPressed(false);
    }

    private static Map<String,Object> posMap() {
        var p = Mc.player();
        return Map.of("x", p.getX(), "y", p.getY(), "z", p.getZ(), "yaw", p.getYaw(), "pitch", p.getPitch());
    }
}
