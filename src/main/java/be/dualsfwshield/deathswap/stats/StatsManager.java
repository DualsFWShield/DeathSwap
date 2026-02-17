package be.dualsfwshield.deathswap.stats;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages loading, saving, and querying player statistics.
 * Stores data in plugins/DeathSwap/stats.yml.
 */
public class StatsManager {

    private final DeathSwapPlugin plugin;
    private final File statsFile;
    private YamlConfiguration statsConfig;
    private final Map<UUID, PlayerStats> statsMap = new HashMap<>();
    private BukkitTask autoSaveTask;

    public StatsManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        load();
        startAutoSave();
    }

    /**
     * Load stats from disk.
     */
    public void load() {
        if (!statsFile.exists()) {
            try {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create stats.yml: " + e.getMessage());
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        statsMap.clear();

        ConfigurationSection players = statsConfig.getConfigurationSection("players");
        if (players == null)
            return;

        for (String uuidStr : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection ps = players.getConfigurationSection(uuidStr);
                if (ps == null)
                    continue;

                PlayerStats stats = new PlayerStats(uuid);
                stats.setLastKnownName(ps.getString("name", "Unknown"));
                for (int i = 0; i < ps.getInt("kills", 0); i++)
                    stats.addKill();
                for (int i = 0; i < ps.getInt("deaths", 0); i++)
                    stats.addDeath();
                for (int i = 0; i < ps.getInt("wins", 0); i++)
                    stats.addWin();
                for (int i = 0; i < ps.getInt("games-played", 0); i++)
                    stats.addGamePlayed();
                stats.addSurvivalTime(ps.getLong("survival-time", 0));
                stats.setLastPlayed(ps.getLong("last-played", 0));

                statsMap.put(uuid, stats);
            } catch (IllegalArgumentException ignored) {
            }
        }

        plugin.getLogger().info("Loaded stats for " + statsMap.size() + " player(s).");
    }

    /**
     * Save stats to disk.
     */
    public void save() {
        for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerStats s = entry.getValue();
            statsConfig.set(path + ".name", s.getLastKnownName());
            statsConfig.set(path + ".kills", s.getKills());
            statsConfig.set(path + ".deaths", s.getDeaths());
            statsConfig.set(path + ".wins", s.getWins());
            statsConfig.set(path + ".games-played", s.getGamesPlayed());
            statsConfig.set(path + ".survival-time", s.getTotalSurvivalTime());
            statsConfig.set(path + ".last-played", s.getLastPlayed());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save stats.yml: " + e.getMessage());
        }
    }

    /**
     * Start automatic saving.
     */
    private void startAutoSave() {
        int minutes = plugin.getConfigManager().getStatsAutoSaveMinutes();
        if (minutes <= 0)
            minutes = 5;
        long ticks = minutes * 60L * 20L;
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::save, ticks, ticks);
    }

    /**
     * Stop auto-save and save final state.
     */
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        save();
    }

    /**
     * Get or create stats for a player.
     */
    public PlayerStats getStats(UUID uuid) {
        return statsMap.computeIfAbsent(uuid, PlayerStats::new);
    }

    /**
     * Record a kill for the killer.
     */
    public void addKill(UUID killer, String name) {
        PlayerStats s = getStats(killer);
        s.setLastKnownName(name);
        s.addKill();
    }

    /**
     * Record a death.
     */
    public void addDeath(UUID player, String name) {
        PlayerStats s = getStats(player);
        s.setLastKnownName(name);
        s.addDeath();
    }

    /**
     * Record a win.
     */
    public void addWin(UUID player, String name) {
        PlayerStats s = getStats(player);
        s.setLastKnownName(name);
        s.addWin();
    }

    /**
     * Record a new game played.
     */
    public void addGamePlayed(UUID player, String name) {
        PlayerStats s = getStats(player);
        s.setLastKnownName(name);
        s.addGamePlayed();
        s.setLastPlayed(System.currentTimeMillis());
    }

    /**
     * Add survival time in seconds.
     */
    public void addSurvivalTime(UUID player, long seconds) {
        getStats(player).addSurvivalTime(seconds);
    }

    /**
     * Get top players sorted by a specific stat.
     *
     * @param stat  stat name (kills, wins, deaths, time, games)
     * @param limit max results
     */
    public List<PlayerStats> getTopPlayers(String stat, int limit) {
        List<PlayerStats> sorted = new ArrayList<>(statsMap.values());
        sorted.sort(Comparator.comparingLong((PlayerStats s) -> s.getStat(stat)).reversed());
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }
}
