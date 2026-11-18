package dev.survivalcommands.mixin.lan;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
abstract class IntegratedLanCheatMixin {
    @Inject(method = "setWorldAllowCommands", at = @At("HEAD"), cancellable = true)
    private void survivalCommands$preserveWorldCommandSetting(boolean requested, CallbackInfo callback) {
        var server = (IntegratedServer) (Object) this;
        if (requested != server.getWorldData().isAllowCommands()) {
            callback.cancel();
        }
    }

    @ModifyVariable(method = "setGuestCommandAccess", at = @At("HEAD"), argsOnly = true)
    private boolean survivalCommands$denyGuestCheats(boolean requested) {
        return false;
    }
}
