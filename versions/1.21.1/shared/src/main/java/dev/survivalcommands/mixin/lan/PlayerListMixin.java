package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanCheatPolicy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    // 1.21.1 renamed this setter. LAN mods can call it again after publication.
    @ModifyVariable(method = "setAllowCommandsForAllPlayers", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean survivalCommands$disableSharedCheats(boolean allowCheats) {
        return allowCheats && !LanCheatPolicy.disablesCheats(server);
    }
}
