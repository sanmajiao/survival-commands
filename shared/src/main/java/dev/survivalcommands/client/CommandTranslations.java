package dev.survivalcommands.client;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class CommandTranslations {
    private CommandTranslations() {}

    public static MutableComponent text(String key, Object... arguments) {
        // Loader-only Fabric installs can display the bundled text before any resource pack integration.
        String fallback = fallback(key);
        return fallback == null ? Component.translatable(key, arguments)
                : Component.translatableWithFallback(key, fallback, arguments);
    }

    static boolean has(String key) {
        return key != null && (I18n.exists(key) || fallback(key) != null);
    }

    static String chinese(String key) {
        return Labels.CHINESE.get(key);
    }

    private static String fallback(String key) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        if (language != null && language.startsWith("zh_")) {
            return Labels.CHINESE.getOrDefault(key, Labels.ENGLISH.get(key));
        }
        return Labels.ENGLISH.get(key);
    }

    private static final class Labels {
        private static final Map<String, String> CHINESE = load("zh_cn");
        private static final Map<String, String> ENGLISH = load("en_us");

        private static Map<String, String> load(String language) {
            var resource = CommandTranslations.class.getResourceAsStream(
                    "/assets/survivalcommands/lang/" + language + ".json");
            if (resource == null) {
                return Map.of();
            }
            try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                Map<String, String> labels = new HashMap<>();
                JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry ->
                        labels.put(entry.getKey(), entry.getValue().getAsString()));
                return Map.copyOf(labels);
            } catch (IOException | JsonParseException | IllegalStateException exception) {
                System.getLogger(CommandTranslations.class.getName())
                        .log(System.Logger.Level.WARNING, "Unable to load command settings labels for " + language,
                                exception);
                return Map.of();
            }
        }
    }
}
