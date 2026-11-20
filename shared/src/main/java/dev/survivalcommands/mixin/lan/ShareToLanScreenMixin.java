package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanControls;
import dev.survivalcommands.lan.LanCreativeRestriction;
import dev.survivalcommands.lan.LanDefaults;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Properties;

@Mixin(ShareToLanScreen.class)
abstract class ShareToLanScreenMixin extends Screen {
    @Shadow private GameType gameMode;
    @Shadow private int port;
    @Shadow private EditBox portEdit;

    @Unique private Properties survivalCommands$defaults;
    @Unique private boolean survivalCommands$restrictCreative;
    @Unique private String survivalCommands$portDraft;
    @Unique private Button survivalCommands$open;

    protected ShareToLanScreenMixin(Component title) {
        super(title);
    }

    // ---------- Read once. Subsequent initializations retain the current draft. ----------

    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/server/IntegratedServer;getDefaultGameType()Lnet/minecraft/world/level/GameType;"))
    private GameType survivalCommands$restoreOptions(IntegratedServer server) {
        if (survivalCommands$defaults != null) {
            survivalCommands$portDraft = portEdit.getValue();
            return gameMode;
        }
        survivalCommands$defaults = LanDefaults.read(server, false);
        survivalCommands$restrictCreative = LanDefaults.creativeRestriction(survivalCommands$defaults);
        int savedPort = LanDefaults.port(survivalCommands$defaults, 0);
        survivalCommands$portDraft = savedPort == 0 ? "" : Integer.toString(savedPort);
        return LanDefaults.option(survivalCommands$defaults, "gameMode", GameType.class, server.getDefaultGameType());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void survivalCommands$addRestriction(CallbackInfo callback) {
        for (var widget : children()) {
            if (widget instanceof Button button) {
                if (LanControls.hasLabel(button.getMessage(), "survivalcommands.lan.open")) {
                    survivalCommands$open = button;
                }
            }
        }
        var restriction = LanControls.restriction(survivalCommands$restrictCreative,
                value -> survivalCommands$restrictCreative = value);
        restriction.setPosition((width - restriction.getWidth()) / 2, 130);
        addRenderableWidget(restriction);
        portEdit.setValue(survivalCommands$portDraft);
    }

    // ---------- Wrap the existing publish action and replace the redundant Cancel button ----------

    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;"))
    private Button.Builder survivalCommands$actions(Component label, Button.OnPress vanilla) {
        if (label.equals(CommonComponents.GUI_CANCEL)) {
            return Button.builder(LanControls.text("save"), button -> {
                var server = minecraft.getSingleplayerServer();
                if (server == null || survivalCommands$open == null || !survivalCommands$open.active) {
                    button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(LanControls.text("port_unavailable")));
                    return;
                }
                Properties values = new Properties();
                values.setProperty("port", Integer.toString(port));
                values.setProperty("gameMode", gameMode.name());
                values.setProperty("restrictCreative", Boolean.toString(survivalCommands$restrictCreative));
                LanControls.saved(button, LanDefaults.save(server, false, values));
            });
        }
        return Button.builder(LanControls.text("open"), button -> {
            var server = minecraft.getSingleplayerServer();
            if (server != null) {
                LanCreativeRestriction.configure(server, survivalCommands$restrictCreative);
                vanilla.onPress(button);
            }
        });
    }

}
