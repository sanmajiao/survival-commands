package dev.survivalcommands.mixin.commands;

import dev.survivalcommands.network.PermissionsProtocol;
import dev.survivalcommands.network.PermissionPackets;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerPermissionsPacketMixin implements PermissionsProtocol.Subscription {
    @Shadow @Final private MinecraftServer server;
    @Shadow public ServerPlayer player;
    @Unique private boolean survivalcommands$subscribed;

    @Override
    public boolean survivalcommands$subscribed() {
        return survivalcommands$subscribed;
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void survivalcommands$handleSettings(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (PermissionPackets.REQUEST.equals(packet.getIdentifier())) {
            ci.cancel();
            if (!packet.getData().isReadable()) {
                server.execute(() -> {
                    if (!survivalcommands$subscribed) {
                        survivalcommands$subscribed = true;
                        PermissionPackets.sendSnapshot(server, player);
                    }
                });
            }
        } else if (PermissionPackets.UPDATE.equals(packet.getIdentifier())) {
            ci.cancel();
            // Parse before enqueueing. The packet releases its data when this handler returns.
            PermissionsProtocol.Change change;
            try {
                change = PermissionsProtocol.Change.read(packet.getData());
            } catch (RuntimeException invalidPayload) {
                return;
            }
            server.execute(() -> {
                if (survivalcommands$subscribed) {
                    PermissionsProtocol.update(server, player, change, recipient -> PermissionPackets.sendSnapshot(server, recipient));
                }
            });
        }
    }
}
