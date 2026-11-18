package dev.survivalcommands.mixin.commands;

import com.mojang.brigadier.StringReader;
import dev.survivalcommands.commands.CommandPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.network.ServerCommandSuggestionsProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerCommandSuggestionsProvider.class)
abstract class ServerCommandSuggestionsMixin {
    @ModifyArg(method = "tryProcessRequest",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;"),
            index = 1)
    private Object survivalcommands$authorizeSuggestions(StringReader reader, Object source) {
        return CommandPermissions.forInput((CommandSourceStack) source, reader);
    }
}
