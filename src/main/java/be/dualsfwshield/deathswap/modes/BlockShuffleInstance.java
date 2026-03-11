package be.dualsfwshield.deathswap.modes;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.DifficultyMode;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import be.dualsfwshield.deathswap.UIMode;
import be.dualsfwshield.deathswap.util.Lang;
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

import java.util.ArrayList;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
            new ShuffleTarget(Material.CAKE, 3, AssignmentType.CRAFT, "Gâteau"));

    private final List<ShuffleTarget> targets = new ArrayList<>();

    // Materials that require the Nether dimension
    private static final Set<Material> NETHER_MATERIALS = Set.of(
            Material.NETHERRACK, Material.SOUL_SAND, Material.SOUL_SOIL,
            Material.MAGMA_BLOCK, Material.GLOWSTONE, Material.NETHER_BRICKS,
            Material.NETHER_BRICK, Material.NETHER_WART, Material.NETHER_WART_BLOCK,
            Material.WARPED_STEM, Material.CRIMSON_STEM, Material.SHROOMLIGHT,
            Material.BLAZE_ROD, Material.BLAZE_POWDER, Material.GHAST_TEAR,
            Material.NETHER_QUARTZ_ORE, Material.QUARTZ, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP, Material.NETHERITE_INGOT,
            Material.NETHERITE_BLOCK, Material.NETHER_STAR,
            Material.CRYING_OBSIDIAN, Material.BLACKSTONE, Material.BASALT,
            Material.GILDED_BLACKSTONE, Material.BREWING_STAND, Material.BEACON);

    // Materials that require the End dimension
    private static final Set<Material> END_MATERIALS = Set.of(
            Material.END_STONE, Material.END_STONE_BRICKS, Material.PURPUR_BLOCK,
            Material.PURPUR_PILLAR, Material.END_ROD, Material.CHORUS_FLOWER,
            Material.CHORUS_FRUIT, Material.CHORUS_PLANT, Material.DRAGON_EGG,
            Material.ELYTRA, Material.SHULKER_SHELL, Material.SHULKER_BOX,
            Material.DRAGON_BREATH);

    // Materials exclusive to rare overworld biomes — auto-bumped to difficulty 3
    private static final Set<Material> RARE_BIOME_MATERIALS = Set.of(
            // Mushroom Island
            Material.MYCELIUM, Material.MUSHROOM_STEM,
            Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK,
            // Deep Dark
            Material.SCULK, Material.SCULK_CATALYST, Material.SCULK_SENSOR,
            Material.SCULK_SHRIEKER, Material.SCULK_VEIN,
            // Ice Spikes / Frozen
            Material.PACKED_ICE, Material.BLUE_ICE,
            // Badlands
            Material.TERRACOTTA, Material.RED_SAND,
            // Lush Caves
            Material.MOSS_BLOCK, Material.SPORE_BLOSSOM, Material.BIG_DRIPLEAF,
            // Amethyst Geodes
            Material.AMETHYST_BLOCK, Material.AMETHYST_CLUSTER, Material.CALCITE, Material.BUDDING_AMETHYST,
            // Ocean Monuments
            Material.PRISMARINE, Material.SEA_LANTERN, Material.SPONGE,
            // Jungle
            Material.BAMBOO);

    private final Map<UUID, ShuffleTarget> playerTargets = new HashMap<>();

    private int currentRound = 0;
    private int roundTimer;
    private int roundDuration;
    private BukkitTask roundTask;
    private final Set<UUID> completedRound = new HashSet<>();

    // Shared RNG for RANDOM mode fairness — both players get the same difficulty
    // sequence.
    private Random sharedRandom;
    // Track consecutive hard rounds for hard-lock prevention.
    private int consecutiveHardRounds = 0;

    public BlockShuffleInstance(DeathSwapPlugin plugin, String arenaId, ConfigManager.ArenaConfig config) {
        super(plugin, arenaId, config);
        loadTargets();
        // Initialize shared RNG with a fixed seed so all players share the same
        // sequence.
        this.sharedRandom = new Random(System.nanoTime());
    }

    /**
     * Resolve the difficulty tier for the current round based on the configured
     * DifficultyMode.
     * Includes hard-lock prevention: max 2 consecutive hard rounds in RANDOM mode.
     */
    private int resolveDifficulty(int round) {
        int diff = switch (getConfig().difficultyMode) {
            case PROGRESSIVE -> round <= 3 ? 1 : round <= 6 ? 2 : round <= 9 ? 3 : 4;
            case THEMATIC_EASY -> 1;
            case THEMATIC_MEDIUM -> 2;
            case THEMATIC_HARD -> 3;
            case THEMATIC_EXTREME -> 4;
            case RANDOM -> {
                int[] pool = { 1, 2, 3, 4 };
                yield pool[sharedRandom.nextInt(pool.length)];
            }
            case BALANCED -> {
                // Pattern: Easy, Easy, Medium, repeat — avoids long sessions.
                int idx = (round - 1) % 3;
                yield idx < 2 ? 1 : 2;
            }
        };

        // Hard-lock prevention: if we have had 2+ consecutive hard rounds, downgrade.
        if (diff == 3) {
            consecutiveHardRounds++;
            if (consecutiveHardRounds > 2) {
                diff = 2; // Force medium
                consecutiveHardRounds = 0;
            }
        } else {
            consecutiveHardRounds = 0;
        }

        return diff;
    }

    public void reloadSettings() {
        loadTargets();
    }

    private void loadTargets() {
        targets.clear();
        ConfigManager.BlockShuffleConfig bsConfig = getPlugin().getConfigManager().getBlockShuffleConfig();
        if (bsConfig != null && !bsConfig.getEntries().isEmpty()) {
            for (ConfigManager.BlockShuffleEntry entry : bsConfig.getEntries()) {
                if (!entry.enabled())
                    continue;
                try {
                    Material mat = Material.valueOf(entry.material().toUpperCase());

                    // Filter out Nether/End materials if dimensions are disabled
                    if (!getConfig().netherEnabled && NETHER_MATERIALS.contains(mat))
                        continue;
                    if (!getConfig().endEnabled && END_MATERIALS.contains(mat))
                        continue;

                    String typeName = entry.type() != null ? entry.type().toUpperCase() : "STAND";
                    AssignmentType assignType = typeName.equals("CRAFT") ? AssignmentType.CRAFT : AssignmentType.STAND;
                    String name = entry.material().toLowerCase().replace("_", " ");
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    // Auto-bump rare biome materials to minimum difficulty 3
                    int effectiveDifficulty = entry.difficulty();
                    if (RARE_BIOME_MATERIALS.contains(mat) && effectiveDifficulty < 3) {
                        effectiveDifficulty = 3;
                    }
                    targets.add(new ShuffleTarget(mat, effectiveDifficulty, assignType, name));
                } catch (IllegalArgumentException e) {
                    getPlugin().getLogger().warning("Invalid material in blockshuffle.yml: " + entry.material());
                }
            }
        }

        // Fallback if config empty or all disabled
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
        playerTargets.clear();

        // Check maxItemsPerGame limit
        if (getConfig().maxItemsPerGame > 0 && currentRound > getConfig().maxItemsPerGame) {
            broadcastGame(Lang.get("game-ended"));
            stopGame();
            return;
        }

        // Determine difficulty tier using the configured DifficultyMode
        int difficulty = resolveDifficulty(currentRound);

        // Pick random target from difficulty tier
        List<ShuffleTarget> pool = targets.stream()
                .filter(t -> t.difficulty() == difficulty)
                .toList();

        if (pool.isEmpty()) {
            pool = new ArrayList<>(targets); // fallback to all
        }
        if (pool.isEmpty()) {
            if (targets.isEmpty())
                targets.addAll(TARGETS);
            pool = targets;
        }

        // Assign targets
        if (getConfig().blockShuffleRaceMode) {
            // Race Mode: Single target for everyone, infinite time
            ShuffleTarget sharedTarget = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            for (Player p : getAlivePlayers()) {
                playerTargets.put(p.getUniqueId(), sharedTarget);
            }
            roundDuration = Integer.MAX_VALUE;
        } else if (getConfig().blockShuffleUniqueTargets) {
            // Unique targets per player
            for (Player p : getAlivePlayers()) {
                ShuffleTarget t = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
                playerTargets.put(p.getUniqueId(), t);
            }
            roundDuration = getConfig().getRoundTime(difficulty);
        } else {
            // Classic mode: Same target for everyone
            ShuffleTarget sharedTarget = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            for (Player p : getAlivePlayers()) {
                playerTargets.put(p.getUniqueId(), sharedTarget);
            }
            roundDuration = getConfig().getRoundTime(difficulty);
        }

        roundTimer = roundDuration;

        // Announcements
        if (getConfig().blockShuffleUniqueTargets && !getConfig().blockShuffleRaceMode) {
            // Everyone has different target, broadcast generic message
            broadcastGame("&b&lROUND " + currentRound + " &7— &eObjectifs individuels assignés !");
        } else {
            // Everyone has same target (Race or Classic)
            // Pick one to display (they are all same)
            if (!getAlivePlayers().isEmpty()) {
                // Get ANY target
                ShuffleTarget display = playerTargets.values().iterator().next();
                String emoji = display.material().isBlock() ? "🧱" : "🎯";
                broadcastGame("&b&lROUND " + currentRound + " &7— " + emoji + " &eObtiens ou tiens-toi sur : "
                        + display.displayName());
            }
        }
        broadcastGame("&7Difficulté : " + getDifficultyStars(difficulty) + " &7| Temps : &e"
                + (roundDuration == Integer.MAX_VALUE ? "∞" : roundDuration + "s"));

        // Personal Titles / Sounds
        for (Player p : getAlivePlayers()) {
            ShuffleTarget t = playerTargets.get(p.getUniqueId());
            if (t == null)
                continue; // Should not happen

            p.showTitle(Title.title(
                    Component.text("ROUND " + currentRound, NamedTextColor.AQUA, TextDecoration.BOLD),
                    Component.text(t.displayName(), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));

            if (getPlugin().getSoundManager() != null) {
                getPlugin().getSoundManager().playSound("shuffle", p);
            }
        }

        // Update BossBar (Only works well if shared target, or we show generic info)
        if (getConfig().uiMode == UIMode.RICH && getBossBar() != null) {
            if (getConfig().blockShuffleUniqueTargets && !getConfig().blockShuffleRaceMode) {
                getBossBar().name(Component.text("Round " + currentRound + " : ", NamedTextColor.AQUA)
                        .append(Component.text("Objectif personnel", NamedTextColor.YELLOW)));
            } else {
                // Shared
                if (!playerTargets.isEmpty()) {
                    ShuffleTarget t = playerTargets.values().iterator().next();
                    String emoji = t.material().isBlock() ? "🧱" : "🎯";
                    getBossBar().name(Component.text("Round " + currentRound + " : ", NamedTextColor.AQUA)
                            .append(Component.text(emoji + " " + t.displayName(), NamedTextColor.YELLOW)));
                }
            }
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

                // Boss bar progress
                if (getConfig().uiMode == UIMode.RICH && getBossBar() != null && roundDuration != Integer.MAX_VALUE) {
                    float progress = Math.max(0, Math.min(1, (float) roundTimer / roundDuration));
                    getBossBar().progress(progress);
                    if (roundTimer <= 10) {
                        getBossBar().color(BossBar.Color.RED);
                    }
                }

                if (getConfig().uiMode == UIMode.RICH) {
                    // Active scan and action bar for players
                    for (Player p : getAlivePlayers()) {
                        ShuffleTarget t = playerTargets.get(p.getUniqueId());
                        if (t == null)
                            continue;

                        if (!completedRound.contains(p.getUniqueId())) {
                            // Active Scan to see if they hold it, have it, or stand on it
                            boolean hasCompleted = false;
                            if (p.getInventory().contains(t.material())) {
                                hasCompleted = true; // In inventory (or held)
                            } else if (t.material().isBlock()) {
                                org.bukkit.block.Block below = p.getLocation().subtract(0, 1, 0).getBlock();
                                if (below.getType() == t.material()) {
                                    hasCompleted = true; // Standing on it
                                }
                            }
                            if (hasCompleted) {
                                completeRound(p); // Mark as completed instantly this tick
                            }
                        }

                        if (getConfig().blockShuffleRaceMode) {
                            // RACE MODE ACTION BAR
                            p.sendActionBar(Component.text("🏁 RACE: " + t.displayName(), NamedTextColor.GOLD,
                                    TextDecoration.BOLD));
                            continue;
                        }

                        if (!completedRound.contains(p.getUniqueId())) {
                            if (roundTimer <= 10) {
                                p.sendActionBar(
                                        Component.text("⚠ " + roundTimer + "s — " + t.displayName() + " ⚠",
                                                NamedTextColor.RED, TextDecoration.BOLD));
                            } else {
                                p.sendActionBar(Component.text(t.displayName() + " — " + roundTimer + "s",
                                        NamedTextColor.AQUA));
                            }
                        } else {
                            p.sendActionBar(
                                    Component.text("✅ Complété ! En attente des autres...", NamedTextColor.GREEN));
                        }
                    }
                } else if (getConfig().uiMode == UIMode.RICH && getConfig().blockShuffleRaceMode) {
                    // Handled in loop above
                } else {
                    // CLEAN Mode: Chat notifications
                    if (roundDuration != Integer.MAX_VALUE) {
                        if (roundTimer == 60 || roundTimer == 30 || roundTimer == 10
                                || (roundTimer <= 5 && roundTimer > 0)) {
                            broadcastGame("&c⚠ FIN DU ROUND DANS " + roundTimer + " SECONDES ⚠");
                            if (roundTimer <= 5 && getPlugin().getSoundManager() != null) {
                                getPlugin().getSoundManager().playSoundAll("countdown-tick", getGamePlayers());
                            }
                        }
                        if (roundTimer % 60 == 0 && roundTimer > 0) {
                            // Reminder
                            for (Player p : getAlivePlayers()) {
                                ShuffleTarget t = playerTargets.get(p.getUniqueId());
                                if (t != null) {
                                    String emoji = t.material().isBlock() ? "🧱" : "🎯";
                                    p.sendMessage(Lang.get("game-prefix")
                                            + Component.text(
                                                    "Rappel: " + emoji + " Obtiens ou tiens-toi sur : "
                                                            + t.displayName(),
                                                    NamedTextColor.YELLOW));
                                }
                            }
                        }
                    }
                }

                // All alive completed?
                if (allAliveCompleted()) {
                    cancel();
                    broadcastGame("&a&lTout le monde a réussi ! Round suivant...");
                    Bukkit.getScheduler().runTaskLater(getPlugin(), () -> startNextRound(), 60L);
                    return;
                }

                if (roundTimer <= 0 && roundDuration != Integer.MAX_VALUE) {
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
        broadcastGame(Lang.get("ds-round-fail-everyone"));

        Set<Player> failed = new HashSet<>();
        for (Player p : new HashSet<>(getAlivePlayers())) {
            if (!completedRound.contains(p.getUniqueId())) {
                failed.add(p);
            }
        }

        if (failed.size() == getAlivePlayers().size() && !failed.isEmpty()) {
            // Mercy Rule: Everyone failed, nobody is eliminated
            broadcastGame(Lang.get("ds-round-mercy"));
        } else {
            // Normal elimination
            for (Player p : failed) {
                broadcastGame(Lang.get("ds-round-fail", "%player%", p.getName()));
                eliminatePlayer(p);
            }
        }

        checkWinCondition();

        if (getState() == GameState.RUNNING) {
            // Context: User wants game to continue until global time OR "Everyone loses"
            // logic.
            // If we have survivors, continue.
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

        ShuffleTarget t = playerTargets.get(player.getUniqueId());
        if (t == null)
            return false;
        if (blockType != t.material())
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

        ShuffleTarget t = playerTargets.get(player.getUniqueId());
        if (t == null)
            return false;
        if (itemType != t.material())
            return false;

        completeRound(player);
        return true;
    }

    /**
     * Mark a player as having completed the round.
     */
    private void completeRound(Player player) {
        completedRound.add(player.getUniqueId());

        if (getConfig().blockShuffleRaceMode) {
            broadcastGame(Lang.get("ds-round-success", "%player%", player.getName(), "%count%", "1", "%total%", "1"));
            if (getPlugin().getSoundManager() != null) {
                getPlugin().getSoundManager().playSound("victory", player);
            }
            broadcastGame(Lang.get("game-winner", "%winner%", player.getName()));
            stopGame();
            return;
        }

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
    /**
     * Get the target for a specific player.
     * Replaces getCurrentTarget.
     */
    public ShuffleTarget getPlayerTarget(Player player) {
        return playerTargets.get(player.getUniqueId());
    }

    /**
     * Deprecated, kept for safety but should not be used if unique targets are
     * enabled.
     * Returns the target of the first player found.
     */
    public ShuffleTarget getCurrentTarget() {
        if (playerTargets.isEmpty())
            return null;
        return playerTargets.values().iterator().next();
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
        playerTargets.clear();
        super.stopGame();
    }

    @Override
    public void handleDeath(Player player) {
        // In BlockShuffle, death does not eliminate you (unless time runs out).
        // call super.handleDeath(player) ONLY if you want to eliminate them.
        // Here we just notify.
        broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-death", "%player%", player.getName()));

        // Stats can still be recorded
        if (getPlugin().getConfigManager().isStatsEnabled() && getPlugin().getStatsManager() != null) {
            getPlugin().getStatsManager().addDeath(player.getUniqueId(), player.getName());
        }
    }
}
