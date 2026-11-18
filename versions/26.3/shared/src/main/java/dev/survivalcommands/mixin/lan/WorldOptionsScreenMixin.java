package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanCheatControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOptionsScreen.class)
abstract class WorldOptionsScreenMixin {
    // Selecting a multiplayer scope can re-enable the guest toggle after widget registration.
    @Inject(method = "updateGuestCommandAccessButton", at = @At("TAIL"))
    private void survivalCommands$keepGuestCheatsDisabled(IntegratedServer server, CallbackInfo callback) {
        LanCheatControls.disable((Screen) (Object) this, Minecraft.getInstance());
    }
}
