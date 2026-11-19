package dev.survivalcommands.commands;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.survivalcommands.api.CreativeModePolicy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class CommandStructure {
    public static final int MAX_SETTINGS = 1024;
    public static final int MAX_LENGTH = 128;
    private static final int MAX_DEPTH = 6;

    private CommandStructure() {}

    public record Setting(String id, String parentId, String command, List<String> aliases, String labelKey,
                          boolean available) {}
    private record Branch(String parent, String command, CommandNode<CommandSourceStack> node, int depth) {}

    public static List<Setting> settings(MinecraftServer server) {
        var settings = new ArrayList<Setting>();
        Deque<Branch> branches = new ArrayDeque<>();
        var dispatcherRoot = server.getCommands().getDispatcher().getRoot();
        List<CommandCatalog.Entry> entries = CommandCatalog.entries(server);
        for (CommandCatalog.Entry entry : entries) {
            var node = entry.roots().stream().map(dispatcherRoot::getChild)
                    .filter(candidate -> candidate != null).findFirst().orElse(null);
            settings.add(new Setting(entry.id(), null, entry.command(), entry.aliases(), entry.labelKey(), node != null));
        }
        for (CommandCatalog.Entry entry : entries) {
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
                branches.addLast(new Branch(entry.id(), entry.command(), node, 0));
            }
        }
        while (!branches.isEmpty() && settings.size() < MAX_SETTINGS) {
            Branch branch = branches.removeFirst();
            addChildren(settings, branches, branch);
        }
        return List.copyOf(settings);
    }

    private static void addChildren(List<Setting> settings, Deque<Branch> branches, Branch branch) {
        int depth = branch.depth();
        if (depth >= MAX_DEPTH) {
            return;
        }
        CommandNode<CommandSourceStack> node = branch.node();
        if (node.getRedirect() instanceof LiteralCommandNode<CommandSourceStack> redirected) {
            node = redirected;
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            if (settings.size() >= MAX_SETTINGS) {
                return;
            }
            if (!(child instanceof LiteralCommandNode<CommandSourceStack> literal)) {
                continue;
            }
            String id = childId(branch.parent(), literal.getLiteral());
            String fullCommand = branch.command() + " " + literal.getLiteral();
            if (id != null && fullCommand.getBytes(StandardCharsets.UTF_8).length <= MAX_LENGTH) {
                settings.add(new Setting(id, branch.parent(), fullCommand, List.of(),
                        "survivalcommands.command." + id.replace('/', '.'), true));
                branches.addLast(new Branch(id, fullCommand, child, depth + 1));
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
