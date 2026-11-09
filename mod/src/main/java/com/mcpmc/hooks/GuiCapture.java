package com.mcpmc.hooks;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiCapture {
    private GuiCapture() {}

    public static final class ClickStep {
        public final int slot, button;
        public final String action;
        public ClickStep(int s, int b, String a) { slot = s; button = b; action = a; }
        public Map<String,Object> asMap() { return Map.of("slot", slot, "button", button, "action", action); }
    }

    private static final Map<String, List<ClickStep>> recordings = new ConcurrentHashMap<>();
    private static volatile String activeRecording;
    private static volatile String overlayText = "";
    private static volatile String tablistHeader = "";
    private static volatile String tablistFooter = "";
    private static final Map<UUID, Map<String,Object>> bossbars = new ConcurrentHashMap<>();

    public static void install() { /* registered lazily via mixins touching these fields */ }

    public static List<String> loreText(ItemStack s) {
        LoreComponent lc = s.get(DataComponentTypes.LORE);
        if (lc == null) return List.of();
        List<String> out = new ArrayList<>();
        for (Text t : lc.lines()) out.add(t.getString());
        return out;
    }

    public static void startRecording(String name) {
        activeRecording = name;
        recordings.put(name, Collections.synchronizedList(new ArrayList<>()));
    }

    public static Map<String,Object> stopRecording() {
        String n = activeRecording;
        activeRecording = null;
        if (n == null) return Map.of();
        List<Map<String,Object>> steps = new ArrayList<>();
        for (ClickStep s : recordings.getOrDefault(n, List.of())) steps.add(s.asMap());
        return Map.of("name", n, "steps", steps);
    }

    public static void recordClick(int slot, int button, String action) {
        String n = activeRecording;
        if (n == null) return;
        var list = recordings.get(n);
        if (list != null) list.add(new ClickStep(slot, button, action));
    }

    public static List<ClickStep> getRecording(String name) { return recordings.get(name); }

    public static List<Map<String,Object>> listRecordings() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var e : recordings.entrySet()) out.add(Map.of("name", e.getKey(), "steps", e.getValue().size()));
        return out;
    }

    public static void setOverlay(String text) { overlayText = text == null ? "" : text; }
    public static String overlayText() { return overlayText; }

    public static void setTablistHeader(String s) { tablistHeader = s; }
    public static void setTablistFooter(String s) { tablistFooter = s; }
    public static String tablistHeader() { return tablistHeader; }
    public static String tablistFooter() { return tablistFooter; }

    public static void bossbarUpdate(UUID id, String name, float percent) {
        bossbars.put(id, Map.of("id", id.toString(), "name", name, "percent", percent));
    }
    public static void bossbarRemove(UUID id) { bossbars.remove(id); }
    public static List<Map<String,Object>> bossbars() { return new ArrayList<>(bossbars.values()); }
}
