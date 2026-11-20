package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.McWifiPnpSession;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runtime max players must win over mcwifipnp's disk lookup while the world is open. */
@Mixin(value = IntegratedServer.class, priority = 1100)
abstract class IntegratedLanSessionMixin {
    @Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true, order = 900)
    private void survivalCommands$useRunningPlayerLimit(CallbackInfoReturnable<Integer> callback) {
        var server = (IntegratedServer) (Object) this;
        var active = McWifiPnpSession.options(server);
        if (active != null && server.isPublished()) {
            callback.setReturnValue(active.maxPlayers());
        }
    }

    @Inject(method = "unpublishServer", at = @At("RETURN"))
    private void survivalCommands$clearRunningOptions(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            McWifiPnpSession.start((IntegratedServer) (Object) this, null);
        }
    }
}
