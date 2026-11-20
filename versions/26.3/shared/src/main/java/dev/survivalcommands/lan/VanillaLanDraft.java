package dev.survivalcommands.lan;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.server.MinecraftServer.MultiplayerScope;

import java.util.Properties;

/** Keeps LAN drafts across the vanilla screen's reinitialization and scope changes. */
public final class VanillaLanDraft {
    private Properties options;
    public boolean restrict = true;
    public String portText;

    public Properties restore(Properties fromDisk) {
        if (options == null) {
            options = fromDisk;
            restrict = LanDefaults.creativeRestriction(options);
            int port = LanDefaults.port(options, 0);
            portText = port == 0 ? "" : Integer.toString(port);
        }
        return options;
    }

    public boolean initialized() {
        return options != null;
    }

    public void remember(Properties current, String currentPortText) {
        options = current;
        if (currentPortText != null) {
            portText = currentPortText;
        }
    }

    public Properties values(MultiplayerScope scope, int port) {
        Properties values = new Properties();
        values.setProperty("scope", scope.name());
        values.setProperty("port", Integer.toString(port));
        values.setProperty("restrictCreative", Boolean.toString(restrict));
        return values;
    }

    public static MultiplayerScope scope(Properties values, MultiplayerScope actual) {
        MultiplayerScope fallback = actual == MultiplayerScope.OFF ? MultiplayerScope.LAN : actual;
        return LanDefaults.option(values, "scope", MultiplayerScope.class, fallback);
    }

    public static String applyLabel(MultiplayerScope chosen, MultiplayerScope actual) {
        if (chosen == MultiplayerScope.OFF) {
            return actual == MultiplayerScope.OFF ? "apply_world" : "close";
        }
        return actual == MultiplayerScope.OFF ? "open" : "update";
    }

    public static <T> void setSelection(HeaderAndFooterLayout layout, String key, T value) {
        layout.visitWidgets(widget -> setSelection(widget, key, value));
    }

    @SuppressWarnings("unchecked")
    private static <T> void setSelection(GuiEventListener widget, String key, T value) {
        if (widget instanceof ContainerEventHandler group) {
            group.children().forEach(child -> setSelection(child, key, value));
        }
        if (widget instanceof CycleButton<?> cycle && LanControls.hasLabel(cycle.getMessage(), key)) {
            ((CycleButton<T>) cycle).setValue(value);
        }
    }
}
