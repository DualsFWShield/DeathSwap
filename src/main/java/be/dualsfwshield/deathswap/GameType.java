package be.dualsfwshield.deathswap;

/**
 * Available game modes for arenas.
 */
public enum GameType {
    DEATHSWAP("DeathSwap", "&8[&6DeathSwap&8]"),
    DEATHSHUFFLE("DeathShuffle", "&8[&dDeathShuffle&8]"),
    BLOCKSHUFFLE("BlockShuffle", "&8[&bBlockShuffle&8]");

    private final String displayName;
    private final String defaultPrefix;

    GameType(String displayName, String defaultPrefix) {
        this.displayName = displayName;
        this.defaultPrefix = defaultPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultPrefix() {
        return defaultPrefix;
    }
}
