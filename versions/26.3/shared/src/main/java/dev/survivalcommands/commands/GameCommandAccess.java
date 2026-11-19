package dev.survivalcommands.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;

public final class GameCommandAccess {
    private GameCommandAccess() {}

    static CommandSourceStack elevate(CommandSourceStack source) {
        PermissionSet current = source.permissions();
        if (current instanceof LevelBasedPermissionSet) {
            return source.withPermission(LevelBasedPermissionSet.OWNER);
        }
        return source.withPermission(permission -> current.hasPermission(permission)
                || LevelBasedPermissionSet.OWNER.hasPermission(permission));
    }

    static CommandSourceStack unprivileged(CommandSourceStack source) {
        return source.withPermission(LevelBasedPermissionSet.ALL);
    }

    public static boolean isOwner(MinecraftServer server, ServerPlayer player) {
        return server.isSingleplayerOwner(new NameAndId(player.getGameProfile()));
    }
}
