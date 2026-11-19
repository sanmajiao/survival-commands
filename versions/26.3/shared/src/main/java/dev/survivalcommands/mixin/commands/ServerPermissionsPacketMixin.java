package dev.survivalcommands.mixin.commands;

import dev.survivalcommands.commands.CommandPermissions;
import dev.survivalcommands.network.PermissionPackets;
import dev.survivalcommands.network.PermissionsProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerPermissionsPacketMixin implements PermissionsProtocol.Subscription {
    @Shadow public ServerPlayer player;
    @Unique private boolean survivalcommands$subscribed;
    @Unique private int survivalcommands$syncTicks;
    @Unique private Set<String> survivalcommands$lastEnabled = Set.of();

    @Override
    public boolean survivalcommands$subscribed() {
        return survivalcommands$subscribed;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void survivalcommands$refreshExternalPermissions(CallbackInfo ci) {
        if (!survivalcommands$subscribed || ++survivalcommands$syncTicks < 100) {
            return;
        }
        survivalcommands$syncTicks = 0;
        MinecraftServer server = player.level().getServer();
        if (server == null || !CommandPermissions.applies(server)) {
            return;
        }
        Set<String> enabled = CommandPermissions.forWorld(server).enabled(server);
        if (!enabled.equals(survivalcommands$lastEnabled)) {
            survivalcommands$lastEnabled = enabled;
            server.getCommands().sendCommands(player);
            PermissionPackets.sendSnapshot(server, player);
        }
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void survivalcommands$handleSettings(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (!(packet.payload() instanceof PermissionPackets.Message message)) {
            return;
        }
        ci.cancel();
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        if (message.type().equals(PermissionPackets.REQUEST)) {
            server.execute(() -> {
                if (!survivalcommands$subscribed) {
                    survivalcommands$subscribed = true;
                    PermissionPackets.sendSnapshot(server, player);
                }
            });
        } else if (message.type().equals(PermissionPackets.UPDATE)) {
            FriendlyByteBuf data = message.buffer();
            PermissionsProtocol.Change change;
            try {
                change = PermissionsProtocol.Change.read(data);
            } catch (RuntimeException invalidPayload) {
                return;
            } finally {
                data.release();
            }
            server.execute(() -> {
                if (survivalcommands$subscribed) {
                    PermissionsProtocol.update(server, player, change, recipient -> PermissionPackets.sendSnapshot(server, recipient));
                }
            });
        }
    }
}
