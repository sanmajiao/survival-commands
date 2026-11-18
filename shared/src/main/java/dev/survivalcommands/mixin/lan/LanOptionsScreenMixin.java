package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanCheatControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
abstract class LanOptionsScreenMixin {
    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void survivalCommands$disableCheatControls(Minecraft minecraft, int width, int height, CallbackInfo callback) {
        LanCheatControls.disable((Screen) (Object) this, minecraft);
    }

    // Tab-based LAN screens add widgets again as the active tab changes.
    @Inject(method = "addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)"
        + "Lnet/minecraft/client/gui/components/events/GuiEventListener;", at = @At("TAIL"))
    private void survivalCommands$disableNewCheatControl(CallbackInfoReturnable<?> callback) {
        LanCheatControls.disableWidget((Screen) (Object) this, Minecraft.getInstance(), callback.getReturnValue());
    }
}
