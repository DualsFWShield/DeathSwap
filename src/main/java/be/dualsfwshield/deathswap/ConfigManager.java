package be.dualsfwshield.deathswap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages loading, saving, and accessing the plugin configuration.
 * Supports multi-arena configs with per-arena settings (in arenas/ folder),
 * per-mode prefixes,
 * and feature toggles (stats, sounds, challenges, voting).
 */
public class ConfigManager {

    private final DeathSwapPlugin plugin;

    // Global
    private String hubWorld;

    private String teleportCommand;
    private List<String> worldResetCommands;
    private final Map<GameType, String> prefixes = new EnumMap<>(GameType.class);

    // Feature toggles
    private boolean statsEnabled;
    private int statsAutoSaveMinutes;
    private boolean soundsEnabled;
    private boolean challengesEnabled;
    private boolean votingEnabled;
    private int voteTime;
    private int voteOptionsCount;

    // Sounds config
    private final Map<String, SoundConfig> sounds = new HashMap<>();

    // Challenges config
    private final List<ChallengeConfig> challengeList = new ArrayList<>();

    // Per-arena settings
    private final Map<String, ArenaConfig> arenaConfigs = new HashMap<>();

    // Mode-specific configs
    private BlockShuffleConfig blockShuffleConfig;
    private DeathShuffleConfig deathShuffleConfig;

    public ConfigManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Load or reload all configuration.
     * Migrates old config.yml arenas to arenas/ folder if found.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.hubWorld = config.getString("hub-world", "MainLobby");
        this.teleportCommand = config.getString("teleport-command",
                "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%");

        if (config.contains("world-reset-commands")) {
            this.worldResetCommands = config.getStringList("world-reset-commands");
        } else {
            // Default to CyberWorldReset behavior if missing
            this.worldResetCommands = new ArrayList<>();
            this.worldResetCommands.add("cwr edit %world% setSeed %seed%");
            this.worldResetCommands.add("cwr reset %world%");
        }

        // Load prefixes per mode
        prefixes.clear();
        ConfigurationSection prefSection = config.getConfigurationSection("prefixes");
        if (prefSection != null) {
            for (GameType type : GameType.values()) {
                String key = type.name().toLowerCase();
                prefixes.put(type, prefSection.getString(key, type.getDefaultPrefix()));
            }
        } else {
            for (GameType type : GameType.values()) {
                prefixes.put(type, type.getDefaultPrefix());
            }
        }

        // Load feature toggles
        ConfigurationSection statsSection = config.getConfigurationSection("stats");
        if (statsSection != null) {
            statsEnabled = statsSection.getBoolean("enabled", true);
            statsAutoSaveMinutes = statsSection.getInt("auto-save-minutes", 5);
        } else {
            statsEnabled = true;
            statsAutoSaveMinutes = 5;
        }

        ConfigurationSection soundsSection = config.getConfigurationSection("sounds");
        if (soundsSection != null) {
            soundsEnabled = soundsSection.getBoolean("enabled", true);
            sounds.clear();
            for (String key : soundsSection.getKeys(false)) {
                if (key.equals("enabled"))
                    continue;
                ConfigurationSection s = soundsSection.getConfigurationSection(key);
                if (s != null) {
                    sounds.put(key, new SoundConfig(
                            s.getString("type", "BLOCK_NOTE_BLOCK_HAT"),
                            (float) s.getDouble("volume", 1.0),
                            (float) s.getDouble("pitch", 1.0)));
                }
            }
        } else {
            soundsEnabled = true;
        }

        ConfigurationSection challengeSection = config.getConfigurationSection("challenges");
        if (challengeSection != null) {
            challengesEnabled = challengeSection.getBoolean("enabled", false);
            challengeList.clear();
            List<?> list = challengeSection.getList("list");
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof Map<?, ?> map) {
                        Object typeVal = map.get("type");
                        String type = typeVal != null ? String.valueOf(typeVal) : "CRAFT";

                        Object targetVal = map.get("target");
                        String target = targetVal != null ? String.valueOf(targetVal) : "CRAFTING_TABLE";

                        Object amountVal = map.get("amount");
                        int amount = amountVal instanceof Number ? ((Number) amountVal).intValue() : 1;

                        Object rewardVal = map.get("reward");
                        String reward = rewardVal != null ? String.valueOf(rewardVal) : "SPEED";

                        Object descVal = map.get("description");
                        String desc = descVal != null ? String.valueOf(descVal) : "";

                        challengeList.add(new ChallengeConfig(type, target, amount, reward, desc));
                    }
                }
            }
        } else {
            challengesEnabled = false;
        }

        ConfigurationSection votingSection = config.getConfigurationSection("voting");
        if (votingSection != null) {
            votingEnabled = votingSection.getBoolean("enabled", true);
            voteTime = votingSection.getInt("vote-time", 15);
            voteOptionsCount = votingSection.getInt("options-count", 3);
        } else {
            votingEnabled = true;
            voteTime = 15;
            voteOptionsCount = 3;
        }

        // --- Load Mode Configs ---
        loadBlockShuffleConfig();
        loadDeathShuffleConfig();

        // --- Arena Migration Logic ---
        File arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
        }

        ConfigurationSection oldArenasSection = config.getConfigurationSection("arenas");
        if (oldArenasSection != null) {
            plugin.getLogger().info("Migrating arenas from config.yml to arenas/ folder...");
            for (String key : oldArenasSection.getKeys(false)) {
                ConfigurationSection section = oldArenasSection.getConfigurationSection(key);
                if (section == null)
                    continue;
                ArenaConfig ac = loadArenaConfigFromSection(key, section);
                saveArena(ac); // Save to individual file
                plugin.getLogger().info("Migrated arena: " + key);
            }
            // Clear arenas from config.yml
            config.set("arenas", null);
            plugin.saveConfig();
        }

        // --- Load Arenas from File ---
        arenaConfigs.clear();
        File[] files = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.equals("example.yml"))
                    continue; // Skip example arena
                String id = fileName.substring(0, fileName.lastIndexOf('.'));
                YamlConfiguration arenaConfig = YamlConfiguration.loadConfiguration(file);
                ArenaConfig ac = loadArenaConfigFromSection(id, arenaConfig);
                arenaConfigs.put(id, ac);
            }
        }

        // Always ensure example.yml exists as a reference
        File exampleFile = new File(arenasFolder, "example.yml");
        if (!exampleFile.exists()) {
            // Save example.yml from resources if available
            try {
                plugin.saveResource("arenas/example.yml", false);
                plugin.getLogger().info("Created example.yml from resources.");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Could not find arenas/example.yml in resources!");
                // Fallback to programmatic creation if resource missing
                createArena("example");
            }
        }

        plugin.getLogger().info("Loaded " + arenaConfigs.size() + " arena(s) from arenas/ folder.");
    }

    private void loadBlockShuffleConfig() {
        File file = new File(plugin.getDataFolder(), "modes/blockshuffle.yml");
        if (!file.exists()) {
            plugin.saveResource("modes/blockshuffle.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Config reading logic moved to BlockShuffleInstance or kept minimal here

        this.blockShuffleConfig = new BlockShuffleConfig(config);
    }

    private void loadDeathShuffleConfig() {
        File file = new File(plugin.getDataFolder(), "modes/deathshuffle.yml");
        if (!file.exists()) {
            plugin.saveResource("modes/deathshuffle.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.deathShuffleConfig = new DeathShuffleConfig(config);
    }

    public void createArena(String id) {
        if (arenaConfigs.containsKey(id))
            return;
        ArenaConfig ac = new ArenaConfig();
        ac.id = id;
        ac.gameWorld = id + "_Game";
        ac.lobbyWorld = id + "_Lobby";
        arenaConfigs.put(id, ac);
        saveArena(ac);
        plugin.getLogger().info("Created new arena: " + id);
    }

    /**
     * Delete an arena config and its file.
     * 
     * @return true if the arena existed and was deleted
     */
    public boolean deleteArena(String id) {
        ArenaConfig removed = arenaConfigs.remove(id);
        if (removed == null)
            return false;

        File arenaFile = new File(plugin.getDataFolder(), "arenas/" + id + ".yml");
        if (arenaFile.exists()) {
            arenaFile.delete();
        }
        plugin.getLogger().info("Deleted arena: " + id);
        return true;
    }

    /**
     * Clone an existing arena config to a new ID.
     * 
     * @return true if cloning succeeded
     */
    public boolean cloneArena(String sourceId, String targetId) {
        ArenaConfig source = arenaConfigs.get(sourceId);
        if (source == null)
            return false;
        if (arenaConfigs.containsKey(targetId))
            return false;

        ArenaConfig clone = new ArenaConfig();
        clone.id = targetId;
        clone.gameType = source.gameType;
        clone.gameWorld = targetId + "_Game";
        clone.lobbyWorld = targetId + "_Lobby";
        clone.minPlayers = source.minPlayers;
        clone.maxPlayers = source.maxPlayers;
        clone.uiMode = source.uiMode;
        clone.loadTime = source.loadTime;
        clone.swapMode = source.swapMode;
        clone.swapInterval = source.swapInterval;
        clone.swapMin = source.swapMin;
        clone.swapMax = source.swapMax;
        clone.maxGameTime = source.maxGameTime;
        clone.spawnProtection = source.spawnProtection;
        clone.roundTimeEasy = source.roundTimeEasy;
        clone.roundTimeMedium = source.roundTimeMedium;
        clone.roundTimeHard = source.roundTimeHard;
        clone.pvpEnabled = source.pvpEnabled;
        clone.netherEnabled = source.netherEnabled;
        clone.endEnabled = source.endEnabled;

        clone.seeds = new ArrayList<>(source.seeds);
        clone.gamerules = new HashMap<>(source.gamerules);

        arenaConfigs.put(targetId, clone);
        saveArena(clone);
        plugin.getLogger().info("Cloned arena '" + sourceId + "' → '" + targetId + "'");
        return true;
    }

    private ArenaConfig loadArenaConfigFromSection(String id, ConfigurationSection section) {
        ArenaConfig ac = new ArenaConfig();
        ac.id = id;

        // Game type
        String typeStr = section.getString("game-type", "DEATHSWAP").toUpperCase();
        try {
            ac.gameType = GameType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            ac.gameType = GameType.DEATHSWAP;
            plugin.getLogger()
                    .warning("Invalid game-type '" + typeStr + "' for arena " + id + ", defaulting to DEATHSWAP.");
        }

        ac.gameWorld = section.getString("game-world", "DeathSwap_Game");
        ac.lobbyWorld = section.getString("lobby-world", "DS_WaitingLobby");
        ac.minPlayers = section.getInt("min-players", 2);
        ac.maxPlayers = section.getInt("max-players", 20);

        try {
            ac.uiMode = UIMode.valueOf(section.getString("ui-mode", "RICH").toUpperCase());
        } catch (IllegalArgumentException e) {
            ac.uiMode = UIMode.RICH;
        }

        // Load gamerules
        if (section.isConfigurationSection("gamerules")) {
            ConfigurationSection rulesSection = section.getConfigurationSection("gamerules");
            for (String key : rulesSection.getKeys(false)) {
                ac.gamerules.put(key, rulesSection.getString(key));
            }
        }

        // Timers
        ConfigurationSection timers = section.getConfigurationSection("timers");
        if (timers != null) {
            ac.loadTime = timers.getInt("load-time", 40);
            ac.swapMode = SwapMode.valueOf(timers.getString("swap-mode", "FIXED").toUpperCase());
            ac.swapInterval = timers.getInt("swap-interval", 300);
            ac.swapMin = timers.getInt("swap-min", 120);
            ac.swapMax = timers.getInt("swap-max", 420);
            ac.maxGameTime = timers.getInt("max-game-time", 1800);
            ac.spawnProtection = timers.getInt("spawn-protection", 30);
        }

        // Round timers (for DeathShuffle / BlockShuffle)
        ConfigurationSection roundTimers = section.getConfigurationSection("round-timers");
        if (roundTimers != null) {
            ac.roundTimeEasy = roundTimers.getInt("easy", 90);
            ac.roundTimeMedium = roundTimers.getInt("medium", 70);
            ac.roundTimeHard = roundTimers.getInt("hard", 50);
        }

        // Game rules
        ConfigurationSection game = section.getConfigurationSection("game");
        if (game != null) {
            ac.pvpEnabled = game.getBoolean("pvp-enabled", true);
            ac.netherEnabled = game.getBoolean("nether-enabled", true);
            ac.endEnabled = game.getBoolean("end-enabled", true);
        }

        // Seeds
        ac.seeds = new ArrayList<>();
        List<?> seedList = section.getList("seeds");
        if (seedList != null) {
            for (Object obj : seedList) {
                if (obj instanceof Map<?, ?> map) {
                    Object seedVal = map.get("seed");
                    String seedStr = seedVal != null ? String.valueOf(seedVal) : "";
                    Object nameVal = map.get("name");
                    String name = nameVal != null ? String.valueOf(nameVal) : "Unknown";
                    ac.seeds.add(new SeedEntry(seedStr, name));
                }
            }
        }

        // Resilience settings
        ac.startIfMinPlayersMet = section.getBoolean("start-if-min-players-met", false);
        ac.preventCancelAfterCountdown = section.getBoolean("prevent-cancel-after-countdown", false);

        // Command overrides
        ac.teleportCommand = section.getString("teleport-command", null);
        if (section.contains("world-reset-commands")) {
            ac.worldResetCommands = section.getStringList("world-reset-commands");
        } else {
            ac.worldResetCommands = null; // Use global default
        }

        return ac;
    }

    /**
     * Save global configuration to config.yml (feature toggles, etc.).
     * Does NOT save individual arenas. Use saveArena() for that.
     */
    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("hub-world", hubWorld);
        config.set("teleport-command", teleportCommand);
        config.set("world-reset-commands", worldResetCommands);

        // Save prefixes
        for (Map.Entry<GameType, String> entry : prefixes.entrySet()) {
            config.set("prefixes." + entry.getKey().name().toLowerCase(), entry.getValue());
        }

        // Save feature toggles
        config.set("stats.enabled", statsEnabled);
        config.set("stats.auto-save-minutes", statsAutoSaveMinutes);
        config.set("sounds.enabled", soundsEnabled);
        config.set("challenges.enabled", challengesEnabled);
        config.set("voting.enabled", votingEnabled);
        config.set("voting.vote-time", voteTime);
        config.set("voting.options-count", voteOptionsCount);

        plugin.saveConfig();

        // Also save all arenas just in case
        for (ArenaConfig ac : arenaConfigs.values()) {
            saveArena(ac);
        }
    }

    /**
     * Save a specific arena's configuration to arenas/<id>.yml.
     */
    public void saveArena(ArenaConfig ac) {
        File arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists())
            arenasFolder.mkdirs();

        File arenaFile = new File(arenasFolder, ac.id + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("game-type", ac.gameType.name());
        config.set("game-world", ac.gameWorld);
        config.set("lobby-world", ac.lobbyWorld);
        config.set("min-players", ac.minPlayers);
        config.set("max-players", ac.maxPlayers);
        config.set("ui-mode", ac.uiMode.name());

        config.set("timers.load-time", ac.loadTime);
        config.set("timers.swap-mode", ac.swapMode.name());
        config.set("timers.swap-interval", ac.swapInterval);
        config.set("timers.swap-min", ac.swapMin);
        config.set("timers.swap-max", ac.swapMax);
        config.set("timers.max-game-time", ac.maxGameTime);
        config.set("timers.spawn-protection", ac.spawnProtection);

        config.set("round-timers.easy", ac.roundTimeEasy);
        config.set("round-timers.medium", ac.roundTimeMedium);
        config.set("round-timers.hard", ac.roundTimeHard);

        config.set("game.pvp-enabled", ac.pvpEnabled);
        config.set("game.nether-enabled", ac.netherEnabled);
        config.set("game.end-enabled", ac.endEnabled);

        ConfigurationSection rulesSection = config.createSection("gamerules");
        for (Map.Entry<String, String> ruleEntry : ac.gamerules.entrySet()) {
            rulesSection.set(ruleEntry.getKey(), ruleEntry.getValue());
        }

        List<Map<String, String>> seedMaps = new ArrayList<>();
        for (SeedEntry se : ac.seeds) {
            Map<String, String> map = new HashMap<>();
            map.put("seed", se.seed());
            map.put("name", se.name());
            seedMaps.add(map);
        }
        config.set("start-if-min-players-met", ac.startIfMinPlayersMet);
        config.set("prevent-cancel-after-countdown", ac.preventCancelAfterCountdown);

        if (ac.teleportCommand != null) {
            config.set("teleport-command", ac.teleportCommand);
        }
        if (ac.worldResetCommands != null) {
            config.set("world-reset-commands", ac.worldResetCommands);
        }

        config.set("seeds", seedMaps);

        try {
            config.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save arena config: " + ac.id, e);
        }
    }

    // --- Getters ---

    public String getHubWorld() {
        return hubWorld;
    }

    public String getPrefix(GameType type) {
        return prefixes.getOrDefault(type, type.getDefaultPrefix());
    }

    public ArenaConfig getArenaConfig(String id) {
        return arenaConfigs.get(id);
    }

    public Set<String> getArenaIds() {
        return arenaConfigs.keySet();
    }

    public Map<String, ArenaConfig> getAllArenaConfigs() {
        return arenaConfigs;
    }

    public boolean isStatsEnabled() {
        return statsEnabled;
    }

    public int getStatsAutoSaveMinutes() {
        return statsAutoSaveMinutes;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public boolean isChallengesEnabled() {
        return challengesEnabled;
    }

    public boolean isVotingEnabled() {
        return votingEnabled;
    }

    public int getVoteTime() {
        return voteTime;
    }

    public int getVoteOptionsCount() {
        return voteOptionsCount;
    }

    public Map<String, SoundConfig> getSounds() {
        return sounds;
    }

    public List<ChallengeConfig> getChallengeList() {
        return challengeList;
    }

    // --- Setters ---

    public void setHubWorld(String hubWorld) {
        this.hubWorld = hubWorld;
    }

    public String getTeleportCommand() {
        return teleportCommand;
    }

    public List<String> getWorldResetCommands() {
        return worldResetCommands;
    }

    // --- Inner classes ---

    /**
     * Holds all configuration values for a single arena.
     */
    public static class ArenaConfig {
        public String id;
        public GameType gameType = GameType.DEATHSWAP;

        // Worlds
        public String gameWorld;
        public String lobbyWorld;

        // Players
        public int minPlayers = 2;
        public int maxPlayers = 20;
        public UIMode uiMode = UIMode.RICH;

        // Timers (DeathSwap)
        public int loadTime = 40;
        public SwapMode swapMode = SwapMode.FIXED;
        public int swapInterval = 300;
        public int swapMin = 120;
        public int swapMax = 420;
        public int maxGameTime = 1800;
        public int spawnProtection = 30;

        // Round timers (DeathShuffle / BlockShuffle)
        public int roundTimeEasy = 90;
        public int roundTimeMedium = 70;
        public int roundTimeHard = 50;

        // Game rules
        public boolean pvpEnabled = true;
        public boolean netherEnabled = true;
        public boolean endEnabled = true;

        // Seeds
        public List<SeedEntry> seeds = new ArrayList<>();

        // Gamerules
        public Map<String, String> gamerules = new HashMap<>();

        // Resilience
        public boolean startIfMinPlayersMet = false;
        public boolean preventCancelAfterCountdown = false;

        // Command overrides (null = use global default)
        public String teleportCommand = null;
        public List<String> worldResetCommands = null;

        public ArenaConfig() {
            // Default gamerules (1.21.1 snake_case names)
            gamerules.put("keep_inventory", "false");
            gamerules.put("immediate_respawn", "true");
            gamerules.put("spawn_radius", "0");
            gamerules.put("send_command_feedback", "false");
            gamerules.put("log_admin_commands", "false");
            gamerules.put("random_tick_speed", "3");
            gamerules.put("announce_advancements", "true");
            gamerules.put("do_daylight_cycle", "true");
            gamerules.put("do_weather_cycle", "true");
            gamerules.put("mob_griefing", "true");
            gamerules.put("natural_regeneration", "true");
            gamerules.put("do_mob_spawning", "true");
        }

        /**
         * Get the next swap interval based on the mode.
         */
        public int getNextSwapInterval() {
            if (swapMode == SwapMode.RANDOM) {
                int range = swapMax - swapMin;
                if (range <= 0)
                    return swapMin;
                return swapMin + (int) (Math.random() * (range + 1));
            }
            return swapInterval;
        }

        /**
         * Get round time based on difficulty tier (1=easy, 2=medium, 3+=hard).
         */
        public int getRoundTime(int difficulty) {
            if (difficulty <= 1)
                return roundTimeEasy;
            if (difficulty == 2)
                return roundTimeMedium;
            return roundTimeHard;
        }
    }

    /**
     * Sound configuration entry.
     */
    public record SoundConfig(String type, float volume, float pitch) {
    }

    /**
     * Challenge configuration entry.
     */
    public record ChallengeConfig(String type, String target, int amount, String reward, String description) {
    }

    public BlockShuffleConfig getBlockShuffleConfig() {
        return blockShuffleConfig;
    }

    public DeathShuffleConfig getDeathShuffleConfig() {
        return deathShuffleConfig;
    }

    public static class BlockShuffleConfig {
        private final FileConfiguration config;

        public BlockShuffleConfig(FileConfiguration config) {
            this.config = config;
        }

        public FileConfiguration getConfig() {
            return config;
        }
    }

    public static class DeathShuffleConfig {
        private final FileConfiguration config;

        public DeathShuffleConfig(FileConfiguration config) {
            this.config = config;
        }

        public FileConfiguration getConfig() {
            return config;
        }
    }

}
