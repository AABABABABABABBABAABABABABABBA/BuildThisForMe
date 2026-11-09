package com.mcpmc.tools;

import com.mcpmc.hooks.GuiCapture;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

final class ContainerTools {
    private final ToolRegistry r;
    ContainerTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("container_read",
                "If a container screen is open (chest, shulker, shop GUI, hopper, trading), return every slot: item id, count, display name, lore. This is how you read shop prices.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    HandledScreen<?> s = openedHandled();
                    if (s == null) return Map.of("open", false);
                    ScreenHandler h = s.getScreenHandler();
                    List<Map<String,Object>> slots = new ArrayList<>();
                    for (int i = 0; i < h.slots.size(); i++) {
                        ItemStack st = h.slots.get(i).getStack();
                        Map<String,Object> m = new LinkedHashMap<>();
                        m.put("slot", i);
                        if (st.isEmpty()) { m.put("empty", true); slots.add(m); continue; }
                        m.put("item", Registries.ITEM.getId(st.getItem()).toString());
                        m.put("count", st.getCount());
                        m.put("display_name", st.getName().getString());
                        var lore = GuiCapture.loreText(st);
                        if (!lore.isEmpty()) m.put("lore", lore);
                        slots.add(m);
                    }
                    return Map.of(
                            "open", true,
                            "screen", s.getClass().getSimpleName(),
                            "title", s.getTitle().getString(),
                            "sync_id", h.syncId,
                            "slot_count", h.slots.size(),
                            "slots", slots);
                })));

        r.register(new Simple("container_click",
                "Click a slot in the currently-open container. action=pickup|quick_move|clone|throw|pickup_all|swap. button=0 (left) or 1 (right).",
                Map.of("slot", Tool.prop("number"), "button", Tool.prop("number"), "action", Tool.prop("string")),
                args -> Mc.onClientThread(() -> {
                    HandledScreen<?> s = openedHandled();
                    if (s == null) throw new IllegalStateException("no container open");
                    int slot = Simple.intArg(args, "slot");
                    int button = Simple.intArg(args, "button", 0);
                    SlotActionType action = switch (Simple.str(args, "action", "pickup")) {
                        case "pickup" -> SlotActionType.PICKUP;
                        case "quick_move" -> SlotActionType.QUICK_MOVE;
                        case "clone" -> SlotActionType.CLONE;
                        case "throw" -> SlotActionType.THROW;
                        case "pickup_all" -> SlotActionType.PICKUP_ALL;
                        case "swap" -> SlotActionType.SWAP;
                        default -> throw new IllegalArgumentException("bad action");
                    };
                    Mc.client().interactionManager.clickSlot(s.getScreenHandler().syncId, slot, button, action, Mc.player());
                    GuiCapture.recordClick(slot, button, action.name());
                    return Map.of("clicked", slot, "action", action.name());
                })));

        r.register(new Simple("container_close",
                "Close the currently open container screen.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    Mc.player().closeHandledScreen();
                    return Map.of("ok", true);
                })));

        r.register(new Simple("container_record",
                "Start recording slot clicks on the current container. Later replay with container_replay.",
                Map.of("name", Tool.prop("string")),
                args -> { GuiCapture.startRecording(Simple.str(args, "name")); return Map.of("recording", Simple.str(args, "name")); }));

        r.register(new Simple("container_stop_recording",
                "Stop recording and return the captured click sequence.",
                Map.of(),
                args -> Map.of("recording", GuiCapture.stopRecording())));

        r.register(new Simple("container_replay",
                "Replay a previously recorded click sequence on the current container.",
                Map.of("name", Tool.prop("string"), "delay_ms", Tool.prop("number")),
                args -> {
                    String name = Simple.str(args, "name");
                    long delay = (long) Simple.num(args, "delay_ms", 150);
                    var seq = GuiCapture.getRecording(name);
                    if (seq == null) throw new IllegalArgumentException("no recording named " + name);
                    int done = 0;
                    for (var c : seq) {
                        Mc.onClientThread(() -> {
                            HandledScreen<?> s = openedHandled();
                            if (s == null) return null;
                            Mc.client().interactionManager.clickSlot(
                                    s.getScreenHandler().syncId, c.slot, c.button,
                                    SlotActionType.valueOf(c.action), Mc.player());
                            return null;
                        });
                        done++;
                        Thread.sleep(delay);
                    }
                    return Map.of("replayed", done);
                }));

        r.register(new Simple("container_recordings",
                "List all saved recordings with step counts.",
                Map.of(),
                args -> Map.of("recordings", GuiCapture.listRecordings())));
    }

    private static HandledScreen<?> openedHandled() {
        var scr = Mc.client().currentScreen;
        return scr instanceof HandledScreen<?> h ? h : null;
    }
}
