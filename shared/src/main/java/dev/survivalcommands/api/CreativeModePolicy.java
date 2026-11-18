package dev.survivalcommands.api;

import net.minecraft.server.MinecraftServer;

/** Optional guard module contract for other modules that display game mode permissions. */
public interface CreativeModePolicy {
    boolean survivalcommands$isCreativeLocked();

    static boolean isLocked(MinecraftServer server) {
        return server instanceof CreativeModePolicy policy && policy.survivalcommands$isCreativeLocked();
    }
}
