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
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z"))
    private static <S> boolean survivalcommands$canUse(CommandNode<S> node, S source) {
        if (source instanceof CommandSourceStack serverSource) {
            @SuppressWarnings("unchecked")
            CommandNode<CommandSourceStack> serverNode = (CommandNode<CommandSourceStack>) node;
            return CommandPermissions.canUseForTree(serverNode, serverSource);
        }
        return node.canUse(source);
    }
}
