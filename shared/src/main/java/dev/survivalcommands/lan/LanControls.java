package dev.survivalcommands.lan;

import dev.survivalcommands.client.CommandTranslations;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.function.Consumer;

/** Common labels and widgets for the native and optional LAN screens. */
public final class LanControls {
    private LanControls() {}

    public static Component text(String option) {
        return CommandTranslations.text("survivalcommands.lan." + option);
    }

    public static CycleButton<Boolean> restriction(boolean selected, Consumer<Boolean> change) {
        return CycleButton.onOffBuilder(selected).withTooltip(value -> Tooltip.create(text("restrict_tip")))
                .create(0, 0, 200, 20, text("restrict"), (button, value) -> change.accept(value));
    }

    public static void saved(Button button, boolean success) {
        button.setTooltip(Tooltip.create(text(success ? "saved" : "save_error")));
        button.setMessage(text(success ? "saved_short" : "save_error_short"));
    }

    public static boolean hasLabel(Component label, String key) {
        if (label.getContents() instanceof TranslatableContents contents) {
            if (key.equals(contents.getKey())) {
                return true;
            }
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component component && hasLabel(component, key)) {
                    return true;
                }
            }
        }
        return label.getSiblings().stream().anyMatch(sibling -> hasLabel(sibling, key));
    }
}
