package be.dualsfwshield.deathswap.util;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Lang {

    private static Lang instance;
    private final DeathSwapPlugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String languageCode = "fr"; // Default

    private Lang(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public static void init(DeathSwapPlugin plugin) {
        instance = new Lang(plugin);
        instance.load();
    }

    public static Lang getInstance() {
        return instance;
    }

    public void load() {
        messages.clear();
        // Determine language from config, default to fr
        languageCode = plugin.getConfig().getString("language", "fr");
        String fileName = "messages_" + languageCode + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        // Save default resource if not exists
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load defaults from internal resource to fallback
        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }

        plugin.getLogger().info("Loaded " + messages.size() + " messages for language: " + languageCode);
    }

    public static String get(String key) {
        if (instance == null)
            return key;
        String msg = instance.messages.get(key);
        if (msg == null) {
            // Try fallback to default config or return key
            return key;
        }
        return msg;
    }

    public static String get(String key, String... placeholders) {
        String msg = get(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return msg;
    }

    public static Component getComponent(String key) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(get(key));
    }

    public static Component getComponent(String key, String... placeholders) {
        return colorize(get(key, placeholders));
    }

    public static Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static void send(CommandSender sender, String key) {
        sender.sendMessage(getComponent(key));
    }

    public static void send(CommandSender sender, String key, String... placeholders) {
        String msg = get(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(msg));
    }
}
