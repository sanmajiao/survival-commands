package dev.survivalcommands.network;

import dev.survivalcommands.commands.CommandCatalog;
import dev.survivalcommands.commands.CommandPermissions;
import dev.survivalcommands.commands.CommandStructure;
import dev.survivalcommands.commands.GameCommandAccess;
import io.netty.buffer.Unpooled;
import io.netty.channel.local.LocalAddress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class PermissionsProtocol {
    private static final int MAX_ID_LENGTH = CommandStructure.MAX_LENGTH;

    private PermissionsProtocol() {}

    public interface Subscription {
        boolean survivalcommands$subscribed();
    }

    public record Change(String id, boolean enabled) {
        public static Change read(FriendlyByteBuf data) {
            String id = data.readUtf(MAX_ID_LENGTH);
            boolean enabled = data.readBoolean();
            if (data.isReadable()) {
                throw new IllegalArgumentException("Unexpected permissions update data");
            }
            return new Change(id, enabled);
        }

        public FriendlyByteBuf encode() {
            FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
            data.writeUtf(id, MAX_ID_LENGTH);
            data.writeBoolean(enabled);
            return data;
        }
    }

    public record Row(CommandStructure.Setting entry, boolean enabled) {}

    private static boolean canEdit(MinecraftServer server, ServerPlayer player) {
        return player.connection.getRemoteAddress() instanceof LocalAddress
                && GameCommandAccess.isOwner(server, player);
    }

    public record Snapshot(boolean editable, List<Row> rows) {
        public static Snapshot read(FriendlyByteBuf data) {
            boolean editable = data.readBoolean();
            int count = data.readVarInt();
            if (count < 0 || count > CommandStructure.MAX_SETTINGS) {
                throw new IllegalArgumentException("Invalid command settings count");
            }
            List<Row> rows = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                String id = data.readUtf(MAX_ID_LENGTH);
                String parentId = data.readBoolean() ? data.readUtf(MAX_ID_LENGTH) : null;
                String command = data.readUtf(MAX_ID_LENGTH);
                int aliasCount = data.readVarInt();
                if (aliasCount < 0 || aliasCount > CommandCatalog.MAX_ALIASES) {
                    throw new IllegalArgumentException("Invalid command alias count");
                }
                List<String> aliases = new ArrayList<>(aliasCount);
                for (int alias = 0; alias < aliasCount; alias++) {
                    aliases.add(data.readUtf(MAX_ID_LENGTH));
                }
                int flags = data.readUnsignedByte();
                String labelKey = "survivalcommands.command." + id.replace('/', '.');
                var entry = new CommandStructure.Setting(id, parentId, command, List.copyOf(aliases), labelKey, (flags & 1) != 0);
                rows.add(new Row(entry, (flags & 2) != 0));
            }
            return new Snapshot(editable, List.copyOf(rows));
        }
    }

    public static FriendlyByteBuf encodeSnapshot(MinecraftServer server, ServerPlayer player) {
        if (!CommandPermissions.applies(server)) {
            return null;
        }
        List<CommandStructure.Setting> entries = CommandStructure.settings(server);
        Set<String> enabled = CommandPermissions.forWorld(server).enabled(server);
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        data.writeBoolean(canEdit(server, player));
        data.writeVarInt(entries.size());
        for (CommandStructure.Setting entry : entries) {
            data.writeUtf(entry.id(), MAX_ID_LENGTH);
            data.writeBoolean(entry.parentId() != null);
            if (entry.parentId() != null) {
                data.writeUtf(entry.parentId(), MAX_ID_LENGTH);
            }
            data.writeUtf(entry.command(), MAX_ID_LENGTH);
            data.writeVarInt(entry.aliases().size());
            for (String alias : entry.aliases()) {
                data.writeUtf(alias, MAX_ID_LENGTH);
            }
            int flags = (entry.available() ? 1 : 0) | (enabled.contains(entry.id()) ? 2 : 0);
            data.writeByte(flags);
        }
        return data;
    }

    public static void update(MinecraftServer server, ServerPlayer sender, Change change, Consumer<ServerPlayer> sendSnapshot) {
        if (!CommandPermissions.applies(server) || !canEdit(server, sender)
                || !sender.connection.isAcceptingMessages()) {
            return;
        }
        boolean valid = CommandStructure.settings(server).stream().anyMatch(entry ->
                entry.id().equals(change.id()) && (entry.available() || !change.enabled()));
        if (!valid) {
            return;
        }
        if (CommandPermissions.forWorld(server).setEnabled(change.id(), change.enabled())) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                server.getCommands().sendCommands(player);
                if ((Object) player.connection instanceof Subscription subscription && subscription.survivalcommands$subscribed()) {
                    sendSnapshot.accept(player);
                }
            }
        }
    }
}
