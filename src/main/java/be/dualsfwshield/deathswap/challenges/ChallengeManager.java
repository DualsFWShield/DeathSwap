package be.dualsfwshield.deathswap.challenges;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages challenge assignment and completion during DeathSwap games.
 * Challenges are assigned at each swap and must be completed before the next
 * swap.
 */
public class ChallengeManager {

    private final DeathSwapPlugin plugin;
    private final List<Challenge> availableChallenges = new ArrayList<>();

    // Active challenges per player: UUID -> Challenge
    private final Map<UUID, Challenge> activeChallenges = new HashMap<>();
    // Progress per player: UUID -> current count
    private final Map<UUID, Integer> challengeProgress = new HashMap<>();

    public ChallengeManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
        loadChallenges();
    }

    /**
     * Load challenges from config.
     */
    public void loadChallenges() {
        availableChallenges.clear();
        for (ConfigManager.ChallengeConfig cc : plugin.getConfigManager().getChallengeList()) {
            try {
                Challenge.ChallengeType type = Challenge.ChallengeType.valueOf(cc.type().toUpperCase());
                availableChallenges.add(new Challenge(type, cc.target(), cc.amount(), cc.reward(), cc.description()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid challenge type: " + cc.type());
            }
        }
        plugin.getLogger().info("Loaded " + availableChallenges.size() + " challenge(s).");
    }

    /**
     * Assign a random challenge to all alive players in a game instance.
     */
    public void assignChallenges(GameInstance game) {
        if (!plugin.getConfigManager().isChallengesEnabled())
            return;
        if (availableChallenges.isEmpty())
            return;

        for (Player player : game.getAlivePlayers()) {
            Challenge challenge = availableChallenges.get(
                    ThreadLocalRandom.current().nextInt(availableChallenges.size()));
            activeChallenges.put(player.getUniqueId(), challenge);
            challengeProgress.put(player.getUniqueId(), 0);

            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("⚡ CHALLENGE : ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text(challenge.getDescription(), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, false)));
            player.sendMessage(Component.text("  Objectif : ", NamedTextColor.GRAY)
                    .append(Component.text(challenge.getAmount() + "x " + challenge.getTarget(),
                            NamedTextColor.WHITE)));
            player.sendMessage(Component.text("  Récompense : ", NamedTextColor.GRAY)
                    .append(Component.text(challenge.getRewardEffect(), NamedTextColor.GREEN)));
            player.sendMessage(Component.empty());
        }
    }

    /**
     * Report progress for a player.
     *
     * @return true if the challenge was just completed
     */
    public boolean reportProgress(UUID playerUuid, Challenge.ChallengeType type, String target) {
        Challenge active = activeChallenges.get(playerUuid);
        if (active == null)
            return false;
        if (active.getType() != type)
            return false;
        if (!active.getTarget().equalsIgnoreCase(target))
            return false;

        int current = challengeProgress.getOrDefault(playerUuid, 0) + 1;
        challengeProgress.put(playerUuid, current);

        if (current >= active.getAmount()) {
            // Challenge completed!
            Player player = plugin.getServer().getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text("✅ Challenge complété ! ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(active.getDescription(), NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, false)));

                // Apply reward
                PotionEffectType effect = active.getRewardPotionEffect();
                if (effect != null) {
                    player.addPotionEffect(new PotionEffect(effect, 600, 0, false, true)); // 30 seconds
                }

                if (plugin.getSoundManager() != null) {
                    plugin.getSoundManager().playSound("challenge-complete", player);
                }
            }

            activeChallenges.remove(playerUuid);
            challengeProgress.remove(playerUuid);
            return true;
        }

        // Show progress
        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player != null) {
            player.sendActionBar(
                    Component.text("Challenge: " + current + "/" + active.getAmount() + " " + active.getTarget(),
                            NamedTextColor.GOLD));
        }

        return false;
    }

    /**
     * Clear all active challenges (on game end).
     */
    public void clearAll() {
        activeChallenges.clear();
        challengeProgress.clear();
    }

    /**
     * Check if a player has an active challenge.
     */
    public boolean hasActiveChallenge(UUID playerUuid) {
        return activeChallenges.containsKey(playerUuid);
    }

    /**
     * Get the active challenge for a player.
     */
    public Challenge getActiveChallenge(UUID playerUuid) {
        return activeChallenges.get(playerUuid);
    }
}
