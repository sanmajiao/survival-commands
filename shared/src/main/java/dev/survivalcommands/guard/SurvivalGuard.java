package dev.survivalcommands.guard;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

/** Gameplay policy stored independently for each server world. */
public final class SurvivalGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MinecraftServer, Boolean> CREATIVE_LOCKS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private SurvivalGuard() {
    }

    public static boolean isCreativeLocked(MinecraftServer server) {
        return server != null && CREATIVE_LOCKS.computeIfAbsent(server, SurvivalGuard::load);
    }

    public static boolean denies(MinecraftServer server, GameType requestedMode) {
        return requestedMode == GameType.CREATIVE && isCreativeLocked(server);
    }

    public static GameType allowedMode(MinecraftServer server, GameType requestedMode) {
        if (!denies(server, requestedMode)) {
            return requestedMode;
        }
        GameType defaultMode = server.getWorldData().getGameType();
        return defaultMode == GameType.CREATIVE ? GameType.SURVIVAL : defaultMode;
    }

    private static boolean load(MinecraftServer server) {
        Path world = server.getWorldPath(LevelResource.ROOT);
        Path originPath = world.resolve("data/survivalcommands-creative-origin.properties");
        Path configPath = world.resolve("serverconfig/survivalcommands-guard.properties");

        boolean creativeOrigin = saveOrReadOrigin(server, originPath);
        try {
            if (Files.notExists(configPath)) {
                writeNewFile(configPath, "# Allow creative mode in this world after reopening it.\nallowCreative=false\n");
            }
            return !creativeOrigin && !Boolean.parseBoolean(readProperties(configPath).getProperty("allowCreative"));
        } catch (IOException | IllegalArgumentException error) {
            LOGGER.warn("Could not read creative lock settings from {}", configPath, error);
            return !creativeOrigin;
        }
    }

    private static boolean saveOrReadOrigin(MinecraftServer server, Path file) {
        try {
            if (Files.notExists(file)) {
                boolean creative = server.getWorldData().getGameType() == GameType.CREATIVE;
                writeNewFile(file, "origin=" + (creative ? "creative" : "noncreative") + "\n");
            }
            return switch (readProperties(file).getProperty("origin", "")) {
                case "creative" -> true;
                case "noncreative" -> false;
                default -> throw new IOException("Invalid world origin value");
            };
        } catch (IOException | IllegalArgumentException error) {
            LOGGER.warn("Could not record or read the world origin at {}. Creative stays locked.", file, error);
            return false;
        }
    }

    private static Properties readProperties(Path file) throws IOException {
        Properties result = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            result.load(reader);
        }
        return result;
    }

    private static void writeNewFile(Path file, String value) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "survivalcommands-", ".tmp");
        try {
            Files.writeString(temp, value);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, file);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
