package com.mcpmc.server;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StickyPort {
    private static final int MIN = 25600;
    private static final int MAX = 25699;

    private StickyPort() {}

    public static Path configDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("mcpmc");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir;
    }

    public static int pick(String role) throws BindException {
        Path file = configDir().resolve("port-" + role);
        Integer saved = read(file);
        if (saved != null && isFree(saved)) {
            write(file, saved);
            return saved;
        }
        for (int p = MIN; p <= MAX; p++) {
            if (isFree(p)) {
                write(file, p);
                return p;
            }
        }
        throw new BindException("no free port in " + MIN + "-" + MAX);
    }

    private static boolean isFree(int port) {
        try (ServerSocket s = new ServerSocket(port)) {
            s.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Integer read(Path file) {
        if (!Files.exists(file)) return null;
        try {
            String raw = Files.readString(file).trim();
            int n = Integer.parseInt(raw);
            if (n > 0 && n < 65536) return n;
        } catch (Exception ignored) {}
        return null;
    }

    private static void write(Path file, int port) {
        try { Files.writeString(file, String.valueOf(port)); } catch (IOException ignored) {}
    }
}
