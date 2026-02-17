package be.dualsfwshield.deathswap;

import be.dualsfwshield.deathswap.modes.BlockShuffleInstance;
import be.dualsfwshield.deathswap.modes.DeathShuffleInstance;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of all active GameInstance objects.
 * Maps arena IDs to their game instances and tracks which arena each player is
 * in.
 * Handles instantiation of specific game mode classes based on config.
 */
public class ArenaManager {

    private final DeathSwapPlugin plugin;
    private final Map<String, GameInstance> arenas = new HashMap<>();
    private final Map<Player, String> playerArenaMap = new HashMap<>();

    public ArenaManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
        initArenas();
    }

    /**
     * Initialize all arenas from config.
     */
    private void initArenas() {
        arenas.clear();
        for (String arenaId : plugin.getConfigManager().getArenaIds()) {
            ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
            GameInstance instance;

            switch (config.gameType) {
                case DEATHSHUFFLE:
                    instance = new DeathShuffleInstance(plugin, arenaId, config);
                    break;
                case BLOCKSHUFFLE:
                    instance = new BlockShuffleInstance(plugin, arenaId, config);
                    break;
                case DEATHSWAP:
                default:
                    instance = new GameInstance(plugin, arenaId, config);
                    break;
            }

            arenas.put(arenaId, instance);
        }
        plugin.getLogger().info("Initialized " + arenas.size() + " arena(s).");
    }

    /**
     * Reload all arenas from config. Stops running games first.
     */
    public void reload() {
        for (GameInstance instance : arenas.values()) {
            if (instance.getState() == GameState.RUNNING || instance.getState() == GameState.STARTING) {
                instance.stopGame();
            }
        }
        playerArenaMap.clear();
        initArenas();
    }

    /**
     * Get a game instance by arena ID.
     */
    public GameInstance getArena(String arenaId) {
        return arenas.get(arenaId);
    }

    /**
     * Get all arena IDs.
     */
    public Collection<String> getArenaIds() {
        return arenas.keySet();
    }

    /**
     * Get all game instances.
     */
    public Collection<GameInstance> getAllArenas() {
        return arenas.values();
    }

    /**
     * Find the arena a player is currently in (lobby or game).
     */
    public GameInstance getPlayerArena(Player player) {
        String arenaId = playerArenaMap.get(player);
        if (arenaId == null)
            return null;
        return arenas.get(arenaId);
    }

    /**
     * Get the arena ID a player is currently in.
     */
    public String getPlayerArenaId(Player player) {
        return playerArenaMap.get(player);
    }

    /**
     * Register a player as being in an arena.
     */
    public void addPlayerToArena(Player player, String arenaId) {
        playerArenaMap.put(player, arenaId);
    }

    /**
     * Remove a player from their current arena.
     */
    public void removePlayer(Player player) {
        playerArenaMap.remove(player);
    }

    /**
     * Find an arena by its lobby world name.
     */
    public GameInstance findByLobbyWorld(String worldName) {
        for (GameInstance instance : arenas.values()) {
            if (instance.getConfig().lobbyWorld.equals(worldName)) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Find an arena by its game world name.
     */
    public GameInstance findByGameWorld(String worldName) {
        for (GameInstance instance : arenas.values()) {
            if (instance.getConfig().gameWorld.equals(worldName)) {
                return instance;
            }
        }
        return null;
    }
}
