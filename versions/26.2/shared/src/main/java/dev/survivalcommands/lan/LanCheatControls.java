package dev.survivalcommands.lan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.MultiplayerOptionsScreen;
import net.minecraft.client.gui.screens.options.WorldOptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class LanCheatControls {
    public static final String MCWIFIPNP_SCREEN = "io.github.satxm.mcwifipnp.client.WorldOptionsScreenNew";
    private static final String ALLOW_COMMANDS = "selectWorld.allowCommands";
    private static final String GUEST_COMMANDS = "options.worldOptions.guest.command_access";

    private LanCheatControls() {
    }

    public static void disable(Screen screen, Minecraft minecraft) {
        if (!(screen instanceof WorldOptionsScreen || screen instanceof MultiplayerOptionsScreen || MCWIFIPNP_SCREEN.equals(screen.getClass().getName()))) {
            return;
        }

        var server = minecraft.getSingleplayerServer();
        if (!LanCheatPolicy.disablesCheats(server)) {
            return;
        }

        boolean allowCommands = !(screen instanceof MultiplayerOptionsScreen) && server.getWorldData().isAllowCommands();
        screen.children().forEach(widget -> disableWidget(widget, allowCommands));
    }

    private static void disableWidget(GuiEventListener widget, boolean allowCommands) {
        if (widget instanceof ContainerEventHandler container) {
            container.children().forEach(child -> disableWidget(child, allowCommands));
        }
        if (!(widget instanceof CycleButton<?> button) || !(button.getValue() instanceof Boolean)) {
            return;
        }

        Component label = button.getMessage();
        if (hasLabel(label, GUEST_COMMANDS)) {
            disableToggle(button, false);
        } else if (hasLabel(label, ALLOW_COMMANDS)) {
            disableToggle(button, allowCommands);
        }
    }

    @SuppressWarnings("unchecked")
    private static void disableToggle(CycleButton<?> button, boolean value) {
        ((CycleButton<Boolean>) button).setValue(value);
        button.active = false;
    }

    private static boolean hasLabel(Component label, String key) {
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

