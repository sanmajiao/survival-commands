package dev.survivalcommands.mixin.guard;

import dev.survivalcommands.guard.SurvivalGuard;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
abstract class IntegratedServerMixin {
    @ModifyVariable(method = "publishServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private GameType survivalCommands$sanitizeLanGameMode(GameType mode) {
        return SurvivalGuard.allowedMode((MinecraftServer) (Object) this, mode);
    }

    // The integrated override also resets the forced LAN game mode after calling super.
    @Inject(method = "setDefaultGameType", at = @At("HEAD"), cancellable = true)
    private void survivalCommands$denyCreativeDefault(GameType mode, CallbackInfo callback) {
        if (SurvivalGuard.denies((MinecraftServer) (Object) this, mode)) {
            callback.cancel();
        }
    }
}
