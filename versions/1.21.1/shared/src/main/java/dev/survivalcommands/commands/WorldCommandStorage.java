package dev.survivalcommands.commands;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;

final class WorldCommandStorage extends SavedData {
    private static final Factory<WorldCommandStorage> FACTORY =
            new Factory<>(WorldCommandStorage::new, WorldCommandStorage::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final CommandPermissions permissions = new CommandPermissions(this::setDirty);

    static CommandPermissions forWorld(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "survivalcommands").permissions;
    }

    private static WorldCommandStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldCommandStorage data = new WorldCommandStorage();
        ListTag saved = tag.getList("Enabled", Tag.TAG_STRING);
        var ids = new ArrayList<String>(saved.size());
        for (int index = 0; index < saved.size(); index++) {
            ids.add(saved.getString(index));
        }
        data.permissions.load(ids);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag ids = new ListTag();
        permissions.enabled().forEach(id -> ids.add(StringTag.valueOf(id)));
        tag.put("Enabled", ids);
        return tag;
    }
}
