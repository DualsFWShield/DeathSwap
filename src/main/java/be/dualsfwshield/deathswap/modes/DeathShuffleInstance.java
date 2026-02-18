package be.dualsfwshield.deathswap.modes;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import be.dualsfwshield.deathswap.UIMode;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.List;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DeathShuffle game mode.
 * Each round, a random death cause is assigned. Players must die that way
 * within the time limit.
 * Success = next round. Failure = spectator. Difficulty increases each round.
 */
public class DeathShuffleInstance extends GameInstance {

    private int currentRound = 0;
    private DeathCause currentDeathCause;
    private int roundTimer;
    private int roundDuration;
    private BukkitTask roundTask;

    // Track which players have completed the current round
    private final Set<UUID> completedRound = new HashSet<>();
    // Track players pending respawn (died the right way)
    private final Map<UUID, Boolean> pendingRespawn = new HashMap<>();

    // Allowed causes from config
    private final Set<DeathCause> allowedCauses = new HashSet<>();

    public DeathShuffleInstance(DeathSwapPlugin plugin, String arenaId, ConfigManager.ArenaConfig config) {
        super(plugin, arenaId, config);
        loadAllowedCauses();
    }

    private void loadAllowedCauses() {
        allowedCauses.clear();
        ConfigManager.DeathShuffleConfig dsConfig = getPlugin().getConfigManager().getDeathShuffleConfig();
        if (dsConfig != null) {
            List<String> causesList = dsConfig.getConfig().getStringList("causes");
            if (causesList != null && !causesList.isEmpty()) {
                for (String causeName : causesList) {
                    try {
                        DeathCause dc = DeathCause.valueOf(causeName.toUpperCase());
                        allowedCauses.add(dc);
                    } catch (IllegalArgumentException e) {
                        getPlugin().getLogger().warning("Invalid cause in deathshuffle.yml: " + causeName);
                    }
                }
            }
        }

        // Fallback
        if (allowedCauses.isEmpty()) {
            for (DeathCause dc : DeathCause.values()) {
                allowedCauses.add(dc);
            }
        }
    }

    @Override
    public GameType getGameType() {
        return GameType.DEATHSHUFFLE;
    }

    /**
     * Override the game loop to use round-based mechanics instead of swap timers.
     */
    @Override
    protected void startGameLoop() {
        startNextRound();
    }

    /**
     * Start a new round with a random death cause of appropriate difficulty.
     */
    private void startNextRound() {
        if (getState() != GameState.RUNNING)
            return;
        if (getAlivePlayers().size() < 1)
            return;

        currentRound++;
        completedRound.clear();

        // Determine difficulty tier based on round number
        int difficulty;
        if (currentRound <= 3)
            difficulty = 1; // Rounds 1-3: Easy
        else if (currentRound <= 6)
            difficulty = 2; // Rounds 4-6: Medium
        else
            difficulty = 3; // Rounds 7+: Hard

        // Pick random death cause from difficulty tier
        // Pick random death cause from difficulty tier
        // Filter by allowed causes
        DeathCause[] causes = Arrays.stream(DeathCause.getByDifficulty(difficulty))
                .filter(allowedCauses::contains)
                .toArray(DeathCause[]::new);

        if (causes.length == 0) {
            // Fallback to all allowed causes
            causes = allowedCauses.toArray(new DeathCause[0]);
        }
        currentDeathCause = causes[ThreadLocalRandom.current().nextInt(causes.length)];

        // Round duration based on difficulty
        roundDuration = getConfig().getRoundTime(difficulty);
        roundTimer = roundDuration;

        // Announce the round
        broadcastGame("&d&lROUND " + currentRound + " &7— &e" + currentDeathCause.getChallenge());
        broadcastGame("&7Difficulté : " + getDifficultyStars(difficulty) + " &7| Temps : &e" + roundDuration + "s");

        for (Player p : getAlivePlayers()) {
            p.showTitle(Title.title(
                    Component.text("ROUND " + currentRound, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
                    Component.text(currentDeathCause.getChallenge(), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));

            if (getPlugin().getSoundManager() != null) {
                getPlugin().getSoundManager().playSound("shuffle", p);
            }
        }

        // Update boss bar
        if (getConfig().uiMode == UIMode.RICH && getBossBar() != null) {
            getBossBar().name(Component.text("Round " + currentRound + " : ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(currentDeathCause.getDisplayName(), NamedTextColor.YELLOW)));
            getBossBar().color(BossBar.Color.PURPLE);
            getBossBar().progress(1.0f);
        }

        // Start round timer
        roundTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (getState() != GameState.RUNNING) {
                    cancel();
                    return;
                }

                roundTimer--;

                // Update boss bar progress
                if (getConfig().uiMode == UIMode.RICH && getBossBar() != null) {
                    float progress = Math.max(0, Math.min(1, (float) roundTimer / roundDuration));
                    getBossBar().progress(progress);

                    if (roundTimer <= 10) {
                        getBossBar().color(BossBar.Color.RED);
                    }
                }

                if (getConfig().uiMode == UIMode.RICH) {
                    // Action bar countdown
                    for (Player p : getAlivePlayers()) {
                        if (!completedRound.contains(p.getUniqueId())) {
                            if (roundTimer <= 10) {
                                p.sendActionBar(
                                        Component.text(
                                                "⚠ " + roundTimer + "s — " + currentDeathCause.getChallenge() + " ⚠",
                                                NamedTextColor.RED, TextDecoration.BOLD));
                            } else {
                                p.sendActionBar(
                                        Component.text(currentDeathCause.getChallenge() + " — " + roundTimer + "s",
                                                NamedTextColor.YELLOW));
                            }
                        } else {
                            p.sendActionBar(
                                    Component.text("✅ Complété ! En attente des autres...", NamedTextColor.GREEN));
                        }
                    }
                } else {
                    // CLEAN Mode: Chat notifications
                    if (roundTimer == 60 || roundTimer == 30 || roundTimer == 10
                            || (roundTimer <= 5 && roundTimer > 0)) {
                        broadcastGame("&c⚠ FIN DU ROUND DANS " + roundTimer + " SECONDES ⚠");
                        if (roundTimer <= 5 && getPlugin().getSoundManager() != null) {
                            getPlugin().getSoundManager().playSoundAll("countdown-tick", getGamePlayers());
                        }
                    }
                    if (roundTimer % 60 == 0 && roundTimer > 0) {
                        broadcastGame("&eRappel: Vous devez mourir par &6" + currentDeathCause.getDisplayName());
                    }
                }

                // Check if all alive players completed the round
                if (allAliveCompleted()) {
                    cancel();
                    broadcastGame("&a&lTout le monde a réussi ! Round suivant...");
                    Bukkit.getScheduler().runTaskLater(getPlugin(), () -> startNextRound(), 60L); // 3 seconds
                    return;
                }

                // Round time expired
                if (roundTimer <= 0) {
                    cancel();
                    onRoundTimeExpired();
                }
            }
        }.runTaskTimer(getPlugin(), 20L, 20L);
    }

    /**
     * When the round timer expires, eliminate players who haven't completed the
     * challenge.
     */
    private void onRoundTimeExpired() {
        broadcastGame("&c&lTemps écoulé !");

        Set<Player> failed = new HashSet<>();
        for (Player p : new HashSet<>(getAlivePlayers())) {
            if (!completedRound.contains(p.getUniqueId())) {
                failed.add(p);
            }
        }

        for (Player p : failed) {
            broadcastGame("&c" + p.getName() + " n'a pas réussi ! → Spectateur");
            eliminatePlayer(p);
        }

        // Check win condition
        if (getAlivePlayers().size() <= 1) {
            checkWinCondition();
        } else {
            // Start next round after delay
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> startNextRound(), 60L);
        }
    }

    /**
     * Called by DeathShuffleListener when a player dies.
     * Check if the death cause matches the current challenge.
     *
     * @return true if this was a correct death (player should respawn)
     */
    public boolean onPlayerDeath(Player player, EntityDamageEvent.DamageCause cause) {
        if (getState() != GameState.RUNNING)
            return false;
        if (!getAlivePlayers().contains(player))
            return false;
        if (completedRound.contains(player.getUniqueId()))
            return false;

        if (currentDeathCause != null && currentDeathCause.getDamageCause() == cause) {
            // Correct death!
            completedRound.add(player.getUniqueId());
            pendingRespawn.put(player.getUniqueId(), true);

            broadcastGame("&a" + player.getName() + " a réussi le challenge ! &7(" +
                    completedRound.size() + "/" + getAlivePlayers().size() + ")");

            player.sendMessage(Component.text("✅ Bien joué ! ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("En attente des autres joueurs...", NamedTextColor.GRAY)
                            .decoration(TextDecoration.BOLD, false)));

            if (getPlugin().getSoundManager() != null) {
                getPlugin().getSoundManager().playSound("round-success", player);
            }

            // Record stats
            if (getPlugin().getConfigManager().isStatsEnabled() && getPlugin().getStatsManager() != null) {
                getPlugin().getStatsManager().addDeath(player.getUniqueId(), player.getName());
            }

            return true; // Signal to respawn, not spectate
        }

        // Wrong death: respawn but don't mark as complete
        pendingRespawn.put(player.getUniqueId(), false);
        player.sendMessage(Component.text("❌ Mauvaise mort ! Tu dois : " + currentDeathCause.getChallenge(),
                NamedTextColor.RED));

        if (getPlugin().getSoundManager() != null) {
            getPlugin().getSoundManager().playSound("round-fail", player);
        }

        return true; // Still respawn the player
    }

    /**
     * Check if a respawn is pending for the player (used by the listener).
     */
    public boolean isPendingRespawn(UUID uuid) {
        return pendingRespawn.containsKey(uuid);
    }

    /**
     * Consume the pending respawn flag.
     */
    public boolean consumePendingRespawn(UUID uuid) {
        Boolean val = pendingRespawn.remove(uuid);
        return val != null;
    }

    /**
     * Eliminate a player — make them a spectator.
     */
    private void eliminatePlayer(Player player) {
        getAlivePlayers().remove(player);
        getSpectators().add(player);
        player.setGameMode(GameMode.SPECTATOR);
        giveSpectatorTools(player);

        if (getPlugin().getSoundManager() != null) {
            getPlugin().getSoundManager().playSound("death", player);
        }

        if (getPlugin().getConfigManager().isStatsEnabled() && getPlugin().getStatsManager() != null) {
            getPlugin().getStatsManager().addDeath(player.getUniqueId(), player.getName());
        }
    }

    /**
     * Check if all alive players have completed the round.
     */
    private boolean allAliveCompleted() {
        for (Player p : getAlivePlayers()) {
            if (!completedRound.contains(p.getUniqueId()))
                return false;
        }
        return !getAlivePlayers().isEmpty();
    }

    /**
     * Get difficulty stars display.
     */
    private String getDifficultyStars(int difficulty) {
        return switch (difficulty) {
            case 1 -> "&a★&7☆☆";
            case 2 -> "&e★★&7☆";
            case 3 -> "&c★★★";
            default -> "&7☆☆☆";
        };
    }

    /**
     * Get the current death cause for the listener.
     */
    public DeathCause getCurrentDeathCause() {
        return currentDeathCause;
    }

    /**
     * Get the current round number.
     */
    public int getCurrentRound() {
        return currentRound;
    }

    @Override
    public void stopGame() {
        if (roundTask != null) {
            roundTask.cancel();
            roundTask = null;
        }
        completedRound.clear();
        pendingRespawn.clear();
        currentRound = 0;
        super.stopGame();
    }
}
