package dev.survivalcommands.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class GameCommandAccess {
    private GameCommandAccess() {}

    static CommandSourceStack elevate(CommandSourceStack source) {
        return source.withMaximumPermission(4);
    }

    static CommandSourceStack unprivileged(CommandSourceStack source) {
        return source.withPermission(0);
    }

    public static boolean isOwner(MinecraftServer server, ServerPlayer player) {
        return server.isSingleplayerOwner(player.getGameProfile());
    }
}
