package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanControls;
import dev.survivalcommands.lan.LanCreativeRestriction;
import dev.survivalcommands.lan.LanDefaults;
import dev.survivalcommands.lan.VanillaLanDraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.MultiplayerOptionsScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer.MultiplayerScope;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Properties;

@Mixin(MultiplayerOptionsScreen.class)
abstract class MultiplayerOptionsScreenMixin {
    @Shadow @Final private HeaderAndFooterLayout layout;
    @Shadow private Button applyChanges;
    @Shadow private int port;
    @Shadow private boolean portValid;
    @Shadow private EditBox portEdit;
    @Shadow private MultiplayerScope wantedMultiplayerScope;
    @Shadow private MultiplayerScope initialMultiplayerScope;
    @Shadow private GameType gameMode;

    @Shadow private void updatePortControlsState() {}
    @Shadow private void updateApplyChangesActiveState() {}

    @Unique private final VanillaLanDraft survivalCommands$draft = new VanillaLanDraft();
    @Unique private Button survivalCommands$save;
    @Unique private net.minecraft.client.gui.components.CycleButton<Boolean> survivalCommands$restrictionButton;

    // ---------- Draft lifecycle ----------

    @Inject(method = "init", at = @At("HEAD"))
    private void survivalCommands$rememberDraft(CallbackInfo callback) {
        survivalCommands$save = null;
        if (survivalCommands$draft.initialized() && portEdit != null) {
            survivalCommands$draft.remember(survivalCommands$currentOptions(), portEdit.active ? portEdit.getValue() : null);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void survivalCommands$restoreDraft(CallbackInfo callback) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null || portEdit == null) {
            return;
        }
        Properties defaults = survivalCommands$draft.restore(survivalCommands$draft.initialized()
                ? null : LanDefaults.read(server, false));
        wantedMultiplayerScope = VanillaLanDraft.scope(defaults, initialMultiplayerScope);
        VanillaLanDraft.setSelection(layout, "menu.multiplayerOptions.lan", wantedMultiplayerScope == MultiplayerScope.LAN);
        survivalCommands$restrictionButton.setValue(survivalCommands$draft.restrict);
        updatePortControlsState();
        gameMode = LanDefaults.option(defaults, "gameMode", GameType.class, gameMode);
        VanillaLanDraft.setSelection(layout, "lanServer.gameMode", gameMode);
        updateApplyChangesActiveState();
    }

    @Inject(method = "updatePortControlsState", at = @At("HEAD"))
    private void survivalCommands$rememberPort(CallbackInfo callback) {
        if (survivalCommands$draft.initialized() && portEdit != null && portEdit.active
                && wantedMultiplayerScope == MultiplayerScope.OFF) {
            survivalCommands$draft.portText = portEdit.getValue();
        }
    }

    @Inject(method = "updatePortControlsState", at = @At("TAIL"))
    private void survivalCommands$restorePort(CallbackInfo callback) {
        if (survivalCommands$draft.initialized() && portEdit != null && wantedMultiplayerScope == MultiplayerScope.LAN) {
            portEdit.setValue(survivalCommands$draft.portText);
        }
    }

    // ---------- Footer actions ----------

    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;"))
    private Button.Builder survivalCommands$actions(Component label, Button.OnPress vanilla) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return Button.builder(label, vanilla);
        }
        if (label.equals(CommonComponents.GUI_CANCEL)) {
            return Button.builder(LanControls.text("save"), button ->
                    LanControls.saved(button, LanDefaults.save(server, false, survivalCommands$currentOptions())));
        }
        return Button.builder(label, button -> {
            LanCreativeRestriction.configure(server, survivalCommands$draft.restrict);
            vanilla.onPress(button);
        });
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private net.minecraft.client.gui.layouts.LayoutElement survivalCommands$addRestriction(
            HeaderAndFooterLayout target, net.minecraft.client.gui.layouts.LayoutElement content) {
        if (content instanceof LinearLayout column) {
            column.addChild(survivalCommands$restriction());
        }
        return target.addToContents(content);
    }

    @Unique
    private net.minecraft.client.gui.components.CycleButton<Boolean> survivalCommands$restriction() {
        survivalCommands$restrictionButton = LanControls.restriction(survivalCommands$draft.restrict, value -> {
            survivalCommands$draft.restrict = value;
            updateApplyChangesActiveState();
        });
        return survivalCommands$restrictionButton;
    }

    @Inject(method = "updateApplyChangesActiveState", at = @At("TAIL"))
    private void survivalCommands$updateActions(CallbackInfo callback) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (!survivalCommands$draft.initialized() || applyChanges == null || server == null) {
            return;
        }
        applyChanges.setMessage(LanControls.text(VanillaLanDraft.applyLabel(wantedMultiplayerScope, initialMultiplayerScope)));
        if (wantedMultiplayerScope == MultiplayerScope.LAN) {
            applyChanges.active = portValid;
        }
        if (survivalCommands$save == null) {
            layout.visitWidgets(widget -> {
                if (widget instanceof Button button && LanControls.hasLabel(button.getMessage(), "survivalcommands.lan.save")) {
                    survivalCommands$save = button;
                }
            });
        }
        if (survivalCommands$save != null) {
            survivalCommands$save.active = wantedMultiplayerScope != MultiplayerScope.LAN || portValid;
        }
    }

    @Unique
    private Properties survivalCommands$currentOptions() {
        Properties values = survivalCommands$draft.values(wantedMultiplayerScope, port);
        values.setProperty("gameMode", gameMode.name());
        return values;
    }
}
