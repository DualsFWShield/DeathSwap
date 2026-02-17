package be.dualsfwshield.deathswap.modes;

import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Death causes for DeathShuffle mode, ordered by difficulty.
 * Maps to Bukkit's DamageCause enum.
 */
public enum DeathCause {

    // Difficulty 1 — Easy
    DROWNING("Noyade", 1, EntityDamageEvent.DamageCause.DROWNING, "Noie-toi !"),
    FALL("Chute", 1, EntityDamageEvent.DamageCause.FALL, "Meurs d'une chute !"),
    FIRE("Feu", 1, EntityDamageEvent.DamageCause.FIRE_TICK, "Meurs dans les flammes !"),
    CONTACT("Cactus", 1, EntityDamageEvent.DamageCause.CONTACT, "Meurs par un cactus !"),
    STARVATION("Famine", 1, EntityDamageEvent.DamageCause.STARVATION, "Meurs de faim !"),
    SUFFOCATION("Suffocation", 1, EntityDamageEvent.DamageCause.SUFFOCATION, "Étouffe dans un bloc !"),

    // Difficulty 2 — Medium
    LAVA("Lave", 2, EntityDamageEvent.DamageCause.LAVA, "Meurs dans la lave !"),
    EXPLOSION("Explosion", 2, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, "Meurs d'une explosion !"),
    PROJECTILE("Projectile", 2, EntityDamageEvent.DamageCause.PROJECTILE, "Meurs par un projectile !"),
    MAGIC("Magie", 2, EntityDamageEvent.DamageCause.MAGIC, "Meurs par la magie !"),
    HOT_FLOOR("Magma", 2, EntityDamageEvent.DamageCause.HOT_FLOOR, "Meurs sur du magma !"),
    FREEZE("Gel", 2, EntityDamageEvent.DamageCause.FREEZE, "Meurs de froid !"),

    // Difficulty 3 — Hard
    LIGHTNING("Foudre", 3, EntityDamageEvent.DamageCause.LIGHTNING, "Meurs foudroyé !"),
    FLY_INTO_WALL("Mur", 3, EntityDamageEvent.DamageCause.FLY_INTO_WALL, "Vole dans un mur !"),
    FALLING_BLOCK("Enclume/Bloc", 3, EntityDamageEvent.DamageCause.FALLING_BLOCK, "Meurs sous un bloc !"),
    VOID("Vide", 3, EntityDamageEvent.DamageCause.VOID, "Tombe dans le vide !");

    private final String displayName;
    private final int difficulty;
    private final EntityDamageEvent.DamageCause damageCause;
    private final String challenge;

    DeathCause(String displayName, int difficulty, EntityDamageEvent.DamageCause cause, String challenge) {
        this.displayName = displayName;
        this.difficulty = difficulty;
        this.damageCause = cause;
        this.challenge = challenge;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public EntityDamageEvent.DamageCause getDamageCause() {
        return damageCause;
    }

    public String getChallenge() {
        return challenge;
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
