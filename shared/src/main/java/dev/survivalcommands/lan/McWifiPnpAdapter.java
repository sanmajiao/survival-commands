package dev.survivalcommands.lan;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import org.slf4j.Logger;

import java.lang.reflect.Field;

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
