package dev.survivalcommands.client;

import dev.survivalcommands.client.CommandSettingsClient.Snapshot;
import dev.survivalcommands.client.CommandSettingsWidgets.CommandList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CommandSettingsScreen extends Screen {
    private final Screen parent;
    private final Set<String> expanded = new HashSet<>();
    private final Set<String> collapsedSearch = new HashSet<>();
    private Snapshot snapshot;
    private CommandTree tree;
    private String query = "";
    private CommandSettingsLayout layout;
    private CommandList<CommandSettingsRow> rows;
    private EditBox search;

    public CommandSettingsScreen(Screen parent) {
        super(CommandTranslations.text("survivalcommands.settings.title"));
        this.parent = parent;
    }

    // ---------- Screen layout and navigation ----------

    @Override
    protected void init() {
        layout = new CommandSettingsLayout(width, height);
        double scroll = rows == null ? 0.0 : rows.scrollAmount();
        rows = addRenderableWidget(new CommandList<CommandSettingsRow>(minecraft, width, height,
                CommandSettingsLayout.LIST_TOP, layout.bottom(), layout.rowWidth(), CommandSettingsLayout.ROW_HEIGHT));

        Component searchLabel = CommandTranslations.text("survivalcommands.settings.search");
        int labelWidth = font.width(CommandTranslations.text("survivalcommands.settings.search_label")) + 10;
        search = addRenderableWidget(new EditBox(font, layout.left() + labelWidth, 26,
                Math.max(48, layout.rowWidth() - labelWidth), 20, searchLabel));
        search.setMaxLength(100);
        search.setHint(searchLabel);
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            collapsedSearch.clear();
            showRows(0.0);
        });

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - 120) / 2, height - 26, 120, 20).build());
        setInitialFocus(search);
        refreshSnapshot(CommandSettingsClient.snapshot(), scroll);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xE0101010, 0xEE101010);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, title, layout.left(), 11, 0xFFFFFFFF);
        graphics.text(font, CommandTranslations.text("survivalcommands.settings.search_label"),
                layout.left(), 32, 0xFFF1F1F1);
        String status = snapshot == null ? "waiting" : snapshot.editable() ? "shared" : "read_only";
        graphics.text(font, CommandTranslations.text("survivalcommands.settings." + status),
                layout.left(), 52, 0xFFC5C5C5);
        int statusX = layout.left() + layout.rowWidth() - CommandSettingsLayout.STATUS_WIDTH - 4;
        graphics.text(font, CommandTranslations.text("survivalcommands.settings.column.command"),
                layout.left() + CommandSettingsLayout.ARROW_WIDTH + 8, 68, 0xFFB9B9B9);
        if (layout.useColumns()) {
            graphics.text(font, CommandTranslations.text("survivalcommands.settings.column.syntax"),
                    layout.syntaxX(layout.left(), statusX), 68, 0xFFB9B9B9);
        }
        graphics.text(font, CommandTranslations.text("survivalcommands.settings.column.access"),
                statusX + 4, 68, 0xFFB9B9B9);
        if (snapshot != null && rows.children().isEmpty()) {
            int y = (CommandSettingsLayout.LIST_TOP + layout.bottom()) / 2;
            graphics.centeredText(font, CommandTranslations.text("survivalcommands.settings.empty"),
                    width / 2, y, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------- Live snapshots and view state ----------

    @Override
    public void tick() {
        CommandSettingsWidgets.tickSearch(search);
        Snapshot latest = CommandSettingsClient.snapshot();
        if (!Objects.equals(latest, snapshot) && !rows.isDragging()) {
            refreshSnapshot(latest, rows.scrollAmount());
        }
    }

    private void refreshSnapshot(Snapshot latest, double scroll) {
        snapshot = latest;
        tree = new CommandTree(latest == null ? List.of() : latest.commands());
        showRows(scroll);
    }

    private void showRows(double scroll) {
        CommandSettingsRow focus = rows.getFocused();
        boolean focusArrow = focus != null && focus.getFocused() == focus.expander;
        List<CommandTree.Row> visible = tree.visible(query, expanded, collapsedSearch);
        if (rows.children().stream().map(row -> row.item).toList().equals(visible)
                && (rows.children().isEmpty() || rows.children().get(0).editable == snapshot.editable())) {
            return;
        }

        rows.setFocused(null);
        boolean editable = snapshot != null && snapshot.editable();
        List<CommandSettingsRow> entries = visible.stream().map(item ->
                new CommandSettingsRow(item, editable, font, layout, this::toggleGroup)).toList();
        rows.updateEntries(entries);

        if (focus != null) {
            for (CommandSettingsRow row : entries) {
                if (row.item.entry().id().equals(focus.item.entry().id())) {
                    Button button = focusArrow ? row.expander : row.toggle;
                    if (button != null && button.active) {
                        rows.setFocused(row);
                        row.setFocused(button);
                    }
                    break;
                }
            }
        }
        rows.setScrollAmount(scroll);
    }

    private void toggleGroup(String id) {
        Set<String> changed = CommandSearch.normalize(query).isEmpty() ? expanded : collapsedSearch;
        if (!changed.add(id)) {
            changed.remove(id);
        }
        showRows(rows.scrollAmount());
    }
}
