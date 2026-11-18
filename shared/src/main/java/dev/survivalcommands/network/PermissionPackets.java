package dev.survivalcommands.network;

import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionPackets {
    public static final ResourceLocation REQUEST = new ResourceLocation("survivalcommands", "permissions_request_v1");
    public static final ResourceLocation UPDATE = new ResourceLocation("survivalcommands", "permissions_update_v1");
    public static final ResourceLocation SNAPSHOT = new ResourceLocation("survivalcommands", "permissions_snapshot_v1");

    private PermissionPackets() {}

    public static void sendSnapshot(MinecraftServer server, ServerPlayer player) {
        var data = PermissionsProtocol.encodeSnapshot(server, player);
        if (data != null) {
            player.connection.send(new ClientboundCustomPayloadPacket(SNAPSHOT, data));
        }
    }
}
