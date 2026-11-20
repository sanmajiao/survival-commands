package dev.survivalcommands.lan;

import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-local policy for a published world. The permanent world guard has priority. */
public final class LanCreativeRestriction {
    private static final Map<MinecraftServer, Boolean> SESSIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private LanCreativeRestriction() {}

    public static void configure(MinecraftServer server, boolean enabled) {
        SESSIONS.put(server, enabled);
    }

    public static boolean active(MinecraftServer server) {
        return server != null && server.isSingleplayer() && server.isPublished()
                && SESSIONS.getOrDefault(server, true);
    }
}
