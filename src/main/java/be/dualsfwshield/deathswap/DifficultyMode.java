package be.dualsfwshield.deathswap;

/**
 * Controls how difficulty is selected each round in BlockShuffle.
 */
public enum DifficultyMode {
    /** Default: rounds 1-3 easy, 4-6 medium, 7-9 hard, 10+ extreme. */
    PROGRESSIVE,
    /** All items are Easy (difficulty 1). */
    THEMATIC_EASY,
    /** All items are Medium (difficulty 2). */
    THEMATIC_MEDIUM,
    /** All items are Hard (difficulty 3). */
    THEMATIC_HARD,
    /** All items are Extreme (difficulty 4). */
    THEMATIC_EXTREME,
    /** Random difficulty each round, but both players share the same sequence. */
    RANDOM,
    /** Predefined pattern: 2 easy, 1 medium, repeating. Avoids long sessions. */
    BALANCED
}
