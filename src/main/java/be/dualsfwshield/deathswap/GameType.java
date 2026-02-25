package be.dualsfwshield.deathswap;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Available game modes for arenas.
 * Previously an enum, now an extensible class to support the custom API.
 */
public final class GameType {

    private static final Map<String, GameType> REGISTERED_TYPES = new LinkedHashMap<>();

    public static final GameType DEATHSWAP = register(new GameType("DEATHSWAP", "DeathSwap", "&8[&6DeathSwap&8]"));
    public static final GameType DEATHSHUFFLE = register(
            new GameType("DEATHSHUFFLE", "DeathShuffle", "&8[&dDeathShuffle&8]"));
    public static final GameType BLOCKSHUFFLE = register(
            new GameType("BLOCKSHUFFLE", "BlockShuffle", "&8[&bBlockShuffle&8]"));

    private static int NEXT_ORDINAL = 0;

    private final String name;
    private final String displayName;
    private final String defaultPrefix;
    private final int ordinal;

    /**
     * Internal constructor. Use
     * {@link be.dualsfwshield.deathswap.api.DeathSwapAPI#registerMode(String, String, String, be.dualsfwshield.deathswap.api.DeathSwapAPI.GameInstanceFactory)}
     * to add a custom mode.
     */
    public GameType(String name, String displayName, String defaultPrefix) {
        this.name = name.toUpperCase();
        this.displayName = displayName;
        this.defaultPrefix = defaultPrefix;
        this.ordinal = NEXT_ORDINAL++;
    }

    public static GameType register(GameType type) {
        REGISTERED_TYPES.put(type.name(), type);
        return type;
    }

    public static GameType valueOf(String name) {
        GameType type = REGISTERED_TYPES.get(name.toUpperCase());
        if (type == null) {
            throw new IllegalArgumentException("No GameType with name " + name);
        }
        return type;
    }

    public static GameType[] values() {
        return REGISTERED_TYPES.values().toArray(new GameType[0]);
    }

    public static Collection<GameType> getAllPreRegistered() {
        return REGISTERED_TYPES.values();
    }

    public String name() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultPrefix() {
        return defaultPrefix;
    }

    public int ordinal() {
        return ordinal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return name.equals(((GameType) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
