package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanCheatControls;
import dev.survivalcommands.lan.LanControls;
import dev.survivalcommands.lan.LanCreativeRestriction;
import dev.survivalcommands.lan.McWifiPnpSession;
import dev.survivalcommands.lan.McWifiPnpSession.Config;
import dev.survivalcommands.lan.VanillaLanDraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer.MultiplayerScope;
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
@Mixin(targets = LanCheatControls.MCWIFIPNP_SCREEN, remap = false)
abstract class McWifiPnpLanScreenMixin {
    @Shadow private EditBox portEdit;
    @Shadow private int initialPort;
    @Shadow private String initialMotd;
    @Shadow private boolean initialUseUPnP;
    @Shadow private boolean initialGetPublicIP;

    @Unique private Config survivalCommands$config;
    @Unique private Button survivalCommands$open;
    @Unique private boolean survivalCommands$restrictCreative = true;

    // ---------- Use mcwifipnp's existing default storage and working configuration ----------

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lio/github/satxm/mcwifipnp/Config;readFromRunningServer(Lnet/minecraft/server/MinecraftServer;)V"),
            require = 0)
    private void survivalCommands$preferSavedDefaults(@Coerce Object config, MinecraftServer server) {
        if (config instanceof Config defaults && defaults.survivalcommands$usesDefaults()) {
            defaults.survivalcommands$readRunning(server);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void survivalCommands$loadRestriction(CallbackInfo callback) {
        Config config = survivalCommands$findConfig();
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (config == null || server == null) {
            return;
        }
        survivalCommands$config = config;
        survivalCommands$restrictCreative = config.survivalcommands$restrictCreative();
        if (server.isPublished()) {
            initialPort = server.getPort();
            initialMotd = server.getMotd();
            if (config.survivalcommands$usesDefaults()) {
                config.survivalcommands$scope(server.getMultiplayerScope());
            }
            var active = McWifiPnpSession.options(server);
            if (active != null) {
                initialUseUPnP = active.upnp();
                initialGetPublicIP = active.copyPublicAddress();
            }
        } else if (config.survivalcommands$usesDefaults()) {
            config.survivalcommands$scope(MultiplayerScope.LAN);
        }
    }

    @Inject(method = "multiplayerOptions", at = @At("TAIL"), require = 0)
    private void survivalCommands$addRestriction(LinearLayout content, IntegratedServer server, CallbackInfo callback) {
        if (survivalCommands$config != null) {
            content.addChild(LanControls.restriction(survivalCommands$restrictCreative,
                    value -> survivalCommands$restrictCreative = value));
        }
    }

    // ---------- The footer saves or applies the current draft ----------

    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;"),
            require = 0)
    private Button.Builder survivalCommands$actions(Component label, Button.OnPress original) {
        Config config = survivalCommands$config;
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (config == null || server == null) {
            return Button.builder(label, original);
        }
        if (LanControls.hasLabel(label, "mcwifipnp.gui.applyGeneralChanges")) {
            return Button.builder(LanControls.text("save"), button -> {
                if (survivalCommands$validPort(server, config, button)) {
                    config.survivalcommands$restrictCreative(survivalCommands$restrictCreative);
                    config.survivalcommands$preserveCheats(server.getWorldData().isAllowCommands());
                    LanControls.saved(button, config.survivalcommands$saveDefaults(server));
                }
            });
        }
        if (LanControls.hasLabel(label, "menu.multiplayerOptions.applyChanges")) {
            return Button.builder(LanControls.text(survivalCommands$actionLabel(server)), button -> {
                if (survivalCommands$validPort(server, config, button)) {
                    original.onPress(button);
                }
            });
        }
        return Button.builder(label, original);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void survivalCommands$rememberOpenButton(CallbackInfo callback) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (survivalCommands$config != null && server != null) {
            String key = "survivalcommands.lan." + survivalCommands$actionLabel(server);
            survivalCommands$open = Minecraft.getInstance().gui.screen().children().stream()
                    .filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> LanControls.hasLabel(button.getMessage(), key)).findFirst().orElse(null);
        }
    }

    @Inject(method = "updateMultiplayerOptions", at = @At("TAIL"), require = 0)
    private void survivalCommands$updateOpenLabel(IntegratedServer server, CallbackInfo callback) {
        if (survivalCommands$open != null && server != null) {
            survivalCommands$open.setMessage(LanControls.text(survivalCommands$actionLabel(server)));
        }
    }

    @Redirect(method = {"applyChanges", "applyGeneralChanges"}, at = @At(value = "INVOKE",
            target = "Lio/github/satxm/mcwifipnp/Config;save(Lnet/minecraft/server/MinecraftServer;)V"), require = 0)
    private void survivalCommands$applyWithoutSaving(@Coerce Object object, MinecraftServer server) {
        if (object instanceof Config config) {
            config.survivalcommands$preserveCheats(server.getWorldData().isAllowCommands());
            LanCreativeRestriction.configure(server, survivalCommands$restrictCreative);
        }
    }

    @Inject(method = "applyChanges", at = @At("RETURN"), require = 0)
    private void survivalCommands$rememberRunningOptions(CallbackInfo callback) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            McWifiPnpSession.start(server, server.isPublished() && survivalCommands$config != null
                    ? survivalCommands$config.survivalcommands$runtimeOptions() : null);
        }
    }

    @Unique
    private String survivalCommands$actionLabel(IntegratedServer server) {
        return VanillaLanDraft.applyLabel(survivalCommands$config.survivalcommands$scope(), server.getMultiplayerScope());
    }

    @Unique
    private boolean survivalCommands$validPort(IntegratedServer server, Config config, Button button) {
        if (config.survivalcommands$scope() != MultiplayerScope.LAN || portEdit == null) {
            return true;
        }
        String value = portEdit.getValue().isBlank() ? Integer.toString(config.survivalcommands$port()) : portEdit.getValue();
        try {
            int port = Integer.parseInt(value);
            if (port >= 1024 && port <= 65535 && (server.isPublished() && port == server.getPort()
                    || HttpUtil.isPortAvailable(port))) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(LanControls.text("port_unavailable")));
        return false;
    }

    @Unique
    private Config survivalCommands$findConfig() {
        try {
            var field = getClass().getDeclaredField("cfg");
            if (field.trySetAccessible() && field.get(this) instanceof Config config) {
                return config;
            }
        } catch (ReflectiveOperationException error) {
            System.getLogger("Survival Commands").log(System.Logger.Level.WARNING,
                    "Cannot locate the optional mcwifipnp settings", error);
        }
        return null;
    }
}
