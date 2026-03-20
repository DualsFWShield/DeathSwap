package be.dualsfwshield.deathswap.modes;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import be.dualsfwshield.deathswap.UIMode;
import be.dualsfwshield.deathswap.TeamManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import be.dualsfwshield.deathswap.util.Lang;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private final Map<UUID, EntityDamageEvent.DamageCause> playerCauses = new HashMap<>();
    private int roundTimer;
    private int roundDuration;
    private BukkitTask roundTask;

    // Track which players have completed the current round
    private final Set<UUID> completedRound = new HashSet<>();
    // Track players pending respawn (died the right way)
    private final Map<UUID, Boolean> pendingRespawn = new HashMap<>();
    
    // Team scores for the current round
    private final Map<TeamManager.Team, Integer> teamRoundScores = new HashMap<>();

    // Allowed causes from config
    private final Set<EntityDamageEvent.DamageCause> allowedCauses = new HashSet<>();
    // Custom difficulty overrides from config
    private final Map<EntityDamageEvent.DamageCause, Integer> customDifficulties = new EnumMap<>(
            EntityDamageEvent.DamageCause.class);

    // Death causes that require the Nether dimension
    private static final Set<EntityDamageEvent.DamageCause> NETHER_CAUSES = Set.of(
            EntityDamageEvent.DamageCause.HOT_FLOOR // Magma blocks are Nether-exclusive
    );

    // Death causes that require the End dimension
    private static final Set<EntityDamageEvent.DamageCause> END_CAUSES = Set.of(
            EntityDamageEvent.DamageCause.DRAGON_BREATH // Dragon is End-exclusive
    );

    public DeathShuffleInstance(DeathSwapPlugin plugin, String arenaId, ConfigManager.ArenaConfig config) {
        super(plugin, arenaId, config);
        loadAllowedCauses();
    }

    public void reloadSettings() {
        loadAllowedCauses();
    }

    private void loadAllowedCauses() {
        allowedCauses.clear();
        customDifficulties.clear();
        ConfigManager.DeathShuffleConfig dsConfig = getPlugin().getConfigManager().getDeathShuffleConfig();
        if (dsConfig != null && !dsConfig.getEntries().isEmpty()) {
            for (ConfigManager.DeathShuffleEntry entry : dsConfig.getEntries()) {
                if (!entry.enabled())
                    continue;
                try {
                    EntityDamageEvent.DamageCause dc = EntityDamageEvent.DamageCause
                            .valueOf(entry.cause().toUpperCase());

                    // Filter out dimension-dependent causes when disabled
                    if (!getConfig().netherEnabled && NETHER_CAUSES.contains(dc))
                        continue;
                    if (!getConfig().endEnabled && END_CAUSES.contains(dc))
                        continue;

                    allowedCauses.add(dc);
                    // Store custom difficulty override
                    customDifficulties.put(dc, entry.difficulty());
                } catch (IllegalArgumentException e) {
                    getPlugin().getLogger().warning("Invalid cause in deathshuffle.yml: " + entry.cause());
                }
            }
        }

        // No fallback here to all causes. If empty, it means the user intentionally
        // disabled everything
        // or filtered them out via dimension settings. We handle this safely in
        // startNextRound.
    }

    /**
     * Get the effective difficulty for a death cause (from config or default 1).
     */
    private int getEffectiveDifficulty(EntityDamageEvent.DamageCause dc) {
        return customDifficulties.getOrDefault(dc, 1);
    }

    public static String getCauseDisplayName(EntityDamageEvent.DamageCause cause) {
        return be.dualsfwshield.deathswap.util.Lang
                .get("death-cause-" + cause.name().toLowerCase().replace("_", "-") + "-name");
    }

    public static String getCauseChallenge(EntityDamageEvent.DamageCause cause) {
        return be.dualsfwshield.deathswap.util.Lang
                .get("death-cause-" + cause.name().toLowerCase().replace("_", "-") + "-challenge");
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
        playerCauses.clear();
        teamRoundScores.clear();

        // Initialize team scores
        if (getTeamManager() != null && getConfig().teamsEnabled) {
            for (TeamManager.Team t : getTeamManager().getAllTeams()) {
                if (t.hasAlivePlayers(getAlivePlayers())) {
                    teamRoundScores.put(t, 0);
                }
            }
        }

        // Determine difficulty tier based on round number
        int difficulty;
        if (currentRound <= 3)
            difficulty = 1; // Rounds 1-3: Easy
        else if (currentRound <= 6)
            difficulty = 2; // Rounds 4-6: Medium
        else
            difficulty = 3; // Rounds 7+: Hard

        // Pick random death cause matching difficulty tier (using config overrides)
        EntityDamageEvent.DamageCause[] causes = allowedCauses.stream()
                .filter(dc -> getEffectiveDifficulty(dc) == difficulty)
                .toArray(EntityDamageEvent.DamageCause[]::new);

        if (causes.length == 0) {
            // Fallback to all allowed causes (e.g., if no allowed causes match this
            // specific difficulty tier)
            causes = allowedCauses.toArray(new EntityDamageEvent.DamageCause[0]);
        }

        // If STILL empty (e.g., player disabled absolutely everything in the config or
        // all were filtered by dimension settings)
        if (causes.length == 0) {
            causes = new EntityDamageEvent.DamageCause[] { EntityDamageEvent.DamageCause.SUICIDE };
        }

        // Assign causes and duration
        if (getConfig().deathShuffleRaceMode) {
            // Race Mode: Infinite time
            EntityDamageEvent.DamageCause sharedCause = causes[ThreadLocalRandom.current().nextInt(causes.length)];
            for (Player p : getAlivePlayers()) {
                playerCauses.put(p.getUniqueId(), sharedCause);
            }
            roundDuration = Integer.MAX_VALUE;
        } else if (getConfig().deathShuffleUniqueCauses) {
            // Unique causes per player
            for (Player p : getAlivePlayers()) {
                EntityDamageEvent.DamageCause dc = causes[ThreadLocalRandom.current().nextInt(causes.length)];
                playerCauses.put(p.getUniqueId(), dc);
            }
            roundDuration = getConfig().getNextSwapInterval();
        } else {
            // Classic Mode: Shared cause
            EntityDamageEvent.DamageCause sharedCause = causes[ThreadLocalRandom.current().nextInt(causes.length)];
            for (Player p : getAlivePlayers()) {
                playerCauses.put(p.getUniqueId(), sharedCause);
            }
            roundDuration = getConfig().getNextSwapInterval();
        }

        roundTimer = roundDuration;

        // Announcements
        if (getConfig().deathShuffleUniqueCauses && !getConfig().deathShuffleRaceMode) {
            broadcastGame("&d&lROUND " + currentRound + " &7— &eObjectifs individuels assignés !");
        } else {
            if (!getAlivePlayers().isEmpty()) {
                EntityDamageEvent.DamageCause display = playerCauses.values().iterator().next();
                broadcastGame("&d&lROUND " + currentRound + " &7— &e" + getCauseChallenge(display));
            }
        }
        broadcastGame("&7Difficulté : " + getDifficultyStars(difficulty) + " &7| Temps : &e"
                + (roundDuration == Integer.MAX_VALUE ? "∞" : roundDuration + "s"));

        for (Player p : getAlivePlayers()) {
            EntityDamageEvent.DamageCause dc = playerCauses.get(p.getUniqueId());
            if (dc == null)
                continue;

            p.showTitle(Title.title(
                    Lang.getComponent("shuffle-round-title-ds", "%round%", String.valueOf(currentRound)),
                    Component.text(getCauseChallenge(dc), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));

            if (getPlugin().getSoundManager() != null) {
                getPlugin().getSoundManager().playSound("shuffle", p);
            }
        }

        // Update boss bar
        if (getConfig().uiMode == UIMode.RICH && getBossBar() != null) {
            if (getConfig().deathShuffleUniqueCauses && !getConfig().deathShuffleRaceMode) {
                getBossBar().name(Lang.getComponent("shuffle-bossbar-prefix-ds", "%round%", String.valueOf(currentRound))
                        .append(Lang.getComponent("shuffle-personal-objective")));
            } else {
                if (!playerCauses.isEmpty()) {
                    EntityDamageEvent.DamageCause dc = playerCauses.values().iterator().next();
                    getBossBar().name(Lang.getComponent("shuffle-bossbar-prefix-ds", "%round%", String.valueOf(currentRound))
                            .append(Component.text(getCauseDisplayName(dc), NamedTextColor.YELLOW)));
                }
            }
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

                globalTimer--; // Decrease the max game time

                if (globalTimer <= 0) {
                    cancel();
                    broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-timeout"));
                    stopGame();
                    return;
                }

                if (roundDuration != Integer.MAX_VALUE) {
                    roundTimer--;
                }

                // Update boss bar progress
                if (getConfig().uiMode == UIMode.RICH && getBossBar() != null && roundDuration != Integer.MAX_VALUE) {
                    float progress = Math.max(0, Math.min(1, (float) roundTimer / roundDuration));
                    getBossBar().progress(progress);

                    if (roundTimer <= 10) {
                        getBossBar().color(BossBar.Color.RED);
                    }
                }

                if (getConfig().uiMode == UIMode.RICH) {
                    // Action bar countdown
                    for (Player p : getAlivePlayers()) {
                        EntityDamageEvent.DamageCause dc = playerCauses.get(p.getUniqueId());
                        if (dc == null)
                            continue;

                        if (!completedRound.contains(p.getUniqueId())) {
                            if (getConfig().deathShuffleRaceMode) {
                                p.sendActionBar(Lang.getComponent("shuffle-actionbar-race-ds", "%target%", getCauseChallenge(dc)));
                            } else {
                                if (roundTimer <= 10) {
                                    p.sendActionBar(Lang.getComponent("shuffle-actionbar-urgent-ds", "%time%", String.valueOf(roundTimer), "%target%", getCauseChallenge(dc)));
                                } else {
                                    p.sendActionBar(Lang.getComponent("shuffle-actionbar-normal-ds", "%time%", String.valueOf(roundTimer), "%target%", getCauseChallenge(dc)));
                                }
                            }
                        } else {
                            p.sendActionBar(Lang.getComponent("shuffle-actionbar-completed"));
                        }
                    }
                } else {
                    // CLEAN Mode: Chat notifications
                    if (roundDuration != Integer.MAX_VALUE) {
                        if (roundTimer == 60 || roundTimer == 30 || roundTimer == 10
                                || (roundTimer <= 5 && roundTimer > 0)) {
                            broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("shuffle-round-ending", "%time%", String.valueOf(roundTimer)));
                            if (roundTimer <= 5 && getPlugin().getSoundManager() != null) {
                                getPlugin().getSoundManager().playSoundAll("countdown-tick", getGamePlayers());
                            }
                        }
                        if (roundTimer % 60 == 0 && roundTimer > 0) {
                            for (Player p : getAlivePlayers()) {
                                EntityDamageEvent.DamageCause dc = playerCauses.get(p.getUniqueId());
                                if (dc != null) {
                                    p.sendMessage(be.dualsfwshield.deathswap.util.Lang.getComponent("game-prefix")
                                            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("ds-reminder", "%target%", getCauseDisplayName(dc))));
                                }
                            }
                        }
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
                if (roundTimer <= 0 && roundDuration != Integer.MAX_VALUE) {
                    cancel();
                    onRoundTimeExpired();
                }
            }
        }.runTaskTimer(getPlugin(), 20L, 20L);
    }

    /**
     * When the round timer expires, eliminate players who haven't completed the
     * challenge or the team with the lowest score.
     */
    private void onRoundTimeExpired() {
        if (getTeamManager() != null && getConfig().teamsEnabled) {
            handleTeamRoundExpiration();
        } else {
            handleSoloRoundExpiration();
        }

        checkWinCondition();

        if (getState() == GameState.RUNNING) {
            // Continue as long as survivors exist (or until global time ends)
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> startNextRound(), 60L);
        }
    }

    private void handleSoloRoundExpiration() {
        broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-round-fail-everyone"));

        Set<Player> failed = new HashSet<>();
        for (Player p : new HashSet<>(getAlivePlayers())) {
            if (!completedRound.contains(p.getUniqueId())) {
                failed.add(p);
            }
        }

        if (failed.size() == getAlivePlayers().size() && !failed.isEmpty()) {
            // Mercy Rule: Everyone failed
            broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-round-mercy"));
        } else {
            // Normal elimination
            for (Player p : failed) {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-round-fail", "%player%", p.getName()));
                eliminatePlayer(p);
            }
        }
    }

    private void handleTeamRoundExpiration() {
        broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-round-fail-everyone"));

        // Find the lowest score
        int lowestScore = Integer.MAX_VALUE;
        for (int score : teamRoundScores.values()) {
            if (score < lowestScore) {
                lowestScore = score;
            }
        }

        // Find all teams with the lowest score
        List<TeamManager.Team> losingTeams = new java.util.ArrayList<>();
        for (Map.Entry<TeamManager.Team, Integer> entry : teamRoundScores.entrySet()) {
            if (entry.getValue() == lowestScore) {
                losingTeams.add(entry.getKey());
            }
        }

        // If all teams tied for lowest, mercy rule (no one dies)
        if (losingTeams.size() == teamRoundScores.size() && !losingTeams.isEmpty()) {
            broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-round-mercy"));
            return;
        }

        // Eliminate one random player from each losing team
        for (TeamManager.Team team : losingTeams) {
            List<Player> aliveMembers = team.getAlivePlayers(getAlivePlayers());
            if (!aliveMembers.isEmpty()) {
                Player victim = aliveMembers.get(ThreadLocalRandom.current().nextInt(aliveMembers.size()));
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("team-ds-eliminated", 
                        "%player%", victim.getName(), 
                        "%team%", team.getDisplayName()));
                eliminatePlayer(victim);
            }
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

        EntityDamageEvent.DamageCause dc = playerCauses.get(player.getUniqueId());

        if (dc != null && dc == cause) {
            // Correct death!
            completedRound.add(player.getUniqueId());
            pendingRespawn.put(player.getUniqueId(), true);

            // Record stats
            if (getPlugin().getConfigManager().isStatsEnabled() && getPlugin().getStatsManager() != null) {
                getPlugin().getStatsManager().addDeath(player.getUniqueId(), player.getName());
            }

            TeamManager.Team team = getTeamManager() != null && getConfig().teamsEnabled ? getTeamManager().getPlayerTeam(player) : null;

            if (team != null) {
                // Team Mode: +1 point
                teamRoundScores.put(team, teamRoundScores.getOrDefault(team, 0) + 1);
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-team-success", "%player%", player.getName(), "%team%", getTeamColorCode(team) + team.getDisplayName()));
                
                for (Player p : team.getAlivePlayers(getAlivePlayers())) {
                    p.sendMessage(be.dualsfwshield.deathswap.util.Lang.getComponent("ds-team-completed"));
                    if (getPlugin().getSoundManager() != null) {
                        getPlugin().getSoundManager().playSound("round-success", p);
                    }
                }
            } else {
                // Solo Mode
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-solo-success", "%player%", player.getName(),
                        "%completed%", String.valueOf(completedRound.size()), "%total%", String.valueOf(getAlivePlayers().size())));
                
                player.sendMessage(be.dualsfwshield.deathswap.util.Lang.getComponent("ds-success-waiting"));
                if (getPlugin().getSoundManager() != null) {
                    getPlugin().getSoundManager().playSound("round-success", player);
                }
            }

            // Race Mode Check
            if (getConfig().deathShuffleRaceMode) {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-round-success", "%player%", player.getName(),
                        "%count%", "1", "%total%", "1"));
                if (getPlugin().getSoundManager() != null) {
                    getPlugin().getSoundManager().playSound("victory", player);
                }
                stopGame();
                return true;
            }

            return true; // Signal to respawn, not spectate
        }

        // Wrong death: respawn but don't mark as complete
        pendingRespawn.put(player.getUniqueId(), false);
        TeamManager.Team team = getTeamManager() != null && getConfig().teamsEnabled ? getTeamManager().getPlayerTeam(player) : null;

        if (dc != null) {
            player.sendMessage(be.dualsfwshield.deathswap.util.Lang.getComponent("ds-bad-death", "%target%", getCauseChallenge(dc)));
        } else {
            player.sendMessage(be.dualsfwshield.deathswap.util.Lang.getComponent("ds-bad-death-simple"));
        }

        if (team != null) {
            // Team Mode penalty
            if (roundDuration != Integer.MAX_VALUE) {
                roundTimer = Math.max(1, roundTimer - 15);
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("ds-team-penalty", "%player%", player.getName(), "%team%", getTeamColorCode(team) + team.getDisplayName()));
            }
        }

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
     * Check if all alive players (or teams) have completed the round.
     */
    private boolean allAliveCompleted() {
        if (getTeamManager() != null && getConfig().teamsEnabled) {
            // In team mode, round ends when ALL alive teams have at least one point
            int completedTeams = 0;
            int aliveTeams = getTeamManager().getAliveTeamCount(getAlivePlayers());
            for (int score : teamRoundScores.values()) {
                if (score > 0) completedTeams++;
            }
            return completedTeams >= aliveTeams && aliveTeams > 0;
        } else {
            // Solo Mode
            for (Player p : getAlivePlayers()) {
                if (!completedRound.contains(p.getUniqueId()))
                    return false;
            }
            return !getAlivePlayers().isEmpty();
        }
    }



    /**
     * Get the current death cause for a specific player.
     * Replaces getCurrentDeathCause.
     */
    public EntityDamageEvent.DamageCause getPlayerDeathCause(Player player) {
        return playerCauses.get(player.getUniqueId());
    }

    /**
     * Deprecated, kept for safety.
     * Returns the cause for the first player found.
     */
    public EntityDamageEvent.DamageCause getCurrentDeathCause() {
        if (playerCauses.isEmpty())
            return null;
        return playerCauses.values().iterator().next();
    }

    /**
     * Get the current round number.
     */
    public int getCurrentRound() {
        return currentRound;
    }

    @Override
    public void handleDeath(Player player) {
        // In DeathShuffle, death is the GOAL (if correct cause).
        // We do not eliminate players here. Check the cause.

        org.bukkit.event.entity.EntityDamageEvent lastDamage = player.getLastDamageCause();
        org.bukkit.event.entity.EntityDamageEvent.DamageCause cause = (lastDamage != null) ? lastDamage.getCause()
                : org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM;

        onPlayerDeath(player, cause);

        // Do NOT call super.handleDeath(player) which eliminates them.
    }

    @Override
    public void stopGame() {
        if (roundTask != null) {
            roundTask.cancel();
            roundTask = null;
        }
        completedRound.clear();
        pendingRespawn.clear();
        playerCauses.clear();
        teamRoundScores.clear();
        currentRound = 0;
        super.stopGame();
    }
}
