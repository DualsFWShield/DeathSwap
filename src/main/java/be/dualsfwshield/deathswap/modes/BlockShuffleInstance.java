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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BlockShuffle game mode.
 * Each round, players are assigned a random block to stand on or item to craft.
 * Success = next round. Failure (time expires) = spectator.
 * Difficulty increases each round.
 */
public class BlockShuffleInstance extends GameInstance {

    /**
     * Type of assignment for each round.
     */
    public enum AssignmentType {
        STAND, CRAFT
    }

    /**
     * Material entry with difficulty tier.
     */
    private record ShuffleTarget(Material material, int difficulty, AssignmentType type, String displayName) {
    }

    // Static target lists
    private static final List<ShuffleTarget> TARGETS = Arrays.asList(
            // Easy — STAND
            new ShuffleTarget(Material.DIRT, 1, AssignmentType.STAND, "Terre"),
            new ShuffleTarget(Material.OAK_LOG, 1, AssignmentType.STAND, "Bûche de chêne"),
            new ShuffleTarget(Material.COBBLESTONE, 1, AssignmentType.STAND, "Pierre taillée"),
            new ShuffleTarget(Material.SAND, 1, AssignmentType.STAND, "Sable"),
            new ShuffleTarget(Material.GRAVEL, 1, AssignmentType.STAND, "Gravier"),
            new ShuffleTarget(Material.STONE, 1, AssignmentType.STAND, "Pierre"),
            // Easy — CRAFT
            new ShuffleTarget(Material.CRAFTING_TABLE, 1, AssignmentType.CRAFT, "Table de craft"),
            new ShuffleTarget(Material.OAK_PLANKS, 1, AssignmentType.CRAFT, "Planches de chêne"),
            new ShuffleTarget(Material.STICK, 1, AssignmentType.CRAFT, "Bâton"),
            new ShuffleTarget(Material.WOODEN_PICKAXE, 1, AssignmentType.CRAFT, "Pioche en bois"),
            new ShuffleTarget(Material.TORCH, 1, AssignmentType.CRAFT, "Torche"),

            // Medium — STAND
            new ShuffleTarget(Material.IRON_ORE, 2, AssignmentType.STAND, "Minerai de fer"),
            new ShuffleTarget(Material.GOLD_ORE, 2, AssignmentType.STAND, "Minerai d'or"),
            new ShuffleTarget(Material.CLAY, 2, AssignmentType.STAND, "Argile"),
            new ShuffleTarget(Material.MOSSY_COBBLESTONE, 2, AssignmentType.STAND, "Pierre moussue"),
            new ShuffleTarget(Material.ICE, 2, AssignmentType.STAND, "Glace"),
            // Medium — CRAFT
            new ShuffleTarget(Material.IRON_PICKAXE, 2, AssignmentType.CRAFT, "Pioche en fer"),
            new ShuffleTarget(Material.FURNACE, 2, AssignmentType.CRAFT, "Four"),
            new ShuffleTarget(Material.BUCKET, 2, AssignmentType.CRAFT, "Seau"),
            new ShuffleTarget(Material.SHIELD, 2, AssignmentType.CRAFT, "Bouclier"),
            new ShuffleTarget(Material.COMPASS, 2, AssignmentType.CRAFT, "Boussole"),
            new ShuffleTarget(Material.BOOKSHELF, 2, AssignmentType.CRAFT, "Bibliothèque"),

            // Hard — STAND
            new ShuffleTarget(Material.DIAMOND_ORE, 3, AssignmentType.STAND, "Minerai de diamant"),
            new ShuffleTarget(Material.EMERALD_ORE, 3, AssignmentType.STAND, "Minerai d'émeraude"),
            new ShuffleTarget(Material.SPAWNER, 3, AssignmentType.STAND, "Spawner"),
            new ShuffleTarget(Material.DEEPSLATE_DIAMOND_ORE, 3, AssignmentType.STAND, "Diamant deepslate"),
            // Hard — CRAFT
            new ShuffleTarget(Material.DIAMOND_PICKAXE, 3, AssignmentType.CRAFT, "Pioche en diamant"),
            new ShuffleTarget(Material.ENCHANTING_TABLE, 3, AssignmentType.CRAFT, "Table d'enchantement"),
            new ShuffleTarget(Material.GOLDEN_APPLE, 3, AssignmentType.CRAFT, "Pomme dorée"),
            new ShuffleTarget(Material.PISTON, 3, AssignmentType.CRAFT, "Piston"),
            new ShuffleTarget(Material.PISTON, 3, AssignmentType.CRAFT, "Piston"),
            new ShuffleTarget(Material.CAKE, 3, AssignmentType.CRAFT, "Gâteau"));

    private final List<ShuffleTarget> targets = new ArrayList<>();

    private int currentRound = 0;
    private ShuffleTarget currentTarget;
    private int roundTimer;
    private int roundDuration;
    private BukkitTask roundTask;
    private final Set<UUID> completedRound = new HashSet<>();

    public BlockShuffleInstance(DeathSwapPlugin plugin, String arenaId, ConfigManager.ArenaConfig config) {
        super(plugin, arenaId, config);
        loadTargets();
    }

    private void loadTargets() {
        targets.clear();
        ConfigManager.BlockShuffleConfig bsConfig = getPlugin().getConfigManager().getBlockShuffleConfig();
        if (bsConfig != null) {
            List<String> blockList = bsConfig.getConfig().getStringList("blocks");
            if (blockList != null && !blockList.isEmpty()) {
                for (String matName : blockList) {
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase());
                        // Default to Difficulty 1, STAND, and formatted name
                        String name = matName.toLowerCase().replace("_", " ");
                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        targets.add(new ShuffleTarget(mat, 1, AssignmentType.STAND, name));
                    } catch (IllegalArgumentException e) {
                        getPlugin().getLogger().warning("Invalid material in blockshuffle.yml: " + matName);
                    }
                }
            }
        }
        
        // Fallback if config empty
        if (targets.isEmpty()) {
            targets.addAll(TARGETS);
        }
    }

    @Override
    public GameType getGameType() {
        return GameType.BLOCKSHUFFLE;
    }

    @Override
    protected void startGameLoop() {
        startNextRound();
    }

    /**
     * Start a new round with a random target.
     */
    private void startNextRound() {
        if (getState() != GameState.RUNNING)
            return;
        if (getAlivePlayers().size() < 1)
            return;

        currentRound++;
        completedRound.clear();

        // Determine difficulty tier
        int difficulty;
        if (currentRound <= 3)
            difficulty = 1;
        else if (currentRound <= 6)
            difficulty = 2;
        else
            difficulty = 3;

        // Pick random target from difficulty tier
        // Pick random target from difficulty tier
        // If we loaded from config (simple list), everything is difficulty 1.
        // So validation: check if we have targets for this difficulty. 
        // If not, use all available targets.
        List<ShuffleTarget> pool = targets.stream()
                .filter(t -> t.difficulty() == difficulty)
                .toList();

        if (pool.isEmpty()) {
            pool = new ArrayList<>(targets); // fallback to all
        }

        if (pool.isEmpty()) {
            // Should not happen if fallback used, unless targets is empty
            if (targets.isEmpty()) targets.addAll(TARGETS);
            pool = targets; 
        }
        currentTarget = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        roundDuration = getConfig().getRoundTime(difficulty);
        roundTimer = roundDuration;

        // Build announcement
        String emoji = currentTarget.type() == AssignmentType.STAND ? "🧱" : "🔨";
        String action = currentTarget.type() == AssignmentType.STAND
                ? "Tiens-toi sur : "
                : "Craft : ";

        broadcastGame("&b&lROUND " + currentRound + " &7— " + emoji + " &e" + action + currentTarget.displayName());
        broadcastGame("&7Difficulté : " + getDifficultyStars(difficulty) + " &7| Temps : &e" + roundDuration + "s");

        for (Player p : getAlivePlayers()) {
            p.showTitle(Title.title(
                    Component.text("ROUND " + currentRound, NamedTextColor.AQUA, TextDecoration.BOLD),
                    Component.text(action + currentTarget.displayName(), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));

            if (getPlugin().getSoundManager() != null) {
                getPlugin().getSoundManager().playSound("shuffle", p);
            }
        }

        // Update boss bar
        if (getConfig().uiMode == UIMode.RICH && getBossBar() != null) {
            getBossBar().name(Component.text("Round " + currentRound + " : ", NamedTextColor.AQUA)
                    .append(Component.text(emoji + " " + currentTarget.displayName(), NamedTextColor.YELLOW)));
            getBossBar().color(BossBar.Color.BLUE);
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

                // Boss bar progress
                if (getConfig().uiMode == UIMode.RICH && getBossBar() != null) {
                    float progress = Math.max(0, Math.min(1, (float) roundTimer / roundDuration));
                    getBossBar().progress(progress);
                    if (roundTimer <= 10) {
                        getBossBar().color(BossBar.Color.RED);
                    }
                }

                if (getConfig().uiMode == UIMode.RICH) {
                    // Action bar for players
                    for (Player p : getAlivePlayers()) {
                        if (!completedRound.contains(p.getUniqueId())) {
                            if (roundTimer <= 10) {
                                p.sendActionBar(
                                        Component.text("⚠ " + roundTimer + "s — " + currentTarget.displayName() + " ⚠",
                                                NamedTextColor.RED, TextDecoration.BOLD));
                            } else {
                                p.sendActionBar(Component.text(currentTarget.displayName() + " — " + roundTimer + "s",
                                        NamedTextColor.AQUA));
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
                        String emoji = currentTarget.type() == AssignmentType.STAND ? "🧱" : "🔨";
                        String action = currentTarget.type() == AssignmentType.STAND ? "Tiens-toi sur : " : "Craft : ";
                        broadcastGame("&eRappel: " + emoji + " " + action + "&6" + currentTarget.displayName());
                    }
                }

                // All alive completed?
                if (allAliveCompleted()) {
                    cancel();
                    broadcastGame("&a&lTout le monde a réussi ! Round suivant...");
                    Bukkit.getScheduler().runTaskLater(getPlugin(), () -> startNextRound(), 60L);
                    return;
                }

                if (roundTimer <= 0) {
                    cancel();
                    onRoundTimeExpired();
                }
            }
        }.runTaskTimer(getPlugin(), 20L, 20L);
    }

    /**
     * When the round timer expires.
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

        if (getAlivePlayers().size() <= 1) {
            checkWinCondition();
        } else {
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> startNextRound(), 60L);
        }
    }

    /**
     * Called by BlockShuffleListener when a player stands on the target block.
     */
    public boolean onPlayerStandOnBlock(Player player, Material blockType) {
        if (getState() != GameState.RUNNING)
            return false;
        if (!getAlivePlayers().contains(player))
            return false;
        if (completedRound.contains(player.getUniqueId()))
            return false;
        if (currentTarget == null)
            return false;
        if (currentTarget.type() != AssignmentType.STAND)
            return false;
        if (blockType != currentTarget.material())
            return false;

        completeRound(player);
        return true;
    }

    /**
     * Called by BlockShuffleListener when a player crafts the target item.
     */
    public boolean onPlayerCraftItem(Player player, Material itemType) {
        if (getState() != GameState.RUNNING)
            return false;
        if (!getAlivePlayers().contains(player))
            return false;
        if (completedRound.contains(player.getUniqueId()))
            return false;
        if (currentTarget == null)
            return false;
        if (currentTarget.type() != AssignmentType.CRAFT)
            return false;
        if (itemType != currentTarget.material())
            return false;

        completeRound(player);
        return true;
    }

    /**
     * Mark a player as having completed the round.
     */
    private void completeRound(Player player) {
        completedRound.add(player.getUniqueId());

        broadcastGame("&a" + player.getName() + " a réussi ! &7(" +
                completedRound.size() + "/" + getAlivePlayers().size() + ")");

        player.sendMessage(Component.text("✅ Bien joué ! ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("En attente des autres joueurs...", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false)));

        if (getPlugin().getSoundManager() != null) {
            getPlugin().getSoundManager().playSound("round-success", player);
        }
    }

    /**
     * Eliminate a player.
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

    private boolean allAliveCompleted() {
        for (Player p : getAlivePlayers()) {
            if (!completedRound.contains(p.getUniqueId()))
                return false;
        }
        return !getAlivePlayers().isEmpty();
    }

    private String getDifficultyStars(int difficulty) {
        return switch (difficulty) {
            case 1 -> "&a★&7☆☆";
            case 2 -> "&e★★&7☆";
            case 3 -> "&c★★★";
            default -> "&7☆☆☆";
        };
    }

    /**
     * Get the current target for the listener.
     */
    public ShuffleTarget getCurrentTarget() {
        return currentTarget;
    }

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
        currentRound = 0;
        super.stopGame();
    }
}
