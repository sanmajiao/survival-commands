package dev.survivalcommands.mixin.client;

import dev.survivalcommands.client.CommandSettingsClient;
import dev.survivalcommands.network.PermissionPackets;
import dev.survivalcommands.network.PermissionsProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class PermissionClientPacketMixin {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void survivalcommands$subscribe(ClientboundLoginPacket packet, CallbackInfo ci) {
        ClientPacketListener listener = (ClientPacketListener) (Object) this;
        CommandSettingsClient.disconnect();
        CommandSettingsClient.setSender((id, enabled) -> listener.send(new ServerboundCustomPayloadPacket(
                PermissionPackets.Message.copyOf(PermissionPackets.UPDATE, new PermissionsProtocol.Change(id, enabled).encode()))));
        listener.send(new ServerboundCustomPayloadPacket(new PermissionPackets.Message(PermissionPackets.REQUEST, new byte[0])));
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void survivalcommands$disconnect(CallbackInfo ci) {
        Minecraft.getInstance().execute(CommandSettingsClient::disconnect);
    }

    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("HEAD"), cancellable = true)
    private void survivalcommands$receiveSnapshot(CustomPacketPayload payload, CallbackInfo ci) {
        if (!(payload instanceof PermissionPackets.Message message)) {
            return;
        }
        ci.cancel();
        if (!message.type().equals(PermissionPackets.SNAPSHOT) || (Object) Minecraft.getInstance().getConnection() != this) {
            return;
        }
        FriendlyByteBuf data = message.buffer();
        PermissionsProtocol.Snapshot snapshot;
        try {
            snapshot = PermissionsProtocol.Snapshot.read(data);
        } catch (RuntimeException invalidPayload) {
            return;
        } finally {
            data.release();
        }
        var entries = snapshot.rows().stream().map(row -> new CommandSettingsClient.Entry(
                row.entry().id(), row.entry().command(), row.entry().aliases(), row.entry().labelKey(),
                row.enabled(), row.entry().available(), row.entry().parentId())).toList();
        CommandSettingsClient.sync(new CommandSettingsClient.Snapshot(snapshot.editable(), entries));
    }
}
