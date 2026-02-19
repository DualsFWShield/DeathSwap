package be.dualsfwshield.deathswap.modes;

import be.dualsfwshield.deathswap.util.Lang;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Death causes for DeathShuffle mode, ordered by difficulty.
 * Maps to Bukkit's DamageCause enum.
 * Display names and challenges are localized via Lang keys.
 */
public enum DeathCause {

    // Difficulty 1 — Easy
    DROWNING(1, EntityDamageEvent.DamageCause.DROWNING),
    FALL(1, EntityDamageEvent.DamageCause.FALL),
    FIRE(1, EntityDamageEvent.DamageCause.FIRE_TICK),
    CONTACT(1, EntityDamageEvent.DamageCause.CONTACT),
    STARVATION(1, EntityDamageEvent.DamageCause.STARVATION),
    SUFFOCATION(1, EntityDamageEvent.DamageCause.SUFFOCATION),

    // Difficulty 2 — Medium
    LAVA(2, EntityDamageEvent.DamageCause.LAVA),
    EXPLOSION(2, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION),
    PROJECTILE(2, EntityDamageEvent.DamageCause.PROJECTILE),
    MAGIC(2, EntityDamageEvent.DamageCause.MAGIC),
    HOT_FLOOR(2, EntityDamageEvent.DamageCause.HOT_FLOOR),
    FREEZE(2, EntityDamageEvent.DamageCause.FREEZE),

    // Difficulty 3 — Hard
    LIGHTNING(3, EntityDamageEvent.DamageCause.LIGHTNING),
    FLY_INTO_WALL(3, EntityDamageEvent.DamageCause.FLY_INTO_WALL),
    FALLING_BLOCK(3, EntityDamageEvent.DamageCause.FALLING_BLOCK),
    VOID(3, EntityDamageEvent.DamageCause.VOID);

    private final int difficulty;
    private final EntityDamageEvent.DamageCause damageCause;

    DeathCause(int difficulty, EntityDamageEvent.DamageCause cause) {
        this.difficulty = difficulty;
        this.damageCause = cause;
    }

    /**
     * Get the localized display name from lang files.
     * Key format: death-cause-NAME-name (e.g. death-cause-drowning-name)
     */
    public String getDisplayName() {
        return Lang.get("death-cause-" + name().toLowerCase().replace("_", "-") + "-name");
    }

    public int getDifficulty() {
        return difficulty;
    }

    public EntityDamageEvent.DamageCause getDamageCause() {
        return damageCause;
    }

    /**
     * Get the localized challenge text from lang files.
     * Key format: death-cause-NAME-challenge (e.g. death-cause-drowning-challenge)
     */
    public String getChallenge() {
        return Lang.get("death-cause-" + name().toLowerCase().replace("_", "-") + "-challenge");
    }

    /**
     * Get all causes for a specific difficulty tier.
     */
    public static DeathCause[] getByDifficulty(int difficulty) {
        java.util.List<DeathCause> list = new java.util.ArrayList<>();
        for (DeathCause dc : values()) {
            if (dc.difficulty == difficulty)
                list.add(dc);
        }
        return list.toArray(new DeathCause[0]);
    }

    /**
     * Find a DeathCause matching a Bukkit DamageCause.
     */
    public static DeathCause fromDamageCause(EntityDamageEvent.DamageCause cause) {
        for (DeathCause dc : values()) {
            if (dc.damageCause == cause)
                return dc;
        }
        return null;
    }
}
