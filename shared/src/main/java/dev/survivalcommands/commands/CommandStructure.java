package dev.survivalcommands.commands;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.survivalcommands.api.CreativeModePolicy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CommandStructure {
    public static final int MAX_SETTINGS = 1024;
    public static final int MAX_LENGTH = 128;
    private static final int MAX_DEPTH = 6;

    private CommandStructure() {}

    public record Setting(String id, String parentId, String command, List<String> aliases, String labelKey,
                          boolean available) {}

    public static List<Setting> settings(MinecraftServer server) {
        var settings = new ArrayList<Setting>();
        var dispatcherRoot = server.getCommands().getDispatcher().getRoot();
        for (CommandCatalog.Entry entry : CommandCatalog.entries()) {
            var node = entry.roots().stream().map(dispatcherRoot::getChild)
                    .filter(candidate -> candidate != null).findFirst().orElse(null);
            settings.add(new Setting(entry.id(), null, entry.command(), entry.aliases(), entry.labelKey(), node != null));
        }
        for (CommandCatalog.Entry entry : CommandCatalog.entries()) {
            var node = entry.roots().stream().map(dispatcherRoot::getChild)
                    .filter(candidate -> candidate != null).findFirst().orElse(null);
            if (node == null) {
                continue;
            }
            if (CommandCatalog.GAME_MODE.equals(entry.id())) {
                for (String mode : CommandCatalog.GAME_MODES) {
                    String id = entry.id() + "/" + mode;
                    settings.add(new Setting(id, entry.id(), entry.command() + " " + mode, List.of(),
                            "survivalcommands.command.gamemode." + mode,
                            !mode.equals("creative") || !CreativeModePolicy.isLocked(server)));
                }
            } else {
                addChildren(settings, entry.id(), entry.command(), node, 0);
            }
        }
        return List.copyOf(settings);
    }

    private static void addChildren(List<Setting> settings, String parent, String command,
                                    CommandNode<CommandSourceStack> node, int depth) {
        if (depth >= MAX_DEPTH) {
            return;
        }
        if (node.getRedirect() instanceof LiteralCommandNode<CommandSourceStack> redirected) {
            node = redirected;
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            if (settings.size() >= MAX_SETTINGS - CommandCatalog.GAME_MODES.size()) {
                return;
            }
            if (!(child instanceof LiteralCommandNode<CommandSourceStack> literal)) {
                continue;
            }
            String id = childId(parent, literal.getLiteral());
            String fullCommand = command + " " + literal.getLiteral();
            if (id != null && fullCommand.getBytes(StandardCharsets.UTF_8).length <= MAX_LENGTH) {
                settings.add(new Setting(id, parent, fullCommand, List.of(),
                        "survivalcommands.command." + id.replace('/', '.'), true));
                addChildren(settings, id, fullCommand, child, depth + 1);
            }
        }
    }

    static String childId(String parent, String literal) {
        String id = parent + "/" + literal;
        if (literal.indexOf('/') >= 0 || id.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH) {
            return null;
        }
        return id;
    }
}
