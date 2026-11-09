package com.mcpmc.mixin;

import com.mcpmc.hooks.GuiCapture;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(method = "setHeader(Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void mcpmc_setHeader(Text h, CallbackInfo ci) {
        GuiCapture.setTablistHeader(h == null ? "" : h.getString());
    }

    @Inject(method = "setFooter(Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void mcpmc_setFooter(Text f, CallbackInfo ci) {
        GuiCapture.setTablistFooter(f == null ? "" : f.getString());
    }
}
