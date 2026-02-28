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
            be.dualsfwshield.deathswap.api.DeathSwapAPI.GameInstanceFactory factory = be.dualsfwshield.deathswap.api.DeathSwapAPI
                    .getFactory(config.gameType.name());

            if (factory != null) {
                instance = factory.create(plugin, arenaId, config);
            } else if (config.gameType.equals(GameType.DEATHSHUFFLE)) {
                instance = new DeathShuffleInstance(plugin, arenaId, config);
            } else if (config.gameType.equals(GameType.BLOCKSHUFFLE)) {
                instance = new BlockShuffleInstance(plugin, arenaId, config);
            } else {
                instance = new GameInstance(plugin, arenaId, config);
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
     * Reload a specific arena. Stops the running game if any, kicks players,
     * re-instantiates the correct GameInstance subclass based on config,
     * and updates the arena map.
     */
    public void reloadArena(String arenaId) {
        GameInstance existing = arenas.get(arenaId);
        if (existing != null) {
            if (existing.getState() == GameState.RUNNING || existing.getState() == GameState.STARTING) {
                existing.stopGame();
            }
            // Kick all players in lobby too from map
            for (Player p : existing.getAllPlayers()) {
                playerArenaMap.remove(p);
            }
        }

        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            arenas.remove(arenaId);
            return; // Arena was deleted
        }

        GameInstance instance;
        be.dualsfwshield.deathswap.api.DeathSwapAPI.GameInstanceFactory factory = be.dualsfwshield.deathswap.api.DeathSwapAPI
                .getFactory(config.gameType.name());

        if (factory != null) {
            instance = factory.create(plugin, arenaId, config);
        } else if (config.gameType.equals(GameType.DEATHSHUFFLE)) {
            instance = new DeathShuffleInstance(plugin, arenaId, config);
        } else if (config.gameType.equals(GameType.BLOCKSHUFFLE)) {
            instance = new BlockShuffleInstance(plugin, arenaId, config);
        } else {
            instance = new GameInstance(plugin, arenaId, config);
        }

        arenas.put(arenaId, instance);
        plugin.getLogger().info("Reloaded arena " + arenaId + ".");
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

    /**
     * Find an arena by a dedicated dimension world name (nether/end).
     * Matches worlds named {gameWorld}_nether or {gameWorld}_the_end.
     */
    public GameInstance findByDimensionWorld(String worldName) {
        for (GameInstance instance : arenas.values()) {
            String base = instance.getConfig().gameWorld;
            if (worldName.equals(base + "_nether") || worldName.equals(base + "_the_end")) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Find an arena by any associated world (game, nether, or end).
     */
    public GameInstance findByAnyGameWorld(String worldName) {
        GameInstance arena = findByGameWorld(worldName);
        if (arena == null) {
            arena = findByDimensionWorld(worldName);
        }
        return arena;
    }
}
