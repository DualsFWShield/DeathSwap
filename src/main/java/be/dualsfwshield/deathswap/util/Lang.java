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
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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

        // Save default resources if they don't exist
        for (String lang : new String[] { "en", "fr" }) {
            String fName = "messages_" + lang + ".yml";
            File f = new File(plugin.getDataFolder(), fName);
            if (!f.exists() && plugin.getResource(fName) != null) {
                plugin.saveResource(fName, false);
            }
        }

        if (!file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                plugin.getLogger().warning("Language file " + fileName
                        + " not found internally! Creating an empty file. Falling back to English defaults.");
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create language file", e);
                }
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load defaults from internal resource to fallback
        InputStream defStream = plugin.getResource(fileName);
        if (defStream == null) {
            // Fallback to English for missing keys in custom languages
            defStream = plugin.getResource("messages_en.yml");
        }

        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
            
            for (String key : defConfig.getKeys(true)) {
                if (defConfig.isString(key)) {
                    messages.put(key, config.getString(key));
                }
            }
        }

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                // Determine value: from custom config if present, otherwise from defaults
                messages.put(key, config.getString(key));
            }
        }

        plugin.getLogger().info("Loaded " + messages.size() + " messages for language: " + languageCode);
    }

    public static List<String> getAvailableLanguages(DeathSwapPlugin plugin) {
        List<String> langs = new ArrayList<>();
        // Add defaults
        langs.add("en");
        langs.add("fr");

        File dataFolder = plugin.getDataFolder();
        if (dataFolder.exists() && dataFolder.isDirectory()) {
            File[] files = dataFolder.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    String code = name.substring("messages_".length(), name.length() - 4);
                    if (!langs.contains(code)) {
                        langs.add(code);
                    }
                }
            }
        }
        return langs;
    }

    public static void setLanguageCode(DeathSwapPlugin plugin, String code) {
        if (instance != null) {
            instance.languageCode = code;
            plugin.getConfig().set("language", code);
            plugin.saveConfig();
            instance.load();
        }
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

    /**
     * Converts a snake_case string to camelCase.
     * e.g. "spawn_radius" -> "spawnRadius"
     */
    public static String toCamelCase(String snakeCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Helper to get a difficulty string.
     */
    public static String getDifficultyName(int difficulty) {
        return switch (difficulty) {
            case 1 -> "Easy";
            case 2 -> "Medium";
            case 3 -> "Hard";
            default -> "Unknown";
        };
    }
}
