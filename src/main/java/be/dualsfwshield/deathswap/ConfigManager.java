package be.dualsfwshield.deathswap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages loading, saving, and accessing the plugin configuration.
 * Supports multi-arena configs with per-arena settings, per-mode prefixes,
 * and feature toggles (stats, sounds, challenges, voting).
 */
public class ConfigManager {

    private final DeathSwapPlugin plugin;

    // Global
    private String hubWorld;
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

    public ConfigManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Load or reload all configuration from config.yml.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.hubWorld = config.getString("hub-world", "MainLobby");

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

        // Load arenas
        arenaConfigs.clear();
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.getLogger().warning("No arenas defined in config.yml!");
            return;
        }

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(arenaId);
            if (section == null)
                continue;
            arenaConfigs.put(arenaId, loadArenaConfig(arenaId, section));
        }

        plugin.getLogger().info("Loaded " + arenaConfigs.size() + " arena(s) from config.");
    }

    private ArenaConfig loadArenaConfig(String id, ConfigurationSection section) {
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

        // DeathRun config (unused for now, reserved)
        ConfigurationSection deathrun = section.getConfigurationSection("deathrun");
        if (deathrun != null) {
            ac.runnerCount = deathrun.getInt("runner-count", 1);
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

        return ac;
    }

    /**
     * Save current arena configs back to config.yml.
     */
    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("hub-world", hubWorld);

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

        for (Map.Entry<String, ArenaConfig> entry : arenaConfigs.entrySet()) {
            String path = "arenas." + entry.getKey();
            ArenaConfig ac = entry.getValue();

            config.set(path + ".game-type", ac.gameType.name());
            config.set(path + ".game-world", ac.gameWorld);
            config.set(path + ".lobby-world", ac.lobbyWorld);
            config.set(path + ".min-players", ac.minPlayers);
            config.set(path + ".max-players", ac.maxPlayers);
            config.set(path + ".ui-mode", ac.uiMode.name());

            config.set(path + ".timers.load-time", ac.loadTime);
            config.set(path + ".timers.swap-mode", ac.swapMode.name());
            config.set(path + ".timers.swap-interval", ac.swapInterval);
            config.set(path + ".timers.swap-min", ac.swapMin);
            config.set(path + ".timers.swap-max", ac.swapMax);
            config.set(path + ".timers.max-game-time", ac.maxGameTime);
            config.set(path + ".timers.spawn-protection", ac.spawnProtection);

            config.set(path + ".round-timers.easy", ac.roundTimeEasy);
            config.set(path + ".round-timers.medium", ac.roundTimeMedium);
            config.set(path + ".round-timers.hard", ac.roundTimeHard);

            config.set(path + ".game.pvp-enabled", ac.pvpEnabled);
            config.set(path + ".game.nether-enabled", ac.netherEnabled);
            config.set(path + ".game.end-enabled", ac.endEnabled);

            ConfigurationSection rulesSection = config.createSection(path + ".gamerules");
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
            config.set(path + ".seeds", seedMaps);
        }

        plugin.saveConfig();
    }

    // --- Getters ---

    public String getHubWorld() {
        return hubWorld;
    }

    public String getPrefix(GameType type) {
        return prefixes.getOrDefault(type, type.getDefaultPrefix());
    }

    /** @deprecated Use getPrefix(GameType) instead */
    @Deprecated
    public String getPrefix() {
        return getPrefix(GameType.DEATHSWAP);
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

        // DeathRun (reserved)
        public int runnerCount = 1;

        // Seeds
        public List<SeedEntry> seeds = new ArrayList<>();

        // Gamerules
        public Map<String, String> gamerules = new HashMap<>();

        public ArenaConfig() {
            // Default gamerules
            gamerules.put("keepInventory", "false");
            gamerules.put("immediateRespawn", "true");
            gamerules.put("respawnRadius", "0");
            gamerules.put("sendCommandFeedback", "false");
            gamerules.put("logAdminCommands", "false");
            gamerules.put("doDaylightCycle", "true");
            gamerules.put("doWeatherCycle", "true");
            gamerules.put("mobGriefing", "true");
            gamerules.put("naturalRegeneration", "true");
            gamerules.put("doMobSpawning", "true");
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

}
