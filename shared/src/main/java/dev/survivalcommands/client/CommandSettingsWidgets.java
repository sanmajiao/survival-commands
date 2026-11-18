package dev.survivalcommands.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;

final class CommandSettingsWidgets {
    private CommandSettingsWidgets() {}

    static void tickSearch(EditBox search) {
        search.tick();
    }

    static final class CommandList<E extends ContainerObjectSelectionList.Entry<E>>
            extends ContainerObjectSelectionList<E> {
        private final int rowWidth;

        CommandList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int rowWidth,
                    int rowHeight) {
            super(minecraft, width, screenHeight, top, bottom, rowHeight);
            this.rowWidth = rowWidth;
            setRenderBackground(false);
            setRenderTopAndBottom(false);
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
            return (width + rowWidth) / 2 + 4;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int left = (width - rowWidth) / 2 - 6;
            int right = getScrollbarPosition() + 8;
            graphics.fill(left, y0, right, y1, 0x58000000);
            graphics.enableScissor(left, y0, right, y1);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        }
    }
}
