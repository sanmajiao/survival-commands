package dev.survivalcommands.commands;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

final class WorldCommandStorage extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<WorldCommandStorage> CODEC = Codec.STRING.listOf()
            .optionalFieldOf("Enabled", List.of())
            .xmap(WorldCommandStorage::load, data -> List.copyOf(data.permissions.enabled()))
            .codec();
    private static final SavedDataType<WorldCommandStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("survivalcommands", "permissions"),
            WorldCommandStorage::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final CommandPermissions permissions = new CommandPermissions(this::setDirty);

    static CommandPermissions forWorld(MinecraftServer server) {
        var storage = server.getDataStorage();
        WorldCommandStorage data = storage.get(TYPE);
        if (data == null) {
            data = readLegacy(server);
            storage.set(TYPE, data);
        }
        return data.permissions;
    }

    private static WorldCommandStorage load(List<String> ids) {
        var data = new WorldCommandStorage();
        data.permissions.load(ids);
        return data;
    }

    private static WorldCommandStorage readLegacy(MinecraftServer server) {
        var path = server.getWorldPath(LevelResource.ROOT).resolve("data/survivalcommands.dat");
        if (Files.isRegularFile(path)) {
            try {
                var tag = NbtIo.readCompressed(path, NbtAccounter.create(1_048_576));
                var imported = CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("data"))
                        .resultOrPartial(message -> LOGGER.warn("Could not import old command settings: {}", message));
                if (imported.isPresent()) {
                    var data = imported.get();
                    data.setDirty();
                    return data;
                }
            } catch (IOException exception) {
                LOGGER.warn("Could not import old command settings from {}", path, exception);
            }
        }
        return new WorldCommandStorage();
    }
}
