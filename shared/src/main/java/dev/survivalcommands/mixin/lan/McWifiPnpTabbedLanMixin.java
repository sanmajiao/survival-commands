package dev.survivalcommands.mixin.lan;

import dev.survivalcommands.lan.LanControls;
import dev.survivalcommands.lan.LanCreativeRestriction;
import dev.survivalcommands.lan.McWifiPnpAdapter;
import dev.survivalcommands.lan.McWifiPnpLegacyOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.satxm.mcwifipnp.client.ShareToLanScreenNew", remap = false)
abstract class McWifiPnpTabbedLanMixin {
    @Unique private McWifiPnpAdapter.HostOperator survivalCommands$hostOperator;
    @Unique private boolean survivalCommands$restrict = true;

    // ---------- Keep the third-party tabs and configuration path ----------

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void survivalCommands$loadRestriction(CallbackInfo callback) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            survivalCommands$restrict = McWifiPnpLegacyOptions.restriction(server);
        }
    }

    @Redirect(method = {"init", "method_25426", "m_7856_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", remap = true),
            require = 0, remap = true)
    private LayoutElement survivalCommands$addRestriction(HeaderAndFooterLayout layout, LayoutElement footer) {
        var column = new GridLayout().rowSpacing(4);
        var rows = column.createRowHelper(1);
        rows.defaultCellSetting().alignHorizontallyCenter();
        rows.addChild(LanControls.restriction(survivalCommands$restrict,
                value -> survivalCommands$restrict = value));
        rows.addChild(footer);
        layout.setFooterHeight(54);
        layout.addToFooter(column);
        return footer;
    }

    @Redirect(method = {"init", "method_25426", "m_7856_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;", remap = true),
            require = 0, remap = true)
    private Button.Builder survivalCommands$actions(Component label, Button.OnPress original) {
        if (label.equals(CommonComponents.GUI_CANCEL)) {
            return Button.builder(LanControls.text("save"), button -> {
                var server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    boolean saved = McWifiPnpAdapter.saveTabbedOptions(server, this);
                    LanControls.saved(button, saved && McWifiPnpLegacyOptions.saveRestriction(
                            server, survivalCommands$restrict));
                }
            });
        }
        return Button.builder(LanControls.text(label.equals(CommonComponents.GUI_DONE) ? "update" : "open"), original);
    }

    @Redirect(method = "onConfirmClicked", at = @At(value = "INVOKE",
            target = "Lio/github/satxm/mcwifipnp/Config;save()V"), require = 0)
    private void survivalCommands$openWithoutSaving(@Coerce Object config) {}

    // ---------- Apply the session policy and preserve the original operator list ----------

    @Inject(method = "onConfirmClicked()V", at = @At("HEAD"), require = 0, remap = false)
    private void survivalCommands$disableMcWifiPnpCheats(CallbackInfo callback) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            LanCreativeRestriction.configure(server, survivalCommands$restrict);
        }
        survivalCommands$hostOperator = McWifiPnpAdapter.keepExistingOperator(server);
        McWifiPnpAdapter.disableScreenCheats(server, this);
    }

    @Inject(method = "onConfirmClicked()V", at = @At("RETURN"), require = 0, remap = false)
    private void survivalCommands$restoreHostOperator(CallbackInfo callback) {
        var hostOperator = survivalCommands$hostOperator;
        survivalCommands$hostOperator = null;
        if (hostOperator != null) {
            hostOperator.restore();
        }
    }
}
