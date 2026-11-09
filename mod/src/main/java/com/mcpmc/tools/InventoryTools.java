package com.mcpmc.tools;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.*;

final class InventoryTools {
    private final ToolRegistry r;
    InventoryTools(ToolRegistry r) { this.r = r; }

    void register() {
        r.register(new Simple("inventory_list",
                "List all 36 inventory slots + offhand with id and count.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    var inv = Mc.player().getInventory();
                    List<Map<String,Object>> items = new ArrayList<>();
                    for (int i = 0; i < inv.size(); i++) {
                        ItemStack s = inv.getStack(i);
                        if (s.isEmpty()) continue;
                        items.add(Map.of(
                                "slot", i,
                                "item", Registries.ITEM.getId(s.getItem()).toString(),
                                "count", s.getCount(),
                                "custom_name", s.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
                                        ? s.getName().getString() : null));
                    }
                    return Map.of("selected", inv.selectedSlot, "items", items);
                })));

        r.register(new Simple("hold_slot",
                "Switch the hotbar to slot 0-8.",
                Map.of("slot", Tool.prop("number")),
                args -> Mc.onClientThread(() -> {
                    int s = Simple.intArg(args, "slot");
                    if (s < 0 || s > 8) throw new IllegalArgumentException("slot 0-8");
                    Mc.player().getInventory().selectedSlot = s;
                    return Map.of("held", s);
                })));

        r.register(new Simple("drop_held",
                "Drop the currently held item. Pass all=true to drop the whole stack.",
                Map.of("all", Tool.prop("boolean")),
                args -> Mc.onClientThread(() -> {
                    boolean all = Simple.bool(args, "all", false);
                    Mc.player().dropSelectedItem(all);
                    return Map.of("ok", true);
                })));

        r.register(new Simple("drop_slot",
                "Drop the item in a specific inventory slot. Pass all=true to drop entire stack.",
                Map.of("slot", Tool.prop("number"), "all", Tool.prop("boolean")),
                args -> Mc.onClientThread(() -> {
                    int slot = Simple.intArg(args, "slot");
                    boolean all = Simple.bool(args, "all", false);
                    Mc.client().interactionManager.clickSlot(
                            Mc.player().playerScreenHandler.syncId,
                            slot, all ? 1 : 0, SlotActionType.THROW, Mc.player());
                    return Map.of("dropped_slot", slot, "all", all);
                })));

        r.register(new Simple("swap_to_offhand",
                "Swap held item with offhand.",
                Map.of(),
                args -> Mc.onClientThread(() -> {
                    Mc.net().sendPlayerAction(
                            net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                            net.minecraft.util.math.BlockPos.ORIGIN, net.minecraft.util.math.Direction.DOWN);
                    return Map.of("ok", true);
                })));

        r.register(new Simple("use_item",
                "Right-click with the held item (eat, drink, place, shoot). Pass hand=offhand to use offhand.",
                Map.of("hand", Tool.prop("string")),
                args -> Mc.onClientThread(() -> {
                    Hand h = "offhand".equalsIgnoreCase(Simple.str(args, "hand", "main")) ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    Mc.client().interactionManager.interactItem(Mc.player(), h);
                    return Map.of("used", h.name());
                })));

        r.register(new Simple("find_item",
                "Find an inventory slot whose item id contains `match`. Returns the first slot or -1.",
                Map.of("match", Tool.prop("string")),
                args -> Mc.onClientThread(() -> {
                    String n = Simple.str(args, "match").toLowerCase();
                    var inv = Mc.player().getInventory();
                    for (int i = 0; i < inv.size(); i++) {
                        ItemStack s = inv.getStack(i);
                        if (s.isEmpty()) continue;
                        if (Registries.ITEM.getId(s.getItem()).toString().toLowerCase().contains(n))
                            return Map.of("slot", i, "count", s.getCount());
                    }
                    return Map.of("slot", -1);
                })));
    }
}
