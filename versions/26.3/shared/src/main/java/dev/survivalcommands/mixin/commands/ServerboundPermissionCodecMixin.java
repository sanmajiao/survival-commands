package dev.survivalcommands.mixin.commands;

import dev.survivalcommands.commands.CommandStructure;
import dev.survivalcommands.network.PermissionPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerboundCustomPayloadPacket.class)
abstract class ServerboundPermissionCodecMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"),
            index = 0)
    private static <B extends FriendlyByteBuf> CustomPacketPayload.FallbackProvider<B> survivalcommands$settingsCodec(
            CustomPacketPayload.FallbackProvider<B> fallback) {
        return id -> {
            if (PermissionPackets.REQUEST.id().equals(id)) {
                return PermissionPackets.codec(PermissionPackets.REQUEST, 0);
            }
            if (PermissionPackets.UPDATE.id().equals(id)) {
                return PermissionPackets.codec(PermissionPackets.UPDATE, 4 * CommandStructure.MAX_LENGTH + 6);
            }
            return fallback.create(id);
        };
    }
}
