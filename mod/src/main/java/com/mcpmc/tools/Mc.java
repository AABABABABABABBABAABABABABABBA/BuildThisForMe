package com.mcpmc.tools;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class Mc {
    private Mc() {}

    public static MinecraftClient client() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null) throw new IllegalStateException("Minecraft not running");
        return c;
    }

    public static ClientPlayerEntity player() {
        var p = client().player;
        if (p == null) throw new IllegalStateException("not in a world yet");
        return p;
    }

    public static ClientWorld world() {
        var w = client().world;
        if (w == null) throw new IllegalStateException("no world loaded");
        return w;
    }

    public static ClientPlayNetworkHandler net() {
        var n = client().getNetworkHandler();
        if (n == null) throw new IllegalStateException("not connected to a server");
        return n;
    }

    public static boolean connected() {
        return MinecraftClient.getInstance() != null
                && MinecraftClient.getInstance().getNetworkHandler() != null
                && MinecraftClient.getInstance().player != null;
    }

    public static <T> T onClientThread(java.util.concurrent.Callable<T> task) throws Exception {
        MinecraftClient c = client();
        java.util.concurrent.CompletableFuture<T> f = new java.util.concurrent.CompletableFuture<>();
        c.execute(() -> {
            try { f.complete(task.call()); }
            catch (Throwable t) { f.completeExceptionally(t); }
        });
        try { return f.get(10, java.util.concurrent.TimeUnit.SECONDS); }
        catch (java.util.concurrent.ExecutionException e) {
            Throwable c2 = e.getCause();
            if (c2 instanceof Exception ex) throw ex;
            throw new RuntimeException(c2);
        }
    }
}
