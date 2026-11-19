package dev.survivalcommands.commands;

import net.minecraft.server.MinecraftServer;

/** Reflects permissions already opened to ordinary players by the installed mods. */
public final class CommandCompatibility {
    private CommandCompatibility() {}

    public static boolean allows(MinecraftServer server, CommandCatalog.Entry entry) {
        if (!CommandPermissions.applies(server) || CommandCatalog.GAME_MODE.equals(entry.id())) {
            return false;
        }
        var source = server.getPlayerList().getPlayers().stream()
                .filter(player -> GameCommandAccess.isOwner(server, player))
                .findFirst().map(player -> player.createCommandSourceStack())
                .orElseGet(server::createCommandSourceStack);
        var unprivileged = GameCommandAccess.unprivileged(source);
        var root = server.getCommands().getDispatcher().getRoot();
        return entry.roots().stream().map(root::getChild)
                .anyMatch(node -> node != null && node.canUse(unprivileged));
    }
}
