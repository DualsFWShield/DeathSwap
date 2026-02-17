package be.dualsfwshield.deathswap.challenges;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

/**
 * Represents a single challenge that can be assigned between swaps.
 */
public class Challenge {

    public enum ChallengeType {
        CRAFT, MINE, KILL
    }

    private final ChallengeType type;
    private final String target; // Material name or EntityType name
    private final int amount;
    private final String rewardEffect; // PotionEffectType name
    private final String description;

    public Challenge(ChallengeType type, String target, int amount, String rewardEffect, String description) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.rewardEffect = rewardEffect;
        this.description = description;
    }

    public ChallengeType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getAmount() {
        return amount;
    }

    public String getRewardEffect() {
        return rewardEffect;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Try to get the Material for CRAFT/MINE challenges.
     */
    public Material getTargetMaterial() {
        try {
            return Material.valueOf(target.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Try to get the EntityType for KILL challenges.
     */
    public EntityType getTargetEntity() {
        try {
            return EntityType.valueOf(target.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get the reward PotionEffectType or null.
     */
    public PotionEffectType getRewardPotionEffect() {
        try {
            return PotionEffectType.getByName(rewardEffect.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
