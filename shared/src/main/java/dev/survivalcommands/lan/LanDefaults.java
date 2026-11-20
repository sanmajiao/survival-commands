package dev.survivalcommands.lan;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Only the explicit Save button writes defaults. Vanilla settings and mcwifipnp's lock preference are separate. */
public final class LanDefaults {
    private static final Logger LOGGER = LogUtils.getLogger();

    private LanDefaults() {}

    public static Properties read(MinecraftServer server, boolean mcWifiPnp) {
        Properties values = new Properties();
        Path path = path(server, mcWifiPnp);
        if (Files.isRegularFile(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                values.load(reader);
            } catch (IOException | IllegalArgumentException error) {
                LOGGER.warn("Could not read LAN settings from {}", path, error);
                values.clear();
            }
        }
        return values;
    }

    public static boolean save(MinecraftServer server, boolean mcWifiPnp, Properties values) {
        Path path = path(server, mcWifiPnp);
        try {
            Files.createDirectories(path.getParent());
            Path temp = Files.createTempFile(path.getParent(), "survivalcommands-lan-", ".tmp");
            try {
                try (var writer = Files.newBufferedWriter(temp)) {
                    values.store(writer, "Survival Commands LAN screen defaults");
                }
                try {
                    Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException error) {
            LOGGER.warn("Could not save LAN settings to {}", path, error);
            return false;
        }
    }

    public static int port(Properties values, int fallback) {
        try {
            int port = Integer.parseInt(values.getProperty("port", ""));
            return port >= 1024 && port <= 65535 ? port : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static <E extends Enum<E>> E option(Properties values, String key, Class<E> type, E fallback) {
        try {
            return Enum.valueOf(type, values.getProperty(key, ""));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static boolean creativeRestriction(Properties values) {
        return !"false".equalsIgnoreCase(values.getProperty("restrictCreative"));
    }

    private static Path path(MinecraftServer server, boolean mcWifiPnp) {
        String file = mcWifiPnp ? "survivalcommands-mcwifipnp.properties" : "survivalcommands-lan.properties";
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(file);
    }
}
