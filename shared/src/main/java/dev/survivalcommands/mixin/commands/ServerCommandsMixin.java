package dev.survivalcommands.mixin.commands;

import com.mojang.brigadier.StringReader;
import dev.survivalcommands.commands.CommandPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerCommandsMixin {
    @ModifyArg(method = "parseCommand",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Ljava/lang/String;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;", remap = false),
            index = 1)
    private Object survivalcommands$authorizeChat(String input, Object source) {
        return CommandPermissions.forInput((CommandSourceStack) source, new StringReader(input));
    }

    @ModifyArg(method = "handleCustomCommandSuggestions",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;", remap = false),
            index = 1)
    private Object survivalcommands$authorizeSuggestions(StringReader reader, Object source) {
        return CommandPermissions.forInput((CommandSourceStack) source, reader);
    }
}
