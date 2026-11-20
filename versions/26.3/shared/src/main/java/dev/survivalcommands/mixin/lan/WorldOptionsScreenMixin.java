package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanCheatControls;
import dev.survivalcommands.lan.LanControls;
import dev.survivalcommands.lan.LanCreativeRestriction;
import dev.survivalcommands.lan.LanDefaults;
import dev.survivalcommands.lan.VanillaLanDraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer.MultiplayerScope;
import net.minecraft.world.Difficulty;
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

@Mixin(WorldOptionsScreen.class)
abstract class WorldOptionsScreenMixin {
    @Shadow @Final private HeaderAndFooterLayout layout;
    @Shadow private Button applyChanges;
    @Shadow private int port;
    @Shadow private boolean portValid;
    @Shadow private EditBox portEdit;
    @Shadow private MultiplayerScope wantedMultiplayerScope;
    @Shadow private MultiplayerScope initialMultiplayerScope;
    @Shadow private GameType wantedDefaultGameMode;
    @Shadow private GameType wantedPersonalGameMode;
    @Shadow private CycleButton<GameType> defaultGameModeButton;
    @Shadow private CycleButton<GameType> personalGameModeButton;
    @Shadow private Difficulty wantedDifficulty;
    @Shadow private Boolean wantedDifficultyLocked;
    @Shadow private Boolean wantedForceGameMode;
    @Shadow private CycleButton<Boolean> forceGameModeButton;

    @Shadow private void updateGuestCommandAccessButton(IntegratedServer server) {}

    @Shadow private void updatePortControlsState() {}
    @Shadow private void updateApplyChangesActiveState() {}

    @Unique private final VanillaLanDraft survivalCommands$draft = new VanillaLanDraft();
    @Unique private Button survivalCommands$save;
    @Unique private net.minecraft.client.gui.components.CycleButton<Boolean> survivalCommands$restrictionButton;
    @Unique private Boolean survivalCommands$lockDraft;

    // ---------- Draft lifecycle ----------

    @Inject(method = "init", at = @At("HEAD"))
    private void survivalCommands$rememberDraft(CallbackInfo callback) {
        survivalCommands$save = null;
        if (survivalCommands$draft.initialized() && portEdit != null) {
            survivalCommands$draft.remember(survivalCommands$currentOptions(), portEdit.active ? portEdit.getValue() : null);
            survivalCommands$lockDraft = wantedDifficultyLocked;
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
        updateGuestCommandAccessButton(server);
        if (defaultGameModeButton != null) {
            wantedDefaultGameMode = LanDefaults.option(defaults, "gameMode", GameType.class, wantedDefaultGameMode);
            defaultGameModeButton.setValue(wantedDefaultGameMode);
        }
        if (personalGameModeButton != null) {
            wantedPersonalGameMode = LanDefaults.option(defaults, "personalGameMode", GameType.class, wantedPersonalGameMode);
            personalGameModeButton.setValue(wantedPersonalGameMode);
        }
        if (!server.isHardcore()) {
            wantedDifficulty = LanDefaults.option(defaults, "difficulty", Difficulty.class, wantedDifficulty);
            if (wantedDifficulty != null) {
                VanillaLanDraft.setSelection(layout, "options.difficulty", wantedDifficulty);
            }
        }
        if (Boolean.TRUE.equals(survivalCommands$lockDraft)) {
            wantedDifficultyLocked = true;
        }
        if (forceGameModeButton != null && forceGameModeButton.active && defaults.containsKey("joinMode")) {
            wantedForceGameMode = Boolean.parseBoolean(defaults.getProperty("joinMode"));
            forceGameModeButton.setValue(wantedForceGameMode);
        }
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
            vanilla.onPress(button);
        });
    }

    @Inject(method = "multiplayerOptions", at = @At("TAIL"))
    private void survivalCommands$addRestriction(LinearLayout content, IntegratedServer server, CallbackInfo callback) {
        content.addChild(survivalCommands$restriction());
    }

    @Inject(method = "applyChanges", at = @At("HEAD"))
    private void survivalCommands$applyRestriction(IntegratedServer server, CallbackInfo callback) {
        LanCreativeRestriction.configure(server, survivalCommands$draft.restrict);
    }

    // Vanilla registers additional widgets after re-enabling the guest control.
    @Inject(method = "updateGuestCommandAccessButton", at = @At("TAIL"))
    private void survivalCommands$keepGuestCheatsDisabled(IntegratedServer server, CallbackInfo callback) {
        LanCheatControls.disable((WorldOptionsScreen) (Object) this, Minecraft.getInstance());
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
        if (wantedDefaultGameMode != null) {
            values.setProperty("gameMode", wantedDefaultGameMode.name());
        }
        if (wantedPersonalGameMode != null) {
            values.setProperty("personalGameMode", wantedPersonalGameMode.name());
        }
        if (wantedDifficulty != null) {
            values.setProperty("difficulty", wantedDifficulty.name());
        }
        if (wantedForceGameMode != null) {
            values.setProperty("joinMode", wantedForceGameMode.toString());
        }
        return values;
    }
}
