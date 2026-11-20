package dev.survivalcommands.client;

import dev.survivalcommands.client.CommandSettingsClient.Entry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds one effective state per command for ordering, labels, and editing. */
final class CommandTree {
    private final List<Node> roots = new ArrayList<>();

    CommandTree(List<Entry> entries) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (Entry entry : entries) {
            nodes.computeIfAbsent(entry.id(), ignored -> new Node(entry));
        }
        for (Node node : nodes.values()) {
            String parentId = node.entry.parentId();
            Node parent = nodes.get(parentId);
            if (parent == null || !node.entry.id().startsWith(parentId + "/")) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }
        resolve(roots, false);
    }

    // ---------- Permission state and stable tree ordering ----------

    private static void resolve(List<Node> siblings, boolean inherited) {
        for (Node node : siblings) {
            Entry entry = node.entry;
            boolean effective = entry.available() && (entry.enabled() || entry.external() || inherited);
            resolve(node.children, effective);
            boolean descendantGranted = node.children.stream().anyMatch(child -> child.hasGrant);
            if (!entry.available()) {
                node.state = entry.id().equals("gamemode/creative") ? State.LOCKED : State.UNAVAILABLE;
            } else if (entry.external()) {
                node.state = State.EXTERNAL;
            } else if (entry.enabled()) {
                node.state = State.ON;
            } else if (inherited) {
                node.state = State.INHERITED;
            } else {
                node.state = descendantGranted ? State.PARTIAL : State.OFF;
            }
            node.hasGrant = effective || descendantGranted;
        }
        // List.sort is stable, so unchanged groups keep the server's catalog order.
        siblings.sort(Comparator.comparingInt(node -> node.state.enabled ? 0 : node.hasGrant ? 1 : 2));
    }

    enum State {
        ON(true), EXTERNAL(true), INHERITED(true), PARTIAL(false), OFF(false), LOCKED(false), UNAVAILABLE(false);

        final boolean enabled;

        State(boolean enabled) {
            this.enabled = enabled;
        }
    }

    // ---------- Search projection ----------

    List<Row> visible(String query, Set<String> expanded, Set<String> collapsedSearch) {
        String key = CommandSearch.normalize(query);
        boolean searching = !key.isEmpty();
        Map<Node, Boolean> matches = new HashMap<>();
        if (searching) {
            for (Node root : roots) {
                collectMatches(root, key, matches);
            }
        }
        List<Row> rows = new ArrayList<>();
        for (Node root : roots) {
            append(root, rows, matches, !searching, searching, expanded, collapsedSearch, 0);
        }
        return rows;
    }

    private static boolean collectMatches(Node node, String key, Map<Node, Boolean> matches) {
        boolean ownMatch = CommandSearch.matches(node.search, key);
        boolean branchMatch = false;
        for (Node child : node.children) {
            branchMatch |= collectMatches(child, key, matches);
        }
        if (ownMatch || branchMatch) {
            matches.put(node, ownMatch);
        }
        return ownMatch || branchMatch;
    }

    private static void append(Node node, List<Row> rows, Map<Node, Boolean> matches, boolean showWholeBranch,
                               boolean searching, Set<String> expanded, Set<String> collapsedSearch, int depth) {
        if (!showWholeBranch && !matches.containsKey(node)) {
            return;
        }
        String id = node.entry.id();
        boolean open = !node.children.isEmpty()
                && (searching ? !collapsedSearch.contains(id) : expanded.contains(id));
        rows.add(new Row(node.entry, node.label, node.commands, node.state, depth, !node.children.isEmpty(), open));
        if (open) {
            boolean showChildren = showWholeBranch || Boolean.TRUE.equals(matches.get(node));
            for (Node child : node.children) {
                append(child, rows, matches, showChildren, searching, expanded, collapsedSearch, depth + 1);
            }
        }
    }

    record Row(Entry entry, Component label, String commands, State state, int depth,
               boolean hasChildren, boolean expanded) {}

    // ---------- Labels and aliases ----------

    private static final class Node {
        private final Entry entry;
        private final Component label;
        private final String commands;
        private final CommandSearch.Index search;
        private final List<Node> children = new ArrayList<>();
        private State state;
        private boolean hasGrant;

        private Node(Entry entry) {
            this.entry = entry;
            String action = entry.command().substring(entry.command().lastIndexOf(' ') + 1);
            String labelKey = entry.labelKey();
            if ("gamerule".equals(entry.parentId()) && !CommandTranslations.has(labelKey)) {
                labelKey = "gamerule." + action;
            }
            label = CommandTranslations.has(labelKey) ? CommandTranslations.text(labelKey) : Component.literal(action);
            Set<String> names = new LinkedHashSet<>(entry.aliases());
            names.add(label.getString());
            names.add(entry.id());
            names.add(entry.command());
            if (labelKey != null) {
                String keywords = labelKey + ".keywords";
                if (CommandTranslations.hasLanguageKey(keywords)) {
                    names.addAll(List.of(I18n.get(keywords).split("\\s*,\\s*")));
                }
                String chineseLabel = CommandTranslations.chinese(labelKey);
                if (chineseLabel != null) {
                    names.add(chineseLabel);
                }
                String chineseKeywords = CommandTranslations.chinese(keywords);
                if (chineseKeywords != null) {
                    names.addAll(List.of(chineseKeywords.split("\\s*,\\s*")));
                }
            }
            search = CommandSearch.index(List.copyOf(names));
            Set<String> syntax = new LinkedHashSet<>();
            syntax.add(withSlash(entry.command()));
            for (String alias : entry.aliases()) {
                String unqualified = alias.startsWith("minecraft:") ? alias.substring("minecraft:".length()) : null;
                if (unqualified == null || !(entry.command().equals(unqualified) || entry.aliases().contains(unqualified))) {
                    syntax.add(withSlash(alias));
                }
            }
            commands = String.join(" · ", syntax);
        }

        private static String withSlash(String name) {
            return name.startsWith("/") ? name : "/" + name;
        }
    }
}
