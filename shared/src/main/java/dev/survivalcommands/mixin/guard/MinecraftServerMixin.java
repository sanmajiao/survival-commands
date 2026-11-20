package dev.survivalcommands.mixin.guard;

import dev.survivalcommands.api.CreativeModePolicy;
import dev.survivalcommands.guard.SurvivalGuard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin implements CreativeModePolicy {
    // Capture the save's observed initial game type before LAN or dedicated settings run.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void survivalCommands$captureWorldOrigin(CallbackInfo callback) {
        SurvivalGuard.isCreativeLocked((MinecraftServer) (Object) this);
    }

    @Override
    public boolean survivalcommands$isCreativeLocked() {
        return SurvivalGuard.blocksCreative((MinecraftServer) (Object) this);
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void survivalCommands$enforcePublishedRestriction(BooleanSupplier haveTime, CallbackInfo callback) {
        SurvivalGuard.enforcePublishedRestriction((MinecraftServer) (Object) this);
    }

    @Inject(method = "setDefaultGameType", at = @At("HEAD"), cancellable = true)
    private void survivalCommands$denyCreativeDefault(GameType mode, CallbackInfo callback) {
        if (SurvivalGuard.denies((MinecraftServer) (Object) this, mode)) {
            callback.cancel();
        }
    }
}
