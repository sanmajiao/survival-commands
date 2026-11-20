package dev.survivalcommands.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

final class CommandSettingsRow extends ContainerObjectSelectionList.Entry<CommandSettingsRow> {
    final CommandTree.Row item;
    final boolean editable;
    final Button toggle;
    final Button expander;
    private final Font font;
    private final CommandSettingsLayout layout;
    private final List<Button> controls;

    CommandSettingsRow(CommandTree.Row item, boolean editable, Font font, CommandSettingsLayout layout,
                       Consumer<String> toggleGroup) {
        this.item = item;
        this.editable = editable;
        this.font = font;
        this.layout = layout;

        // ---------- State, actions and accessible labels ----------

        Component state = switch (item.state()) {
            case ON -> CommonComponents.OPTION_ON;
            case OFF -> CommonComponents.OPTION_OFF;
            case EXTERNAL -> label("external");
            case INHERITED -> label("inherited");
            case PARTIAL -> label("partial");
            case LOCKED -> label("locked");
            case UNAVAILABLE -> label("unavailable");
        };
        var entry = item.entry();
        toggle = Button.builder(state, button -> CommandSettingsClient.requestChange(entry.id(), !entry.enabled()))
                .size(CommandSettingsLayout.STATUS_WIDTH, 20).createNarration(ignored ->
                        CommandTranslations.text("survivalcommands.settings.toggle_narration", item.label(), state))
                .build();
        toggle.active = editable && entry.available() && item.state() != CommandTree.State.EXTERNAL;
        Component detail = switch (item.state()) {
            case LOCKED -> label("creative_locked");
            case UNAVAILABLE -> label("unsupported");
            case EXTERNAL -> label("external_tip");
            case INHERITED -> label("inherited_tip");
            case PARTIAL -> label("partial_tip");
            default -> Component.empty();
        };
        Component fullText = item.label().copy().append("\n" + item.commands());
        if (!detail.getString().isEmpty()) {
            fullText = fullText.copy().append("\n").append(detail);
        }
        toggle.setTooltip(Tooltip.create(fullText));

        if (item.hasChildren()) {
            var description = CommandTranslations.text(item.expanded()
                    ? "survivalcommands.settings.collapse" : "survivalcommands.settings.expand", item.label());
            expander = Button.builder(Component.literal(item.expanded() ? "▼" : "▶"),
                            button -> toggleGroup.accept(entry.id()))
                    .size(CommandSettingsLayout.ARROW_WIDTH, 20).createNarration(ignored -> description).build();
            expander.setTooltip(Tooltip.create(description));
        } else {
            expander = null;
        }
        controls = expander == null ? List.of(toggle) : List.of(expander, toggle);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return controls;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return controls;
    }

    // ---------- Row geometry ----------

    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        int x = getX();
        int y = getY();
        int rowWidth = getWidth();
        int rowHeight = getHeight();
        graphics.fill(x, y, x + rowWidth, y + rowHeight, hovered ? 0x22FFFFFF : 0x0AFFFFFF);
        graphics.fill(x, y + rowHeight - 1, x + rowWidth, y + rowHeight, 0x30FFFFFF);
        int indent = Math.min(item.depth(), layout.useColumns() ? 7 : 4) * 8;
        int textX = x + CommandSettingsLayout.ARROW_WIDTH + 8 + indent;
        int statusX = x + rowWidth - toggle.getWidth() - 4;
        int syntaxX = layout.useColumns() ? layout.syntaxX(x, statusX) : textX;
        int centerY = y + (rowHeight - font.lineHeight) / 2;
        int labelY = layout.useColumns() ? centerY : centerY - 7;
        int syntaxY = layout.useColumns() ? centerY : centerY + 7;
        int nameWidth = (layout.useColumns() ? syntaxX : statusX) - textX - 8;
        int commandWidth = statusX - syntaxX - 8;
        int color = !item.entry().available() ? 0xFF969696 : item.state().enabled ? 0xFF91E89D : 0xFFF1F1F1;
        graphics.text(font, CommandSettingsLayout.fit(font, item.label().getString(), nameWidth), textX, labelY, color);
        graphics.text(font, CommandSettingsLayout.fit(font, item.commands(), commandWidth), syntaxX, syntaxY, color);

        int buttonY = y + (rowHeight - 20) / 2;
        if (expander != null) {
            expander.setPosition(x + 2, buttonY);
            expander.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        toggle.setPosition(statusX, buttonY);
        toggle.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static Component label(String suffix) {
        return CommandTranslations.text("survivalcommands.settings." + suffix);
    }
}
