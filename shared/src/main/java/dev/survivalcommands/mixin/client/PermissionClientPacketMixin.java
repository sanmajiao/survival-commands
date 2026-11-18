package dev.survivalcommands.mixin.client;

import dev.survivalcommands.client.CommandSettingsClient;
import dev.survivalcommands.network.PermissionPackets;
import dev.survivalcommands.network.PermissionsProtocol;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class PermissionClientPacketMixin {
    @Shadow public abstract void send(Packet<?> packet);

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void survivalcommands$subscribe(ClientboundLoginPacket packet, CallbackInfo ci) {
        CommandSettingsClient.disconnect();
        CommandSettingsClient.setSender((id, enabled) -> send(new ServerboundCustomPayloadPacket(
                PermissionPackets.UPDATE, new PermissionsProtocol.Change(id, enabled).encode())));
        send(new ServerboundCustomPayloadPacket(PermissionPackets.REQUEST, new FriendlyByteBuf(Unpooled.buffer(0))));
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void survivalcommands$disconnect(CallbackInfo ci) {
        Minecraft.getInstance().execute(CommandSettingsClient::disconnect);
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void survivalcommands$receiveSnapshot(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (!PermissionPackets.SNAPSHOT.equals(packet.getIdentifier())) {
            return;
        }
        ci.cancel();
        FriendlyByteBuf data = packet.getData();
        PermissionsProtocol.Snapshot snapshot;
        try {
            snapshot = PermissionsProtocol.Snapshot.read(data);
        } catch (RuntimeException invalidPayload) {
            return;
        } finally {
            data.release();
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if ((Object) minecraft.getConnection() == this) {
                var entries = snapshot.rows().stream().map(row -> new CommandSettingsClient.Entry(
                        row.entry().id(), row.entry().command(), row.entry().aliases(), row.entry().labelKey(),
                        row.enabled(), row.entry().available(), row.entry().parentId())).toList();
                CommandSettingsClient.sync(new CommandSettingsClient.Snapshot(snapshot.editable(), entries));
            }
        });
    }
}
