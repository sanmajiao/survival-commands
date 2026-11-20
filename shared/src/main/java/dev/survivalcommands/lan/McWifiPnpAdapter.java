package dev.survivalcommands.lan;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

/** mcwifipnp 1.20.1 also writes the host's operator entry outside publishServer. */
public final class McWifiPnpAdapter {
    private static final Logger LOGGER = LogUtils.getLogger();

    private McWifiPnpAdapter() {
    }

    public static HostOperator keepExistingOperator(MinecraftServer server) {
        if (!LanCheatPolicy.disablesCheats(server)) {
            return null;
        }
        GameProfile host = server.getSingleplayerProfile();
        ServerOpList operators = server.getPlayerList().getOps();
        return new HostOperator(operators, host, operators.get(host));
    }

    public static void disableLegacyCheats(MinecraftServer server) {
        if (!LanCheatPolicy.disablesCheats(server)) {
            return;
        }

        try {
            Class<?> lan = Class.forName("io.github.satxm.mcwifipnp.MCWiFiPnPUnit");
            Object config = lan.getMethod("getConfig", MinecraftServer.class).invoke(null, server);
            config.getClass().getField("AllowCommands").setBoolean(config, false);
            config.getClass().getField("AllPlayersCheats").setBoolean(config, false);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            LOGGER.warn("Could not update mcwifipnp's cheat options", exception);
        }
    }

    public static void disableScreenCheats(MinecraftServer server, Object screen) {
        if (!LanCheatPolicy.disablesCheats(server)) {
            return;
        }

        try {
            Class<?> lanScreen = Class.forName("io.github.satxm.mcwifipnp.client.ShareToLanScreenNew");
            Field screenConfig = lanScreen.getDeclaredField("cfg");
            if (!screenConfig.trySetAccessible()) {
                throw new IllegalAccessException("LAN screen config is not accessible");
            }
            Object config = screenConfig.get(screen);
            config.getClass().getField("allowHostCheat").setBoolean(config, false);
            config.getClass().getField("allowEveryoneCheat").setBoolean(config, false);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            LOGGER.warn("Could not update mcwifipnp's cheat options", exception);
        }
    }

    // ---------- Explicit save for the optional LAN screens ----------

    public static boolean saveLegacyOptions(MinecraftServer server, String port, String players, String motd) {
        try {
            Class<?> mod = Class.forName("io.github.satxm.mcwifipnp.MCWiFiPnPUnit");
            Object config = mod.getMethod("getConfig", MinecraftServer.class).invoke(null, server);
            Class<?> type = config.getClass();
            if (!port.isEmpty()) {
                type.getField("port").setInt(config, Integer.parseInt(port));
            }
            if (!players.isEmpty()) {
                type.getField("maxPlayers").setInt(config, Integer.parseInt(players));
            }
            if (!motd.isEmpty()) {
                type.getField("motd").set(config, motd);
            }
            disableLegacyCheats(server);
            var save = mod.getDeclaredMethod("saveConfig", type);
            if (!save.trySetAccessible()) {
                return false;
            }
            save.invoke(null, config);
            return matchesSavedConfig(config, new Gson());
        } catch (ReflectiveOperationException | RuntimeException error) {
            LOGGER.warn("Could not save mcwifipnp's legacy settings", error);
            return false;
        }
    }

    public static boolean saveTabbedOptions(MinecraftServer server, Object screen) {
        try {
            Field field = screen.getClass().getDeclaredField("cfg");
            if (!field.trySetAccessible()) {
                return false;
            }
            Object config = field.get(screen);
            disableScreenCheats(server, screen);
            config.getClass().getMethod("save").invoke(config);
            Gson gson = (Gson) config.getClass().getField("GSON").get(null);
            return matchesSavedConfig(config, gson);
        } catch (ReflectiveOperationException | RuntimeException error) {
            LOGGER.warn("Could not save mcwifipnp's tabbed settings", error);
            return false;
        }
    }

    private static boolean matchesSavedConfig(Object config, Gson gson) throws ReflectiveOperationException {
        Path file = (Path) config.getClass().getField("location").get(config);
        try {
            return gson.toJsonTree(config).equals(JsonParser.parseString(Files.readString(file)));
        } catch (java.io.IOException error) {
            return false;
        }
    }

    /** mcwifipnp rewrites the host's operator entry even when the cheat button is off. */
    public record HostOperator(ServerOpList operators, GameProfile host, ServerOpListEntry previous) {
        public void restore() {
            ServerOpListEntry current = operators.get(host);
            if (current == previous) {
                return;
            }
            if (previous == null) {
                operators.remove(host);
            } else {
                operators.add(previous);
            }
        }
    }
}
