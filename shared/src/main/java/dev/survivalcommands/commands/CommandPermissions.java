package dev.survivalcommands.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.survivalcommands.api.CreativeModePolicy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class CommandPermissions {
    private final TreeSet<String> enabled = new TreeSet<>();
    private final Runnable markDirty;

    CommandPermissions(Runnable markDirty) {
        this.markDirty = markDirty;
    }

    public static boolean applies(MinecraftServer server) {
        return server.isSingleplayer();
    }

    public static CommandPermissions forWorld(MinecraftServer server) {
        return WorldCommandStorage.forWorld(server);
    }

    void load(Iterable<String> ids) {
        for (String id : ids) {
            enabled.add(id);
        }
    }

    public Set<String> enabled() {
        return Collections.unmodifiableSet(enabled);
    }

    public Set<String> enabled(MinecraftServer server) {
        TreeSet<String> effective = new TreeSet<>(enabled);
        effective.addAll(externalGrants(server));
        return Collections.unmodifiableSet(effective);
    }

    public static Set<String> externalGrants(MinecraftServer server) {
        Set<String> external = new TreeSet<>();
        for (CommandCatalog.Entry entry : CommandCatalog.entries(server)) {
            if (CommandCompatibility.allows(server, entry)) {
                external.add(entry.id());
            }
        }
        return external;
    }

    public boolean setEnabled(String id, boolean value) {
        boolean changed = value ? enabled.add(id) : enabled.remove(id);
        if (changed) {
            markDirty.run();
        }
        return changed;
    }

    public static CommandSourceStack forInput(CommandSourceStack source, StringReader reader) {
        MinecraftServer server = source.getServer();
        if (!applies(server)) {
            return source;
        }
        StringReader arguments = new StringReader(reader);
        String root = readToken(arguments);
        CommandCatalog.Entry entry = CommandCatalog.forRoot(server, root);
        if (entry == null) {
            return source;
        }
        var node = server.getCommands().getDispatcher().getRoot().getChild(root);
        return forWorld(server).allows(entry, node, arguments, server) ? GameCommandAccess.elevate(source) : source;
    }

    private boolean allows(CommandCatalog.Entry entry, CommandNode<CommandSourceStack> node,
                           StringReader arguments, MinecraftServer server) {
        String id = entry.id();
        if (CommandCatalog.GAME_MODE.equals(id)) {
            String mode = readToken(arguments);
            return CommandCatalog.GAME_MODES.contains(mode)
                    && (!mode.equals("creative") || !CreativeModePolicy.isLocked(server))
                    && (enabled.contains(id) || enabled.contains(id + "/" + mode));
        }
        if (enabled.contains(id) || CommandCompatibility.allows(server, entry)) {
            return true;
        }
        while (node != null && arguments.canRead()) {
            if (node.getRedirect() instanceof LiteralCommandNode<CommandSourceStack> redirected) {
                node = redirected;
            }
            node = node.getChild(readToken(arguments));
            if (!(node instanceof LiteralCommandNode<CommandSourceStack> literal)) {
                return false;
            }
            id = CommandStructure.childId(id, literal.getLiteral());
            if (id == null) {
                return false;
            }
            if (enabled.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private static String readToken(StringReader reader) {
        reader.skipWhitespace();
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    public static boolean canUseForTree(CommandNode<CommandSourceStack> node, CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        CommandCatalog.Entry entry = CommandCatalog.forRoot(server, node.getName());
        if (entry != null && applies(server)
                && server.getCommands().getDispatcher().getRoot().getChild(node.getName()) == node) {
            TreeSet<String> enabled = forWorld(server).enabled;
            if (enabled.contains(entry.id()) || CommandCompatibility.allows(server, entry)) {
                return node.canUse(GameCommandAccess.elevate(source));
            }
            String prefix = entry.id() + "/";
            String firstChild = enabled.ceiling(prefix);
            if (firstChild != null && firstChild.startsWith(prefix)) {
                return node.canUse(GameCommandAccess.elevate(source));
            }
        }
        return node.canUse(source);
    }
}
