package dev.survivalcommands.mixin.commands;

import com.mojang.brigadier.tree.CommandNode;
import dev.survivalcommands.commands.CommandPermissions;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.minecraftforge.server.command.CommandHelper", remap = false)
abstract class ForgeCommandHelperMixin {
    @Redirect(method = "mergeCommandNode",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z"),
            require = 0)
    private static <S> boolean survivalcommands$canUse(CommandNode<S> node, S source) {
        if (source instanceof CommandSourceStack serverSource) {
            @SuppressWarnings("unchecked")
            CommandNode<CommandSourceStack> serverNode = (CommandNode<CommandSourceStack>) node;
            return CommandPermissions.canUseForTree(serverNode, serverSource);
        }
        return node.canUse(source);
    }
}
