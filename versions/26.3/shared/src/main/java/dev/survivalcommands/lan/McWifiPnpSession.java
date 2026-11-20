package dev.survivalcommands.lan;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer.MultiplayerScope;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Optional mcwifipnp integration. Runtime values are separate from its published JSON defaults. */
public final class McWifiPnpSession {
    private static final Map<MinecraftServer, Options> RUNNING =
            Collections.synchronizedMap(new WeakHashMap<>());

    private McWifiPnpSession() {}

    public record Options(int maxPlayers, boolean upnp, boolean copyPublicAddress) {}

    public static Options options(MinecraftServer server) {
        return RUNNING.get(server);
    }

    public static void start(MinecraftServer server, Options options) {
        RUNNING.put(server, options);
    }

    public interface Config {
        boolean survivalcommands$restrictCreative();
        void survivalcommands$restrictCreative(boolean value);
        boolean survivalcommands$usesDefaults();
        int survivalcommands$port();
        MultiplayerScope survivalcommands$scope();
        void survivalcommands$scope(MultiplayerScope value);
        void survivalcommands$preserveCheats(boolean hostAllowed);
        Options survivalcommands$runtimeOptions();
        void survivalcommands$readRunning(MinecraftServer server);
        boolean survivalcommands$saveDefaults(MinecraftServer server);
    }
}
