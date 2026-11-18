package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanCheatControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class LanOptionsScreenMixin {
    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void survivalCommands$disableCheatControls(CallbackInfo callback) {
        LanCheatControls.disable((Screen) (Object) this, Minecraft.getInstance());
    }
}
