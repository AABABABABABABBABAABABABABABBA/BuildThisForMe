package com.mcpmc.anticheat;

import com.mcpmc.hooks.ChatCapture;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionProber {
    private VersionProber() {}

    private static final Pattern VERSION_TOKEN = Pattern.compile("v?(\\d+\\.\\d+(?:\\.\\d+)?(?:[-+][a-z0-9.]+)?)", Pattern.CASE_INSENSITIVE);

    public static Map<String, String> scanChatForVersions(List<String> antiCheatNames) {
        Map<String, String> out = new LinkedHashMap<>();
        for (var msg : ChatCapture.recent(500)) {
            String text = String.valueOf(msg.get("text"));
            String lower = text.toLowerCase();
            for (String ac : antiCheatNames) {
                String needle = ac.toLowerCase();
                int idx = lower.indexOf(needle);
                if (idx < 0) continue;
                String window = text.substring(idx, Math.min(text.length(), idx + needle.length() + 40));
                Matcher m = VERSION_TOKEN.matcher(window);
                if (m.find()) out.putIfAbsent(ac, m.group(1));
            }
        }
        return out;
    }

    public static List<String> probeCommands(String detectedAc) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("/plugins");
        suggestions.add("/pl");
        if (detectedAc != null) {
            String low = detectedAc.toLowerCase();
            if (low.equals("grim"))    suggestions.add("/grim version");
            if (low.equals("vulcan"))  suggestions.add("/vulcan version");
            if (low.equals("matrix"))  suggestions.add("/matrix version");
            if (low.equals("ncp"))     suggestions.add("/ncp version");
            if (low.equals("aac"))     suggestions.add("/aac version");
            if (low.equals("spartan")) suggestions.add("/spartan version");
            if (low.equals("themis"))  suggestions.add("/themis version");
            if (low.equals("polar"))   suggestions.add("/polar version");
        }
        return suggestions;
    }
}
