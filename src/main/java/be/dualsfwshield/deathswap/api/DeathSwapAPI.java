package be.dualsfwshield.deathswap.api;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameType;

import java.util.HashMap;
import java.util.Map;

/**
 * API for registering custom Game Modes in DeathSwap.
 */
public class DeathSwapAPI {

    private static final Map<String, GameInstanceFactory> CUSTOM_FACTORIES = new HashMap<>();

    /**
     * Registers a new custom game mode.
     * 
     * @param id            The internal identifier (e.g., "MYMODE")
     * @param displayName   The display name (e.g., "My Custom Mode")
     * @param defaultPrefix The prefix in chat (e.g., "&8[&aMyMode&8]")
     * @param factory       The factory providing the GameInstance subclass
     */
    public static void registerMode(String id, String displayName, String defaultPrefix, GameInstanceFactory factory) {
        GameType type = new GameType(id, displayName, defaultPrefix);
        GameType.register(type);
        CUSTOM_FACTORIES.put(type.name(), factory);
    }

    /**
     * Internal framework method. Retrieves the factory for a specific mode name.
     */
    public static GameInstanceFactory getFactory(String typeName) {
        return CUSTOM_FACTORIES.get(typeName.toUpperCase());
    }

    /**
     * Factory interface for instantiating custom GameInstances.
     */
    @FunctionalInterface
    public interface GameInstanceFactory {
        GameInstance create(DeathSwapPlugin plugin, String arenaId, ConfigManager.ArenaConfig config);
    }
}
