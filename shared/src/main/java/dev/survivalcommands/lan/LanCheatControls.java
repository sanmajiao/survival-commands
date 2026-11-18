package dev.survivalcommands.lan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Set;

/** Reuses the existing screen and widgets, so port and other LAN options stay available. */
public final class LanCheatControls {
    private static final Set<String> OTHER_LAN_SCREENS = Set.of(
        "io.github.satxm.mcwifipnp.ShareToLanScreenNew",
        "io.github.satxm.mcwifipnp.client.ShareToLanScreenNew",
        "rikka.lanserverproperties.OpenToLanScreenEx",
        "rikka.lanserverproperties.ModifyLanScreen"
    );
    private static final Set<String> CHEAT_LABELS = Set.of(
        "selectWorld.allowCommands",
        "mcwifipnp.gui.AllPlayersCheats"
    );

    private LanCheatControls() {
    }

    public static void disable(Screen screen, Minecraft minecraft) {
        if (!shouldDisableControls(screen, minecraft)) {
            return;
        }

        screen.children().forEach(LanCheatControls::disableIfCheatControl);
    }

    public static void disableWidget(Screen screen, Minecraft minecraft, Object widget) {
        if (shouldDisableControls(screen, minecraft)) {
            disableIfCheatControl(widget);
        }
    }

    private static boolean shouldDisableControls(Screen screen, Minecraft minecraft) {
        return (screen instanceof ShareToLanScreen || OTHER_LAN_SCREENS.contains(screen.getClass().getName()))
            && LanCheatPolicy.disablesCheats(minecraft.getSingleplayerServer());
    }

    private static void disableIfCheatControl(Object child) {
        if (child instanceof AbstractWidget widget && isCheatLabel(widget.getMessage())) {
            if (widget instanceof CycleButton<?> toggle && toggle.getValue() instanceof Boolean) {
                turnOff(toggle);
            }
            widget.active = false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void turnOff(CycleButton<?> toggle) {
        // setValue updates the display without calling the other mod's click handler.
        ((CycleButton<Boolean>) toggle).setValue(false);
    }

    private static boolean isCheatLabel(Component label) {
        if (label.getContents() instanceof TranslatableContents contents) {
            if (CHEAT_LABELS.contains(contents.getKey())) {
                return true;
            }
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component component && isCheatLabel(component)) {
                    return true;
                }
            }
        }
        return label.getSiblings().stream().anyMatch(LanCheatControls::isCheatLabel);
    }
}
