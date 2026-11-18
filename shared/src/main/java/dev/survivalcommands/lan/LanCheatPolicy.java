package dev.survivalcommands.lan;

import net.minecraft.server.MinecraftServer;

/** Opening any singleplayer world to LAN must not create new cheat permissions. */
public final class LanCheatPolicy {
    private LanCheatPolicy() {
    }

    public static boolean disablesCheats(MinecraftServer server) {
        return server != null && server.isSingleplayer();
    }
}
