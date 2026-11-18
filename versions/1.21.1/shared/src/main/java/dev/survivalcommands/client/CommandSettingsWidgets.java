package dev.survivalcommands.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;

final class CommandSettingsWidgets {
    private CommandSettingsWidgets() {}

    static void tickSearch(EditBox search) {
        // This version animates the search field cursor from the system clock.
    }

    static final class CommandList<E extends ContainerObjectSelectionList.Entry<E>>
            extends ContainerObjectSelectionList<E> {
        private final int rowWidth;

        CommandList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int rowWidth,
                    int rowHeight) {
            super(minecraft, width, bottom - top, top, rowHeight);
            this.rowWidth = rowWidth;
        }

        void updateEntries(List<E> entries) {
            replaceEntries(entries);
        }

        @Override
        public int getRowWidth() {
            return rowWidth;
        }

        @Override
        protected int getScrollbarPosition() {
            return (getWidth() + rowWidth) / 2 + 4;
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
            int left = (getWidth() - rowWidth) / 2 - 6;
            graphics.fill(left, getY(), getScrollbarPosition() + 8, getBottom(), 0x58000000);
        }

        @Override
        protected void renderListSeparators(GuiGraphics graphics) {}
    }
}
