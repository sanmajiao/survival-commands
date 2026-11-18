package dev.survivalcommands.mixin.commands;

import com.mojang.brigadier.tree.CommandNode;
import dev.survivalcommands.commands.CommandPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Commands.class)
abstract class CommandsMixin {
    @Redirect(method = "fillUsableCommands",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z", remap = false))
    private boolean survivalcommands$canUse(CommandNode<CommandSourceStack> node, Object source) {
        return CommandPermissions.canUseForTree(node, (CommandSourceStack) source);
    }
}
