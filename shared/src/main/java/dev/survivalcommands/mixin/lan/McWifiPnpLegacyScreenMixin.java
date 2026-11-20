package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanControls;
import dev.survivalcommands.lan.LanCreativeRestriction;
import dev.survivalcommands.lan.McWifiPnpAdapter;
import dev.survivalcommands.lan.McWifiPnpLegacyOptions;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.satxm.mcwifipnp.ShareToLanScreenNew", remap = false)
abstract class McWifiPnpLegacyScreenMixin extends Screen {
    @Shadow private EditBox EditPort;
    @Shadow private EditBox EditPlayers;
    @Shadow private EditBox EditMotd;

    @Unique private boolean survivalCommands$restrict = true;
    @Unique private String survivalCommands$portText = "";
    @Unique private String survivalCommands$playersText = "";
    @Unique private String survivalCommands$motdText = "";

    protected McWifiPnpLegacyScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void survivalCommands$loadRestriction(CallbackInfo callback) {
        var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            survivalCommands$restrict = McWifiPnpLegacyOptions.restriction(server);
        }
    }

    // ---------- Keep text entered before a resize ----------

    @Inject(method = {"init", "method_25426", "m_7856_"}, at = @At("HEAD"), require = 0)
    private void survivalCommands$rememberText(CallbackInfo callback) {
        if (EditPort != null && EditPlayers != null && EditMotd != null) {
            survivalCommands$portText = EditPort.getValue();
            survivalCommands$playersText = EditPlayers.getValue();
            survivalCommands$motdText = EditMotd.getValue();
        }
    }

    @Inject(method = {"init", "method_25426", "m_7856_"}, at = @At("TAIL"), require = 0)
    private void survivalCommands$addRestriction(CallbackInfo callback) {
        var toggle = LanControls.restriction(survivalCommands$restrict, value -> survivalCommands$restrict = value);
        toggle.setPosition((width - toggle.getWidth()) / 2, 97);
        addRenderableWidget(toggle);
        EditPort.setValue(survivalCommands$portText);
        EditPlayers.setValue(survivalCommands$playersText);
        EditMotd.setValue(survivalCommands$motdText);
    }

    // ---------- Keep mcwifipnp's existing publish and config implementations ----------

    @Redirect(method = {"init", "method_25426", "m_7856_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;", remap = true),
            require = 0, remap = true)
    private Button.Builder survivalCommands$actions(Component label, Button.OnPress open) {
        if (label.equals(CommonComponents.GUI_CANCEL)) {
            return Button.builder(LanControls.text("save"), button -> {
                var server = minecraft.getSingleplayerServer();
                if (server != null && survivalCommands$validFields()) {
                    boolean saved = McWifiPnpAdapter.saveLegacyOptions(server, EditPort.getValue(),
                            EditPlayers.getValue(), EditMotd.getValue());
                    LanControls.saved(button, saved && McWifiPnpLegacyOptions.saveRestriction(
                            server, survivalCommands$restrict));
                }
            });
        }
        return Button.builder(LanControls.text("open"), button -> {
            var server = minecraft.getSingleplayerServer();
            if (server != null && survivalCommands$validFields()) {
                LanCreativeRestriction.configure(server, survivalCommands$restrict);
                open.onPress(button);
            }
        });
    }

    @Redirect(method = "lambda$init$*", at = @At(value = "INVOKE",
            target = "Lio/github/satxm/mcwifipnp/MCWiFiPnPUnit;saveConfig(Lio/github/satxm/mcwifipnp/MCWiFiPnPUnit$Config;)V"),
            require = 0)
    private void survivalCommands$startWithoutSaving(@Coerce Object config) {}

    @Unique
    private boolean survivalCommands$validFields() {
        try {
            String portText = EditPort.getValue();
            String playersText = EditPlayers.getValue();
            if (!portText.isEmpty()) {
                int port = Integer.parseInt(portText);
                if (port < 1024 || port > 65535 || !HttpUtil.isPortAvailable(port)) {
                    return false;
                }
            }
            return playersText.isEmpty() || Integer.parseInt(playersText) >= 0;
        } catch (NumberFormatException error) {
            return false;
        }
    }
}
