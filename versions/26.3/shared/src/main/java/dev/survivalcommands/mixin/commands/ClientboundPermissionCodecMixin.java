package dev.survivalcommands.mixin.commands;

import dev.survivalcommands.network.PermissionPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientboundCustomPayloadPacket.class)
abstract class ClientboundPermissionCodecMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"),
            index = 0)
    private static <B extends FriendlyByteBuf> CustomPacketPayload.FallbackProvider<B> survivalcommands$settingsCodec(
            CustomPacketPayload.FallbackProvider<B> fallback) {
        return id -> {
            if (PermissionPackets.SNAPSHOT.id().equals(id)) {
                return PermissionPackets.codec(PermissionPackets.SNAPSHOT, 1_048_576);
            }
            return fallback.create(id);
        };
    }
}
