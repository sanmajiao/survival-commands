package dev.survivalcommands.client;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/** The client view of the commands granted by the current world's host. */
public final class CommandSettingsClient {
    private static final BiConsumer<String, Boolean> NO_SENDER = (id, enabled) -> {};

    private static volatile Snapshot snapshot;
    private static volatile BiConsumer<String, Boolean> sender = NO_SENDER;

    private CommandSettingsClient() {}

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void sync(Snapshot snapshot) {
        CommandSettingsClient.snapshot = Objects.requireNonNull(snapshot);
    }

    public static void disconnect() {
        snapshot = null;
        sender = NO_SENDER;
    }

    public static void setSender(BiConsumer<String, Boolean> sender) {
        CommandSettingsClient.sender = Objects.requireNonNull(sender);
    }

    static void requestChange(String id, boolean enabled) {
        Snapshot current = snapshot;
        if (current != null && current.editable()) {
            sender.accept(id, enabled);
        }
    }

    public record Entry(String id, String command, List<String> aliases, String labelKey,
                        boolean enabled, boolean available, String parentId) {
        public Entry {
            aliases = List.copyOf(aliases);
        }

        public Entry(String id, String command, List<String> aliases, String labelKey,
                     boolean enabled, boolean available) {
            this(id, command, aliases, labelKey, enabled, available, null);
        }
    }

    public record Snapshot(boolean editable, List<Entry> commands) {
        public Snapshot {
            commands = List.copyOf(commands);
        }
    }
}
