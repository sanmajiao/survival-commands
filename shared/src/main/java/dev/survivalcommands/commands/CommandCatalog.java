package dev.survivalcommands.commands;

import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Combines registered command groups with roots supplied by the world's dispatcher. */
public final class CommandCatalog {
    public static final int MAX_ALIASES = 16;
    public static final String GAME_MODE = "gamemode";
    public static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");
    private static final Pattern ROOT = Pattern.compile("[a-z0-9_.-]+(:[a-z0-9_./-]+)?");
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<String, Entry> BY_ROOT = new LinkedHashMap<>();

    static {
        register("teleport", "tp", "teleport", "minecraft:tp", "minecraft:teleport");
        register("tick", "tick");
        vanilla(GAME_MODE);
        for (String root : List.of("weather", "time", "summon", "gamerule", "difficulty",
                "locate", "seed", "spawnpoint", "setworldspawn", "effect", "enchant")) {
            vanilla(root);
        }
        vanilla("experience", "xp");
        for (String root : List.of("give", "clear", "kill", "fill", "setblock", "clone",
                "spreadplayers", "ride", "playsound", "stopsound", "particle", "title",
                "worldborder", "recipe", "advancement")) {
            vanilla(root);
        }
    }

    private CommandCatalog() {}

    private static void vanilla(String root, String... aliases) {
        List<String> names = new ArrayList<>();
        names.add("minecraft:" + root);
        for (String alias : aliases) {
            names.add(alias);
            names.add("minecraft:" + alias);
        }
        register(root, root, names.toArray(String[]::new));
    }

    /** Call during mod initialization. Registration adds an off-by-default option. */
    public static synchronized void register(String id, String command, String... aliases) {
        if (ENTRIES.size() >= 128 || aliases.length > MAX_ALIASES) {
            throw new IllegalArgumentException("Too many command settings or aliases");
        }
        Entry entry = new Entry(id, command, List.of(aliases));
        validate(id);
        if (id.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Command setting ID cannot contain '/': " + id);
        }
        if (ENTRIES.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate command setting " + id);
        }
        for (String root : entry.roots()) {
            validate(root);
            if (BY_ROOT.containsKey(root)) {
                throw new IllegalArgumentException("Command root already registered: " + root);
            }
        }
        ENTRIES.put(id, entry);
        entry.roots().forEach(root -> BY_ROOT.put(root, entry));
    }

    private static void validate(String value) {
        if (value.length() > 96 || !ROOT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid command identifier: " + value);
        }
    }

    public static synchronized List<Entry> entries() {
        return List.copyOf(ENTRIES.values());
    }

    public static synchronized Entry forRoot(String root) {
        return BY_ROOT.get(root);
    }

    public static List<Entry> entries(MinecraftServer server) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        entries().forEach(entry -> entries.put(entry.id(), entry));
        for (var node : server.getCommands().getDispatcher().getRoot().getChildren()) {
            if (entries.size() >= CommandStructure.MAX_SETTINGS - GAME_MODES.size()) {
                break;
            }
            Entry entry = forRoot(server, node.getName());
            if (entry != null) {
                entries.putIfAbsent(entry.id(), entry);
            }
        }
        return List.copyOf(entries.values());
    }

    public static Entry forRoot(MinecraftServer server, String root) {
        Entry registered = forRoot(root);
        if (registered != null) {
            return registered;
        }
        if (root.isEmpty() || root.getBytes(StandardCharsets.UTF_8).length > CommandStructure.MAX_LENGTH
                || server.getCommands().getDispatcher().getRoot().getChild(root) == null) {
            return null;
        }
        String id = root;
        if (!ROOT.matcher(root).matches() || root.indexOf('/') >= 0 || entries().stream()
                .anyMatch(entry -> entry.id().equals(root))) {
            id = "mod." + HexFormat.of().formatHex(root.getBytes(StandardCharsets.UTF_8));
        }
        if (id.getBytes(StandardCharsets.UTF_8).length > CommandStructure.MAX_LENGTH) {
            return null;
        }
        return new Entry(id, root, List.of());
    }

    public record Entry(String id, String command, List<String> aliases) {
        public List<String> roots() {
            List<String> values = new ArrayList<>(aliases.size() + 1);
            values.add(command);
            values.addAll(aliases);
            return values;
        }

        public String labelKey() {
            return "survivalcommands.command." + id;
        }
    }
}
