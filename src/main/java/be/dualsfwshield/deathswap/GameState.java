package be.dualsfwshield.deathswap;

/**
 * Represents the current state of a DeathSwap game instance.
 */
public enum GameState {
    WAITING, // Lobby, waiting for players to ready up
    STARTING, // World generation & countdown in progress
    RUNNING, // Game is active
    ENDED, // Game finished, cleaning up
    DISABLED // Arena disabled by admin
}
