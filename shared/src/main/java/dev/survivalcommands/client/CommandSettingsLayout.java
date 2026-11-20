package dev.survivalcommands.client;

import net.minecraft.client.gui.Font;

/** Dimensions in Minecraft's scaled GUI coordinates, shared across renderers. */
record CommandSettingsLayout(int width, int height) {
    static final int ROW_HEIGHT = 40;
    static final int LIST_TOP = 81;
    static final int STATUS_WIDTH = 94;
    static final int ARROW_WIDTH = 20;

    int rowWidth() {
        return Math.max(100, width - 2 * margin());
    }

    int left() {
        return (width - rowWidth()) / 2;
    }

    int bottom() {
        return Math.max(LIST_TOP + 1, height - 34);
    }

    int syntaxX(int x, int statusX) {
        return x + Math.max(1, (statusX - x) * 43 / 100);
    }

    boolean useColumns() {
        return rowWidth() >= 440;
    }

    private int margin() {
        return Math.max(14, Math.min(48, width / 16));
    }

    static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return width < font.width("…") ? "" : font.plainSubstrByWidth(text, width - font.width("…")) + "…";
    }
}
