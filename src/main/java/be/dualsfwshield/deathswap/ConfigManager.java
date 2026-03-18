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
    private final Map<GameType, String> prefixes = new java.util.HashMap<>();

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

    // Global Seeds
    private final List<SeedEntry> globalSeeds = new ArrayList<>();

    public List<SeedEntry> getGlobalSeeds() {
        return globalSeeds;
    }

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
                            s.getString("type", "block.note_block.hat"),
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
        loadGlobalSeeds();

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
        this.blockShuffleConfig = new BlockShuffleConfig(config, file);
    }

    private void loadDeathShuffleConfig() {
        File file = new File(plugin.getDataFolder(), "modes/deathshuffle.yml");
        if (!file.exists()) {
            plugin.saveResource("modes/deathshuffle.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.deathShuffleConfig = new DeathShuffleConfig(config, file);
    }

    private void loadGlobalSeeds() {
        globalSeeds.clear();
        File file = new File(plugin.getDataFolder(), "seeds.yml");
        if (!file.exists()) {
            plugin.saveResource("seeds.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> seedList = config.getList("seeds");
        if (seedList != null) {
            for (Object obj : seedList) {
                if (obj instanceof Map<?, ?> map) {
                    Object seedVal = map.get("seed");
                    String seedStr = seedVal != null ? String.valueOf(seedVal) : "";
                    Object nameVal = map.get("name");
                    String name = nameVal != null ? String.valueOf(nameVal) : "Unknown";
                    globalSeeds.add(new SeedEntry(seedStr, name));
                }
            }
        }
        plugin.getLogger().info("Loaded " + globalSeeds.size() + " global seeds from seeds.yml.");
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
        clone.gameWorldNether = targetId + "_Game_nether";
        clone.gameWorldEnd = targetId + "_Game_the_end";
        clone.lobbyWorld = targetId + "_Lobby";
        clone.minPlayers = source.minPlayers;
        clone.maxPlayers = source.maxPlayers;
        clone.uiMode = source.uiMode;
        clone.loadTime = source.loadTime;
        clone.swapMode = source.swapMode;
        clone.swapInterval = source.swapInterval;
        clone.swapMin = source.swapMin;
        clone.swapMax = source.swapMax;
        clone.gameEndMode = source.gameEndMode;
        clone.maxGameTime = source.maxGameTime;
        clone.maxRounds = source.maxRounds;
        clone.spawnProtection = source.spawnProtection;
        clone.spawnRadius = source.spawnRadius;
        clone.minPlayerDistance = source.minPlayerDistance;
        clone.rtpMaxRetries = source.rtpMaxRetries;
        clone.gracePeriodBuffer = source.gracePeriodBuffer;
        clone.swapBlindnessDuration = source.swapBlindnessDuration;
        clone.endGameDelay = source.endGameDelay;
        clone.challengeRewardDuration = source.challengeRewardDuration;
        clone.roundTimeEasy = source.roundTimeEasy;
        clone.roundTimeMedium = source.roundTimeMedium;
        clone.roundTimeHard = source.roundTimeHard;
        clone.roundTimeExtreme = source.roundTimeExtreme;
        clone.difficultyMode = source.difficultyMode;
        clone.maxItemsPerGame = source.maxItemsPerGame;
        clone.pvpEnabled = source.pvpEnabled;
        clone.netherEnabled = source.netherEnabled;
        clone.endEnabled = source.endEnabled;
        clone.votingEnabled = source.votingEnabled;
        clone.voteTime = source.voteTime;
        clone.lightningStart = source.lightningStart;
        clone.worldLoadEnabled = source.worldLoadEnabled;
        clone.worldUnloadEnabled = source.worldUnloadEnabled;
        clone.worldLoadCommand = source.worldLoadCommand;
        clone.worldUnloadCommand = source.worldUnloadCommand;
        clone.teamsEnabled = source.teamsEnabled;
        clone.teamSize = source.teamSize;
        clone.maxTeams = source.maxTeams;
        clone.teamAutoAssign = source.teamAutoAssign;

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
        ac.gameWorldNether = section.getString("game-world-nether", ac.gameWorld + "_nether");
        ac.gameWorldEnd = section.getString("game-world-end", ac.gameWorld + "_the_end");
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

            try {
                ac.gameEndMode = GameEndMode.valueOf(timers.getString("game-end-mode", "TIME").toUpperCase());
            } catch (IllegalArgumentException e) {
                ac.gameEndMode = GameEndMode.TIME;
            }
            ac.maxGameTime = timers.getInt("max-game-time", 1800);
            ac.maxRounds = timers.getInt("max-rounds", 10);

            ac.spawnProtection = timers.getInt("spawn-protection", 30);
            ac.spawnRadius = timers.getInt("spawn-radius", 100);
            ac.minPlayerDistance = timers.getInt("min-player-distance", 50);
            ac.rtpMaxRetries = timers.getInt("rtp-max-retries", 10);
            ac.gracePeriodBuffer = timers.getInt("grace-period-buffer", 15);
            ac.swapBlindnessDuration = timers.getInt("swap-blindness-duration", 3);
            ac.endGameDelay = timers.getInt("end-game-delay", 5);
            ac.challengeRewardDuration = timers.getInt("challenge-reward-duration", 30);
        }

        // Round timers (for DeathShuffle / BlockShuffle)
        ConfigurationSection roundTimers = section.getConfigurationSection("round-timers");
        if (roundTimers != null) {
            ac.roundTimeEasy = roundTimers.getInt("easy", 300);
            ac.roundTimeMedium = roundTimers.getInt("medium", 600);
            ac.roundTimeHard = roundTimers.getInt("hard", 900);
            ac.roundTimeExtreme = roundTimers.getInt("extreme", 1200);
        }

        // Difficulty mode (BlockShuffle)
        try {
            ac.difficultyMode = DifficultyMode.valueOf(
                    section.getString("difficulty-mode", "PROGRESSIVE").toUpperCase());
        } catch (IllegalArgumentException e) {
            ac.difficultyMode = DifficultyMode.PROGRESSIVE;
        }
        ac.maxItemsPerGame = section.getInt("max-items-per-game", 0);

        // Game rules
        ConfigurationSection game = section.getConfigurationSection("game");
        if (game != null) {
            ac.pvpEnabled = game.getBoolean("pvp-enabled", true);
            ac.netherEnabled = game.getBoolean("nether-enabled", true);
            ac.endEnabled = game.getBoolean("end-enabled", true);
            ac.votingEnabled = game.getBoolean("voting-enabled", true);
            ac.voteTime = game.getInt("vote-time", 15);
            ac.lightningStart = game.getBoolean("lightning-start", false);
            ac.worldLoadEnabled = game.getBoolean("world-load-enabled", true);
            ac.worldUnloadEnabled = game.getBoolean("world-unload-enabled", false);
            ac.worldLoadCommand = game.getString("world-load-command", "mv load %world%");
            ac.worldUnloadCommand = game.getString("world-unload-command", "mv unload %world%");
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
        ac.forceStartDelay = section.getInt("force-start-delay", 30);
        ac.preventCancelAfterCountdown = section.getBoolean("prevent-cancel-after-countdown", false);
        try {
            ac.launchMode = LaunchMode.valueOf(section.getString("launch-mode", "MINIMUM").toUpperCase());
        } catch (IllegalArgumentException e) {
            ac.launchMode = LaunchMode.MINIMUM;
        }
        ac.debugMode = section.getBoolean("debug-mode", false);
        ac.customArenaSeedOnly = section.getBoolean("custom-arena-seed-only", false);
        try {
            ac.postGameAction = PostGameAction.valueOf(
                    section.getString("post-game-action", "MAIN_LOBBY").toUpperCase());
        } catch (IllegalArgumentException e) {
            ac.postGameAction = PostGameAction.MAIN_LOBBY;
        }
        ac.postGameCommand = section.getString("post-game-command", null);

        ac.blockShuffleRaceMode = section.getBoolean("game.blockshuffle-race-mode", false);
        ac.blockShuffleUniqueTargets = section.getBoolean("game.blockshuffle-unique-targets", true);
        ac.deathShuffleRaceMode = section.getBoolean("game.deathshuffle-race-mode", false);
        ac.deathShuffleUniqueCauses = section.getBoolean("game.deathshuffle-unique-causes", true);

        // Team settings
        ac.teamsEnabled = section.getBoolean("game.teams-enabled", false);
        ac.teamSize = section.getInt("game.teams-size", 0);
        ac.maxTeams = section.getInt("game.teams-max", 12);
        ac.teamAutoAssign = section.getBoolean("game.teams-auto-assign", true);

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
        config.set("game-world-nether", ac.gameWorldNether);
        config.set("game-world-end", ac.gameWorldEnd);
        config.set("lobby-world", ac.lobbyWorld);
        config.set("min-players", ac.minPlayers);
        config.set("max-players", ac.maxPlayers);
        config.set("ui-mode", ac.uiMode.name());

        config.set("timers.load-time", ac.loadTime);
        config.set("timers.swap-mode", ac.swapMode.name());
        config.set("timers.swap-interval", ac.swapInterval);
        config.set("timers.swap-min", ac.swapMin);
        config.set("timers.swap-max", ac.swapMax);

        config.set("timers.game-end-mode", ac.gameEndMode.name());
        config.set("timers.max-game-time", ac.maxGameTime);
        config.set("timers.max-rounds", ac.maxRounds);

        config.set("timers.spawn-protection", ac.spawnProtection);
        config.set("timers.spawn-radius", ac.spawnRadius);
        config.set("timers.min-player-distance", ac.minPlayerDistance);
        config.set("timers.rtp-max-retries", ac.rtpMaxRetries);
        config.set("timers.grace-period-buffer", ac.gracePeriodBuffer);
        config.set("timers.swap-blindness-duration", ac.swapBlindnessDuration);
        config.set("timers.end-game-delay", ac.endGameDelay);
        config.set("timers.challenge-reward-duration", ac.challengeRewardDuration);

        config.set("round-timers.easy", ac.roundTimeEasy);
        config.set("round-timers.medium", ac.roundTimeMedium);
        config.set("round-timers.hard", ac.roundTimeHard);
        config.set("round-timers.extreme", ac.roundTimeExtreme);

        config.set("difficulty-mode", ac.difficultyMode.name());
        config.set("max-items-per-game", ac.maxItemsPerGame);

        config.set("game.pvp-enabled", ac.pvpEnabled);
        config.set("game.nether-enabled", ac.netherEnabled);
        config.set("game.end-enabled", ac.endEnabled);
        config.set("game.voting-enabled", ac.votingEnabled);
        config.set("game.vote-time", ac.voteTime);
        config.set("game.lightning-start", ac.lightningStart);
        config.set("game.world-load-enabled", ac.worldLoadEnabled);
        config.set("game.world-unload-enabled", ac.worldUnloadEnabled);
        config.set("game.world-load-command", ac.worldLoadCommand);
        config.set("game.world-unload-command", ac.worldUnloadCommand);

        config.set("game.blockshuffle-race-mode", ac.blockShuffleRaceMode);
        config.set("game.blockshuffle-unique-targets", ac.blockShuffleUniqueTargets);
        config.set("game.deathshuffle-race-mode", ac.deathShuffleRaceMode);
        config.set("game.deathshuffle-unique-causes", ac.deathShuffleUniqueCauses);

        config.set("game.teams-enabled", ac.teamsEnabled);
        config.set("game.teams-size", ac.teamSize);
        config.set("game.teams-max", ac.maxTeams);
        config.set("game.teams-auto-assign", ac.teamAutoAssign);

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
        config.set("force-start-delay", ac.forceStartDelay);
        config.set("prevent-cancel-after-countdown", ac.preventCancelAfterCountdown);
        config.set("launch-mode", ac.launchMode.name());
        config.set("debug-mode", ac.debugMode);
        config.set("custom-arena-seed-only", ac.customArenaSeedOnly);
        config.set("post-game-action", ac.postGameAction.name());
        if (ac.postGameCommand != null) {
            config.set("post-game-command", ac.postGameCommand);
        }

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

    public enum LaunchMode {
        MINIMUM, MAXIMUM
    }

    public enum PostGameAction {
        MAIN_LOBBY, REJOIN
    }

    public enum GameEndMode {
        TIME, ROUNDS, UNLIMITED
    }

    /**
     * Holds all configuration values for a single arena.
     */
    public static class ArenaConfig {
        public String id;
        public GameType gameType = GameType.DEATHSWAP;

        // Worlds
        public String gameWorld;
        public String gameWorldNether;
        public String gameWorldEnd;
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

        // End Game settings
        public GameEndMode gameEndMode = GameEndMode.TIME;
        public int maxGameTime = 1800;
        public int maxRounds = 10;

        public int spawnProtection = 30;
        public int spawnRadius = 100;
        public int minPlayerDistance = 50;
        public int rtpMaxRetries = 10;
        public int gracePeriodBuffer = 15;
        public int swapBlindnessDuration = 3;
        public int endGameDelay = 5;
        public int challengeRewardDuration = 30;

        // Round timers (DeathShuffle / BlockShuffle)
        public int roundTimeEasy = 300; // 5 min
        public int roundTimeMedium = 600; // 10 min
        public int roundTimeHard = 900; // 15 min
        public int roundTimeExtreme = 1200; // 20 min

        // Difficulty mode (BlockShuffle only)
        public DifficultyMode difficultyMode = DifficultyMode.PROGRESSIVE;
        public int maxItemsPerGame = 0; // 0 = unlimited

        // Game rules
        public boolean pvpEnabled = true;
        public boolean netherEnabled = true;
        public boolean endEnabled = true;
        public boolean votingEnabled = true;
        public int voteTime = 15; // Per-arena vote duration in seconds
        public boolean lightningStart = false; // Ultra-fast start: no vote, no countdown

        // World management
        public boolean worldLoadEnabled = true; // Load worlds before CWR reset (required for CWR)
        public boolean worldUnloadEnabled = false; // Don't unload after game (CWR handles it)
        public String worldLoadCommand = "mv load %world%";
        public String worldUnloadCommand = "mv unload %world%";

        // Seeds
        public List<SeedEntry> seeds = new ArrayList<>();

        // Gamerules
        public Map<String, String> gamerules = new HashMap<>();

        // Resilience
        public int forceStartDelay = 30; // 0 = disabled, otherwise seconds to wait before force start
        public boolean preventCancelAfterCountdown = false;
        public LaunchMode launchMode = LaunchMode.MINIMUM;
        public PostGameAction postGameAction = PostGameAction.MAIN_LOBBY;
        // Custom command for MAIN_LOBBY action (null = use built-in mvtp). Supports
        // %player% placeholder.
        public String postGameCommand = null;
        public boolean debugMode = false;
        public boolean customArenaSeedOnly = false;

        // Block Shuffle
        public boolean blockShuffleRaceMode = false;
        public boolean blockShuffleUniqueTargets = true;

        // Death Shuffle
        public boolean deathShuffleRaceMode = false;
        public boolean deathShuffleUniqueCauses = true;

        // Teams
        public boolean teamsEnabled = false;
        public int teamSize = 0;           // 0 = dynamic (auto-calculated)
        public int maxTeams = 12;          // Maximum number of teams (max 12 wool colors)
        public boolean teamAutoAssign = true; // Auto-assign unassigned players at game start

        // Command overrides (null = use global default)
        public String teleportCommand = null;
        public List<String> worldResetCommands = null;

        public ArenaConfig() {
            // Default gamerules (1.21+ snake_case names) — DeathSwap preset
            gamerules.put("keep_inventory", "false");
            gamerules.put("immediate_respawn", "true");
            gamerules.put("respawn_radius", "0");
            gamerules.put("send_command_feedback", "false");
            gamerules.put("log_admin_commands", "false");
            gamerules.put("random_tick_speed", "3");
            gamerules.put("show_advancement_messages", "true");
            gamerules.put("advance_time", "true");
            gamerules.put("advance_weather", "true");
            gamerules.put("mob_griefing", "true");
            gamerules.put("show_death_messages", "true");
            gamerules.put("natural_health_regeneration", "false");
            gamerules.put("reduced_debug_info", "false");
            gamerules.put("spawn_mobs", "true");
            gamerules.put("spawn_monsters", "true");
            gamerules.put("spawn_phantoms", "true");
            gamerules.put("spawn_wandering_traders", "true");
            gamerules.put("spawn_wardens", "true");
            gamerules.put("spawner_blocks_work", "true");
            gamerules.put("entity_drops", "true");
            gamerules.put("mob_drops", "true");
            gamerules.put("block_drops", "true");
            gamerules.put("drowning_damage", "true");
            gamerules.put("fall_damage", "true");
            gamerules.put("fire_damage", "true");
            gamerules.put("freeze_damage", "true");
            gamerules.put("locatorBar", "false"); // Disable player locator bar (1.21.6+)
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
            switch (difficulty) {
                case 1:
                    return roundTimeEasy;
                case 2:
                    return roundTimeMedium;
                case 3:
                    return roundTimeHard;
                case 4:
                    return roundTimeExtreme;
                default:
                    return roundTimeEasy;
            }
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

    /**
     * Entry for a block shuffle target with enabled, difficulty and type.
     */
    public record BlockShuffleEntry(String material, boolean enabled, int difficulty, String type) {
    }

    public static class BlockShuffleConfig {
        private final FileConfiguration config;
        private final File file;
        private final List<BlockShuffleEntry> entries = new ArrayList<>();

        public BlockShuffleConfig(FileConfiguration config, File file) {
            this.config = config;
            this.file = file;
            if (load()) {
                save();
            }
        }

        private boolean load() {
            boolean changed = false;
            Map<String, BlockShuffleEntry> entryMap = new java.util.LinkedHashMap<>();
            ConfigurationSection blocks = config.getConfigurationSection("blocks");
            if (blocks != null) {
                for (String key : blocks.getKeys(false)) {
                    ConfigurationSection sec = blocks.getConfigurationSection(key);
                    if (sec != null) {
                        boolean enabled = sec.getBoolean("enabled", true);
                        int difficulty = sec.getInt("difficulty", 1);
                        String type = sec.getString("type", "STAND");
                        String matName = key.toUpperCase();
                        entryMap.put(matName, new BlockShuffleEntry(matName, enabled, difficulty, type));
                    }
                }
            }
            entries.clear();
            entries.addAll(entryMap.values());

            // Sync with Bukkit's current Material enum to catch new version blocks/items
            java.util.Set<String> existing = entries.stream()
                    .map(e -> e.material().toUpperCase())
                    .collect(java.util.stream.Collectors.toSet());

            for (org.bukkit.Material mat : org.bukkit.Material.values()) {
                if (mat.isLegacy() || mat.isAir() || !mat.isItem())
                    continue; // Skip unobtainable/technical

                String name = mat.name();
                // Filter out obviously unobtainable survival items
                if (name.contains("COMMAND_BLOCK") || name.contains("SPAWN_EGG") ||
                        name.endsWith("_SPAWNER") || name.equals("SPAWNER") ||
                        name.equals("BARRIER") || name.equals("STRUCTURE_BLOCK") ||
                        name.equals("STRUCTURE_VOID") || name.equals("JIGSAW") ||
                        name.equals("LIGHT") || name.equals("BEDROCK") ||
                        name.equals("KNOWLEDGE_BOOK") || name.equals("DEBUG_STICK") ||
                        name.equals("DRAGON_EGG") || name.equals("END_PORTAL_FRAME")) {
                    continue;
                }

                if (!existing.contains(mat.name())) {
                    // Randomize difficulty 1 or 2 as default for new discovered items
                    int diff = java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? 1 : 2;
                    // Only enable easy/medium by default
                    boolean enable = (diff <= 2);
                    entries.add(new BlockShuffleEntry(mat.name(), enable, diff, "STAND"));
                    changed = true;
                }
            }
            return changed;
        }

        public List<BlockShuffleEntry> getEntries() {
            return entries;
        }

        public void setEnabled(String material, boolean enabled) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).material().equalsIgnoreCase(material)) {
                    BlockShuffleEntry old = entries.get(i);
                    entries.set(i, new BlockShuffleEntry(old.material(), enabled, old.difficulty(), old.type()));
                    break;
                }
            }
        }

        public void setDifficulty(String material, int difficulty) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).material().equalsIgnoreCase(material)) {
                    BlockShuffleEntry old = entries.get(i);
                    entries.set(i, new BlockShuffleEntry(old.material(), old.enabled(), difficulty, old.type()));
                    break;
                }
            }
        }

        public void save() {
            config.set("blocks", null); // Clear to avoid duplicates with different casing
            for (BlockShuffleEntry entry : entries) {
                String path = "blocks." + entry.material();
                config.set(path + ".enabled", entry.enabled());
                config.set(path + ".difficulty", entry.difficulty());
                config.set(path + ".type", entry.type());
            }
            try {
                config.save(file);
            } catch (IOException e) {
                // Logged by caller
            }
        }

        public FileConfiguration getConfig() {
            return config;
        }
    }

    /**
     * Entry for a death shuffle cause with enabled and difficulty.
     */
    public record DeathShuffleEntry(String cause, boolean enabled, int difficulty) {
    }

    public static class DeathShuffleConfig {
        private final FileConfiguration config;
        private final File file;
        private final List<DeathShuffleEntry> entries = new ArrayList<>();

        public DeathShuffleConfig(FileConfiguration config, File file) {
            this.config = config;
            this.file = file;
            if (load()) {
                save();
            }
        }

        private boolean load() {
            boolean changed = false;
            Map<String, DeathShuffleEntry> entryMap = new java.util.LinkedHashMap<>();
            ConfigurationSection causes = config.getConfigurationSection("causes");
            if (causes != null) {
                for (String key : causes.getKeys(false)) {
                    ConfigurationSection sec = causes.getConfigurationSection(key);
                    if (sec != null) {
                        boolean enabled = sec.getBoolean("enabled", true);
                        int difficulty = sec.getInt("difficulty", 1);
                        String causeName = key.toUpperCase();
                        entryMap.put(causeName, new DeathShuffleEntry(causeName, enabled, difficulty));
                    }
                }
            }
            entries.clear();
            entries.addAll(entryMap.values());

            // Sync with Bukkit's current DamageCause enum to catch new version causes
            java.util.Set<String> existing = entries.stream()
                    .map(e -> e.cause().toUpperCase())
                    .collect(java.util.stream.Collectors.toSet());

            for (org.bukkit.event.entity.EntityDamageEvent.DamageCause dc : org.bukkit.event.entity.EntityDamageEvent.DamageCause
                    .values()) {
                if (!existing.contains(dc.name())) {
                    // Default missing causes to difficulty 1 & disabled
                    entries.add(new DeathShuffleEntry(dc.name(), false, 1));
                    changed = true;
                }
            }
            return changed;
        }

        public List<DeathShuffleEntry> getEntries() {
            return entries;
        }

        public void setEnabled(String cause, boolean enabled) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).cause().equalsIgnoreCase(cause)) {
                    DeathShuffleEntry old = entries.get(i);
                    entries.set(i, new DeathShuffleEntry(old.cause(), enabled, old.difficulty()));
                    break;
                }
            }
        }

        public void setDifficulty(String cause, int difficulty) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).cause().equalsIgnoreCase(cause)) {
                    DeathShuffleEntry old = entries.get(i);
                    entries.set(i, new DeathShuffleEntry(old.cause(), old.enabled(), difficulty));
                    break;
                }
            }
        }

        public void save() {
            config.set("causes", null); // Clear to avoid duplicates with different casing
            for (DeathShuffleEntry entry : entries) {
                String path = "causes." + entry.cause();
                config.set(path + ".enabled", entry.enabled());
                config.set(path + ".difficulty", entry.difficulty());
            }
            try {
                config.save(file);
            } catch (IOException e) {
                // Logged by caller
            }
        }

        public FileConfiguration getConfig() {
            return config;
        }
    }

}
