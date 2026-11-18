package dev.survivalcommands.mixin.guard;

import dev.survivalcommands.guard.SurvivalGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "changeGameModeForPlayer", at = @At("HEAD"), cancellable = true)
    private void survivalCommands$denyCreativeSwitch(GameType mode, CallbackInfoReturnable<Boolean> callback) {
        if (SurvivalGuard.denies(player.level().getServer(), mode)) {
            callback.setReturnValue(false);
        }
    }

    // Saved player modes and respawn copies bypass the public change method.
    @ModifyVariable(method = "setGameModeForPlayer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private GameType survivalCommands$sanitizeRestoredGameMode(GameType mode) {
        return SurvivalGuard.allowedMode(player.level().getServer(), mode);
    }
}
