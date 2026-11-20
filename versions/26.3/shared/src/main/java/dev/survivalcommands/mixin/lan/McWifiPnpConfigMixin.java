package dev.survivalcommands.mixin.lan;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import dev.survivalcommands.lan.McWifiPnpSession.Config;
import dev.survivalcommands.lan.McWifiPnpSession.Options;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer.MultiplayerScope;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Pseudo
@Mixin(targets = "io.github.satxm.mcwifipnp.Config", remap = false)
abstract class McWifiPnpConfigMixin implements Config {
    @Shadow @Final private static Gson GSON;
    @Shadow @Final public boolean usingDefaults;
    @Shadow public boolean applyforallworld;
    @Shadow public MultiplayerScope multiplayerScope;
    @Shadow public boolean allowHostCommands;
    @Shadow public boolean allowGuestCommands;
    @Shadow public int port;
    @Shadow public int maxPlayers;
    @Shadow public boolean useUPnP;
    @Shadow public boolean getPublicIP;
    @Shadow public abstract void save(MinecraftServer server);
    @Shadow public abstract void readFromRunningServer(MinecraftServer server);
    @Shadow public static Path getWorldPath(MinecraftServer server) { throw new AssertionError(); }
    @Shadow public static Path getConfigPath(MinecraftServer server) { throw new AssertionError(); }

    // Gson runs the upstream no-argument constructor for both old and newly saved configurations.
    @Unique @SerializedName("survivalcommands-restrict-creative")
    private boolean survivalCommands$restrictCreative = true;

    @Override
    public boolean survivalcommands$restrictCreative() {
        return survivalCommands$restrictCreative;
    }

    @Override
    public void survivalcommands$restrictCreative(boolean value) {
        survivalCommands$restrictCreative = value;
    }

    @Override
    public boolean survivalcommands$usesDefaults() {
        return usingDefaults;
    }

    @Override
    public int survivalcommands$port() {
        return port;
    }

    @Override
    public MultiplayerScope survivalcommands$scope() {
        return multiplayerScope;
    }

    @Override
    public void survivalcommands$scope(MultiplayerScope value) {
        multiplayerScope = value;
    }

    @Override
    public void survivalcommands$preserveCheats(boolean hostAllowed) {
        allowHostCommands = hostAllowed;
        allowGuestCommands = false;
    }

    @Override
    public Options survivalcommands$runtimeOptions() {
        return new Options(maxPlayers, useUPnP, getPublicIP);
    }

    @Override
    public void survivalcommands$readRunning(MinecraftServer server) {
        readFromRunningServer(server);
    }

    @Override
    public boolean survivalcommands$saveDefaults(MinecraftServer server) {
        save(server);
        try {
            var saved = GSON.toJsonTree(this);
            boolean worldMatches = saved.equals(JsonParser.parseString(Files.readString(getWorldPath(server))));
            Path global = getConfigPath(server);
            boolean scopeMatches = applyforallworld
                    ? saved.equals(JsonParser.parseString(Files.readString(global))) : Files.notExists(global);
            return worldMatches && scopeMatches;
        } catch (IOException | RuntimeException error) {
            return false;
        }
    }
}
