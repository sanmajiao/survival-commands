package dev.survivalcommands.mixin.client;

import dev.survivalcommands.client.CommandSettingsClient;
import dev.survivalcommands.client.CommandSettingsScreen;
import dev.survivalcommands.client.CommandTranslations;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = PauseScreen.class, priority = 800)
public abstract class PauseScreenMixin extends Screen {
    @Shadow @Final private boolean showPauseMenu;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void survivalcommands$addSettings(CallbackInfo callback) {
        if (!showPauseMenu || minecraft == null || (minecraft.getSingleplayerServer() == null
                && CommandSettingsClient.snapshot() == null)) {
            return;
        }

        Component label = CommandTranslations.text("survivalcommands.settings.title");
        int buttonWidth = Math.min(width - 12, Math.max(146, font.width(label) + 16));
        int centerX = (width - buttonWidth) / 2;
        List<AbstractWidget> widgets = children().stream().filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast).filter(widget -> widget.visible).toList();
        int bottom = widgets.stream().filter(widget -> widget.getX() < centerX + buttonWidth
                        && widget.getX() + widget.getWidth() > centerX)
                .mapToInt(widget -> widget.getY() + widget.getHeight()).max().orElse(50);
        int rightX = width - buttonWidth - 6;
        int[][] candidates = {{centerX, bottom + 5}, {rightX, height - 26}, {6, height - 26},
                {rightX, 6}, {6, 6}, {centerX, 6}};

        for (int[] candidate : candidates) {
            int x = candidate[0];
            int y = candidate[1];
            if (survivalcommands$fits(widgets, x, y, buttonWidth)) {
                addRenderableWidget(Button.builder(label, button ->
                                minecraft.setScreen(new CommandSettingsScreen(this)))
                        .bounds(x, y, buttonWidth, 20).build());
                return;
            }
        }
    }

    @Unique
    private boolean survivalcommands$fits(List<AbstractWidget> widgets, int x, int y, int buttonWidth) {
        if (x < 0 || x + buttonWidth > width || y < 0 || y + 20 > height) {
            return false;
        }
        return widgets.stream().noneMatch(widget -> x < widget.getX() + widget.getWidth() + 2
                && x + buttonWidth + 2 > widget.getX() && y < widget.getY() + widget.getHeight() + 2
                && y + 22 > widget.getY());
    }
}
