package dev.survivalcommands.lan;

import net.minecraft.server.MinecraftServer;

import java.util.Properties;

/** Older mcwifipnp versions keep their own config format, so the creative preference has its own file. */
public final class McWifiPnpLegacyOptions {
    private McWifiPnpLegacyOptions() {}

    public static boolean restriction(MinecraftServer server) {
        return LanDefaults.creativeRestriction(LanDefaults.read(server, true));
    }

    public static boolean saveRestriction(MinecraftServer server, boolean value) {
        Properties options = new Properties();
        options.setProperty("restrictCreative", Boolean.toString(value));
        return LanDefaults.save(server, true, options);
    }
}
