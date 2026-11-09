package com.mcpmc.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void mcpmc_onAdd(Text text, CallbackInfo ci) {
        // chat events fire via ClientReceiveMessageEvents; this mixin exists to guarantee overlay re-render.
    }
}
