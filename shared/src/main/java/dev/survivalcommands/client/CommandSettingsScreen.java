package dev.survivalcommands.client;

import dev.survivalcommands.client.CommandSettingsClient.Entry;
import dev.survivalcommands.client.CommandSettingsClient.Snapshot;
import dev.survivalcommands.client.CommandSettingsWidgets.CommandList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CommandSettingsScreen extends Screen {
    private static final int LIST_TOP = 69;
    private static final int ROW_HEIGHT = 32;

    private final Screen parent;
    private final Set<String> expandedGroups = new HashSet<>();
    private final Set<String> collapsedSearchGroups = new HashSet<>();
    private Snapshot snapshot;
    private Map<String, IndexedEntry> indexedEntries = Map.of();
    private Map<String, List<IndexedEntry>> children = Map.of();
    private String query = "";
    private CommandList<CommandRow> rows;
    private EditBox search;

    public CommandSettingsScreen(Screen parent) {
        super(CommandTranslations.text("survivalcommands.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rowWidth = Math.min(424, width - 32);
        int searchWidth = Math.min(280, rowWidth);
        double previousScroll = rows == null ? 0.0 : rows.getScrollAmount();
        rows = addRenderableWidget(new CommandList<CommandRow>(minecraft, width, height, LIST_TOP,
                Math.max(LIST_TOP + 1, height - 37), rowWidth, ROW_HEIGHT));

        Component searchLabel = CommandTranslations.text("survivalcommands.settings.search");
        search = addRenderableWidget(new EditBox(font, (width - searchWidth) / 2, 30,
                searchWidth, 20, searchLabel));
        search.setMaxLength(100);
        search.setHint(searchLabel);
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            collapsedSearchGroups.clear();
            refreshRows(0.0);
        });

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - 120) / 2, height - 27, 120, 20).build());
        setInitialFocus(search);
        refreshSnapshot(CommandSettingsClient.snapshot(), previousScroll);
    }

    @Override
    public void tick() {
        CommandSettingsWidgets.tickSearch(search);
        Snapshot latest = CommandSettingsClient.snapshot();
        if (!Objects.equals(latest, snapshot)) {
            refreshSnapshot(latest, rows.getScrollAmount());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        if (snapshot != null) {
            Component status = CommandTranslations.text(snapshot.editable()
                    ? "survivalcommands.settings.shared" : "survivalcommands.settings.read_only");
            graphics.drawCenteredString(font, status, width / 2, 55, 0xC5C5C5);
        }

        if (rows.children().isEmpty()) {
            Component empty = CommandTranslations.text(snapshot == null
                    ? "survivalcommands.settings.waiting" : "survivalcommands.settings.empty");
            int centerY = LIST_TOP + Math.max(8, (height - 37 - LIST_TOP) / 2 - 4);
            graphics.drawCenteredString(font, empty, width / 2, centerY, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshSnapshot(Snapshot latest, double scrollAmount) {
        snapshot = latest;
        indexedEntries = new LinkedHashMap<>();
        children = new HashMap<>();
        for (Entry entry : latest == null ? List.<Entry>of() : latest.commands()) {
            if (indexedEntries.containsKey(entry.id())) {
                continue;
            }
            String labelKey = entry.labelKey();
            String action = entry.command().substring(entry.command().lastIndexOf(' ') + 1);
            if ("gamerule".equals(entry.parentId()) && !CommandTranslations.has(labelKey)) {
                labelKey = "gamerule." + action;
            }
            Component label = CommandTranslations.has(labelKey)
                    ? CommandTranslations.text(labelKey) : Component.literal(action);
            Set<String> names = new LinkedHashSet<>(entry.aliases());
            names.add(label.getString());
            names.add(entry.id());
            names.add(entry.command());

            String keywords = labelKey + ".keywords";
            if (labelKey != null && I18n.exists(keywords)) {
                names.addAll(List.of(I18n.get(keywords).split("\\s*,\\s*")));
            }
            if (labelKey != null) {
                String chineseLabel = CommandTranslations.chinese(labelKey);
                if (chineseLabel != null) {
                    names.add(chineseLabel);
                }
                String chineseKeywords = CommandTranslations.chinese(keywords);
                if (chineseKeywords != null) {
                    names.addAll(List.of(chineseKeywords.split("\\s*,\\s*")));
                }
            }
            var item = new IndexedEntry(entry, label, CommandSearch.index(List.copyOf(names)));
            indexedEntries.put(entry.id(), item);
            if (entry.parentId() != null) {
                children.computeIfAbsent(entry.parentId(), unused -> new ArrayList<>()).add(item);
            }
        }
        refreshRows(scrollAmount);
    }

    private void refreshRows(double scrollAmount) {
        String key = CommandSearch.normalize(query);
        CommandRow focused = rows.getFocused();
        boolean focusedArrow = focused != null && focused.getFocused() == focused.expander;
        rows.setFocused(null);

        List<CommandRow> visible = new ArrayList<>();
        if (snapshot != null) {
            for (IndexedEntry item : indexedEntries.values()) {
                if (indexedEntries.containsKey(item.entry().parentId())) {
                    continue;
                }
                addGroup(visible, item, key, !key.isEmpty(), 0, false);
            }
        }
        rows.updateEntries(visible);
        rows.setScrollAmount(scrollAmount);

        if (focused != null) {
            for (CommandRow row : visible) {
                if (row.item.entry().id().equals(focused.item.entry().id())) {
                    Button focus = focusedArrow ? row.expander : row.toggle;
                    if (focus != null && focus.active) {
                        rows.setFocused(row);
                        row.setFocused(focus);
                    }
                    break;
                }
            }
        }
    }

    private void addGroup(List<CommandRow> visible, IndexedEntry item, String key, boolean searching,
                          int depth, boolean inherited) {
        String id = item.entry().id();
        List<IndexedEntry> descendants = children.getOrDefault(id, List.of());
        boolean matches = CommandSearch.matches(item.searchKeys(), key);
        if (!matches && !hasMatchingChild(descendants, key)) {
            return;
        }

        boolean expanded = searching ? !collapsedSearchGroups.contains(id) : expandedGroups.contains(id);
        visible.add(new CommandRow(item, snapshot.editable(), depth, inherited, !descendants.isEmpty(), expanded));
        if (expanded) {
            String childKey = matches ? "" : key;
            boolean descendantsInherit = inherited || item.entry().available() && item.entry().enabled();
            for (IndexedEntry subcommand : descendants) {
                addGroup(visible, subcommand, childKey, searching, depth + 1, descendantsInherit);
            }
        }
    }

    private boolean hasMatchingChild(List<IndexedEntry> descendants, String key) {
        for (IndexedEntry child : descendants) {
            if (CommandSearch.matches(child.searchKeys(), key)
                    || hasMatchingChild(children.getOrDefault(child.entry().id(), List.of()), key)) {
                return true;
            }
        }
        return false;
    }

    private void toggleGroup(String id) {
        Set<String> changed = CommandSearch.normalize(query).isEmpty() ? expandedGroups : collapsedSearchGroups;
        if (!changed.add(id)) {
            changed.remove(id);
        }
        refreshRows(rows.getScrollAmount());
    }

    private record IndexedEntry(Entry entry, Component label, CommandSearch.Index searchKeys) {}

    private final class CommandRow extends ContainerObjectSelectionList.Entry<CommandRow> {
        private final IndexedEntry item;
        private final int depth;
        private final String commandNames;
        private final Button toggle;
        private final Button expander;
        private final List<Button> controls;

        private CommandRow(IndexedEntry item, boolean editable, int depth, boolean inherited,
                           boolean hasChildren, boolean expanded) {
            this.item = item;
            this.depth = depth;
            Entry entry = item.entry();
            List<String> names = new ArrayList<>();
            names.add(withSlash(entry.command()));
            for (String alias : entry.aliases()) {
                if (alias.startsWith("minecraft:")) {
                    String unqualified = alias.substring("minecraft:".length());
                    if (entry.command().equals(unqualified) || entry.aliases().contains(unqualified)) {
                        continue;
                    }
                }
                String commandName = withSlash(alias);
                if (!names.contains(commandName)) {
                    names.add(commandName);
                }
            }
            commandNames = String.join(" · ", names);

            boolean creativeLocked = "survivalcommands.command.gamemode.creative".equals(entry.labelKey())
                    && !entry.available();
            Component state;
            if (!entry.available()) {
                state = CommandTranslations.text(creativeLocked ? "survivalcommands.settings.locked"
                        : "survivalcommands.settings.unavailable");
            } else if (inherited && !entry.enabled()) {
                state = CommandTranslations.text("survivalcommands.settings.inherited");
            } else {
                state = entry.enabled() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
            }
            toggle = Button.builder(state, button -> CommandSettingsClient.requestChange(entry.id(), !entry.enabled()))
                    .size(76, 20).createNarration(defaultNarration ->
                            CommandTranslations.text("survivalcommands.settings.toggle_narration", item.label(), state))
                    .build();
            toggle.active = editable && entry.available();
            if (creativeLocked) {
                toggle.setTooltip(Tooltip.create(CommandTranslations.text("survivalcommands.settings.creative_locked")));
            } else if (!entry.available()) {
                toggle.setTooltip(Tooltip.create(CommandTranslations.text("survivalcommands.settings.unsupported")));
            } else if (inherited) {
                toggle.setTooltip(Tooltip.create(CommandTranslations.text("survivalcommands.settings.inherited_tip")));
            }

            if (hasChildren) {
                var description = CommandTranslations.text(expanded
                        ? "survivalcommands.settings.collapse" : "survivalcommands.settings.expand", item.label());
                expander = Button.builder(Component.literal(expanded ? "▼" : "▶"), button -> toggleGroup(entry.id()))
                        .size(18, 20).createNarration(defaultNarration -> description).build();
                expander.setTooltip(Tooltip.create(description));
            } else {
                expander = null;
            }
            controls = expander == null ? List.of(toggle) : List.of(toggle, expander);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return controls;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return controls;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                graphics.fill(x, y, x + rowWidth, y + rowHeight, 0x22FFFFFF);
            }

            int indent = Math.min(depth, 4) * 13;
            int textX = x + 5 + indent;
            int toggleOffset = 23;
            int toggleX = x + rowWidth - toggle.getWidth() - toggleOffset;
            int textWidth = Math.max(0, toggleX - textX - 5);
            if (depth > 0) {
                graphics.fill(textX - 10, y + 1, textX - 9, y + rowHeight, 0xFF5F5F5F);
            }
            graphics.drawString(font, font.plainSubstrByWidth(item.label().getString(), textWidth), textX, y + 3,
                    item.entry().available() ? 0xFFFFFF : 0xA0A0A0);
            graphics.drawString(font, font.plainSubstrByWidth(commandNames, textWidth), textX, y + 16, 0xAAAAAA);
            toggle.setPosition(toggleX, y + 4);
            toggle.render(graphics, mouseX, mouseY, partialTick);
            if (expander != null) {
                expander.setPosition(x + rowWidth - expander.getWidth() - 3, y + 4);
                expander.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        private static String withSlash(String name) {
            return name.startsWith("/") ? name : "/" + name;
        }
    }
}
