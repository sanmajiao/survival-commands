package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanCheatControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = LanCheatControls.MCWIFIPNP_SCREEN, remap = false)
abstract class McWifiPnpWorldOptionsMixin {
    @Inject(method = "updateGuestCommandAccessButton", at = @At("TAIL"), require = 0)
    private void survivalCommands$keepGuestCheatsDisabled(IntegratedServer server, CallbackInfo callback) {
        LanCheatControls.disable((Screen) (Object) this, Minecraft.getInstance());
    }

    // The two locked controls also drive changes to the operator list outside the vanilla setters.
    @Redirect(method = {"applyGeneralChanges", "applyChanges"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/ServerOpList;add(Lnet/minecraft/server/players/ServerOpListEntry;)Z"),
            require = 0)
    private boolean survivalCommands$preventLanOperatorGrant(ServerOpList operators, ServerOpListEntry entry) {
        return false;
    }

    @Redirect(method = {"applyGeneralChanges", "applyChanges"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/ServerOpList;remove(Lnet/minecraft/server/players/NameAndId;)Z"),
            require = 0)
    private boolean survivalCommands$preserveLanOperatorEntry(ServerOpList operators, NameAndId player) {
        return false;
    }

    @Redirect(method = "applyChanges", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/ServerOpList;clear()V"), require = 0)
    private void survivalCommands$preserveLanOperatorList(ServerOpList operators) {
    }
}
