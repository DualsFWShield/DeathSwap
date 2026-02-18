package be.dualsfwshield.deathswap.vote;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.SeedEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * Handles seed voting before a game starts.
 * Presents 3 random seeds and lets players vote via clickable chat messages.
 */
public class VoteManager {

    private final DeathSwapPlugin plugin;

    // Active vote sessions: arena ID -> VoteSession
    private final Map<String, VoteSession> activeSessions = new HashMap<>();

    public VoteManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start a vote for an arena. Once voting finishes, calls the callback with the
     * winning seed.
     *
     * @param game     the game instance
     * @param seeds    available seeds
     * @param voters   players who can vote
     * @param callback called with the winning SeedEntry
     */
    public void startVote(GameInstance game, List<SeedEntry> seeds, Set<Player> voters, Consumer<SeedEntry> callback) {
        if (seeds.isEmpty()) {
            callback.accept(new SeedEntry("0", "Random World"));
            return;
        }

        int optionsCount = Math.min(plugin.getConfigManager().getVoteOptionsCount(), seeds.size());
        List<SeedEntry> options = pickRandom(seeds, optionsCount);

        VoteSession session = new VoteSession(game.getArenaId(), options, callback);
        activeSessions.put(game.getArenaId(), session);

        // Send vote UI
        for (Player player : voters) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                    TextDecoration.STRIKETHROUGH));
            player.sendMessage(
                    Component.text("🗳 VOTE — Choisissez la seed !", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.empty());

            for (int i = 0; i < options.size(); i++) {
                SeedEntry seed = options.get(i);
                int voteNumber = i + 1;

                Component button = Component.text("[" + voteNumber + "] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(seed.name(), NamedTextColor.YELLOW).decoration(TextDecoration.BOLD,
                                false))
                        .clickEvent(ClickEvent.runCommand("/ds vote " + game.getArenaId() + " " + voteNumber))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Cliquez pour voter " + seed.name(), NamedTextColor.GRAY)));

                player.sendMessage(button);
            }

            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Temps restant : " + plugin.getConfigManager().getVoteTime() + "s",
                    NamedTextColor.GRAY));
            player.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                    TextDecoration.STRIKETHROUGH));
        }

        // Start countdown
        int voteTime = plugin.getConfigManager().getVoteTime();
        new BukkitRunnable() {
            int remaining = voteTime;

            @Override
            public void run() {
                remaining--;

                if (remaining <= 5 && remaining > 0) {
                    for (Player p : voters) {
                        if (p.isOnline()) {
                            p.sendMessage(Component.text("🗳 Vote se termine dans " + remaining + "s...",
                                    NamedTextColor.YELLOW));
                        }
                    }
                }

                if (remaining <= 0) {
                    cancel();
                    finishVote(game.getArenaId());
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        if (plugin.getSoundManager() != null) {
            for (Player p : voters) {
                plugin.getSoundManager().playSound("vote-cast", p);
            }
        }
    }

    /**
     * Register a vote from a player.
     *
     * @param arenaId    arena ID
     * @param playerUuid voter UUID
     * @param choice     1-based vote choice
     * @return true if vote was registered
     */
    public boolean castVote(String arenaId, UUID playerUuid, int choice) {
        VoteSession session = activeSessions.get(arenaId);
        if (session == null)
            return false;

        if (choice < 1 || choice > session.options.size())
            return false;

        session.votes.put(playerUuid, choice - 1); // 0-indexed
        return true;
    }

    /**
     * Finish voting and determine the winner.
     */
    private void finishVote(String arenaId) {
        VoteSession session = activeSessions.remove(arenaId);
        if (session == null)
            return;

        // Count votes
        int[] counts = new int[session.options.size()];
        for (int choice : session.votes.values()) {
            if (choice >= 0 && choice < counts.length) {
                counts[choice]++;
            }
        }

        // Find winner (most votes, random tiebreak)
        int maxVotes = -1;
        List<Integer> winners = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > maxVotes) {
                maxVotes = counts[i];
                winners.clear();
                winners.add(i);
            } else if (counts[i] == maxVotes) {
                winners.add(i);
            }
        }

        int winnerIndex;
        if (winners.isEmpty()) {
            winnerIndex = ThreadLocalRandom.current().nextInt(session.options.size());
        } else {
            winnerIndex = winners.get(ThreadLocalRandom.current().nextInt(winners.size()));
        }

        SeedEntry winner = session.options.get(winnerIndex);
        session.callback.accept(winner);
    }

    /**
     * Check if a vote is active for an arena.
     */
    public boolean isVoteActive(String arenaId) {
        return activeSessions.containsKey(arenaId);
    }

    /**
     * Pick N random items from a list.
     */
    private List<SeedEntry> pickRandom(List<SeedEntry> source, int count) {
        List<SeedEntry> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * Internal vote session state.
     */
    private static class VoteSession {
        final String arenaId;
        final List<SeedEntry> options;
        final Map<UUID, Integer> votes = new HashMap<>();
        final Consumer<SeedEntry> callback;

        VoteSession(String arenaId, List<SeedEntry> options, Consumer<SeedEntry> callback) {
            this.arenaId = arenaId;
            this.options = options;
            this.callback = callback;
        }
    }
}
