package dev.survivalcommands.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionPackets {
    public static final CustomPacketPayload.Type<Message> REQUEST = type("permissions_request_v1");
    public static final CustomPacketPayload.Type<Message> UPDATE = type("permissions_update_v1");
    public static final CustomPacketPayload.Type<Message> SNAPSHOT = type("permissions_snapshot_v1");

    private PermissionPackets() {}

    private static CustomPacketPayload.Type<Message> type(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("survivalcommands", name));
    }

    public static void sendSnapshot(MinecraftServer server, ServerPlayer player) {
        FriendlyByteBuf data = PermissionsProtocol.encodeSnapshot(server, player);
        if (data != null) {
            player.connection.send(new ClientboundCustomPayloadPacket(Message.copyOf(SNAPSHOT, data)));
        }
    }

    public static <B extends FriendlyByteBuf> StreamCodec<B, Message> codec(CustomPacketPayload.Type<Message> type, int maxBytes) {
        return StreamCodec.of((buffer, message) -> buffer.writeBytes(message.bytes()), buffer -> {
            int size = buffer.readableBytes();
            if (size > maxBytes) {
                throw new IllegalArgumentException("Invalid command settings packet size");
            }
            byte[] bytes = new byte[size];
            buffer.readBytes(bytes);
            return new Message(type, bytes);
        });
    }

    public record Message(CustomPacketPayload.Type<Message> type, byte[] bytes) implements CustomPacketPayload {
        public static Message copyOf(CustomPacketPayload.Type<Message> type, FriendlyByteBuf data) {
            try {
                byte[] bytes = new byte[data.readableBytes()];
                data.readBytes(bytes);
                return new Message(type, bytes);
            } finally {
                data.release();
            }
        }

        public FriendlyByteBuf buffer() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        }
    }
}
