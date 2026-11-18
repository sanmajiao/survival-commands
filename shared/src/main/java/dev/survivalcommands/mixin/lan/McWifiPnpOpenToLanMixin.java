package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.McWifiPnpAdapter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.satxm.mcwifipnp.MCWiFiPnPUnit", remap = false)
abstract class McWifiPnpOpenToLanMixin {
    @Unique private static McWifiPnpAdapter.HostOperator survivalCommands$hostOperator;

    @Inject(method = "OpenToLan()V", at = @At("HEAD"), require = 0, remap = false)
    private static void survivalCommands$disableMcWifiPnpCheats(CallbackInfo callback) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        survivalCommands$hostOperator = McWifiPnpAdapter.keepExistingOperator(server);
        McWifiPnpAdapter.disableLegacyCheats(server);
    }

    @Inject(method = "OpenToLan()V", at = @At("RETURN"), require = 0, remap = false)
    private static void survivalCommands$restoreHostOperator(CallbackInfo callback) {
        var hostOperator = survivalCommands$hostOperator;
        survivalCommands$hostOperator = null;
        if (hostOperator != null) {
            hostOperator.restore();
        }
    }
}
