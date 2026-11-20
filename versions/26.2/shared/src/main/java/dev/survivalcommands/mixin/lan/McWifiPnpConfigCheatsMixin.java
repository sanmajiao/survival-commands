package dev.survivalcommands.mixin.lan;

import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** mcwifipnp 26.2 applies these fields directly, outside the integrated server's setters. */
@Pseudo
@Mixin(targets = "io.github.satxm.mcwifipnp.Config", remap = false)
abstract class McWifiPnpConfigCheatsMixin {
    @Redirect(method = "applyTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/WorldData;setAllowCommands(Z)V"), require = 0)
    private void survivalCommands$preserveWorldCommands(WorldData data, boolean requested) {
    }

    @Redirect(method = "applyTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;setAllowCommandsForAllPlayers(Z)V"), require = 0)
    private void survivalCommands$disableGuestCommands(PlayerList players, boolean requested) {
        players.setAllowCommandsForAllPlayers(false);
    }
}
