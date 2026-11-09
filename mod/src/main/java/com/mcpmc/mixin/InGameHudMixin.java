package com.mcpmc.mixin;

import com.mcpmc.hooks.GuiCapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void mcpmc_renderOverlay(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
        String text = GuiCapture.overlayText();
        if (text == null || text.isEmpty()) return;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int x = (w - mc.textRenderer.getWidth(text)) / 2;
        int y = h - 60;
        ctx.fill(x - 4, y - 2, x + mc.textRenderer.getWidth(text) + 4, y + 10, 0x80000000);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text), x, y, 0xFFFFFF55);
    }
}
