package dev.survivalcommands.commands;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;

final class WorldCommandStorage extends SavedData {
    private final CommandPermissions permissions = new CommandPermissions(this::setDirty);

    static CommandPermissions forWorld(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                WorldCommandStorage::load, WorldCommandStorage::new, "survivalcommands").permissions;
    }

    private static WorldCommandStorage load(CompoundTag tag) {
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
    public CompoundTag save(CompoundTag tag) {
        ListTag ids = new ListTag();
        permissions.enabled().forEach(id -> ids.add(StringTag.valueOf(id)));
        tag.put("Enabled", ids);
        return tag;
    }
}
