package be.dualsfwshield.deathswap;

import be.dualsfwshield.deathswap.vote.VoteManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a single DeathSwap game instance (one arena).
 * Manages its own state, players, timers, bossbar, and game loop.
 * Subclassed by DeathShuffleInstance and BlockShuffleInstance.
 */
public class GameInstance {

    private final DeathSwapPlugin plugin;
    private final String arenaId;
    private final ConfigManager.ArenaConfig config;

    private GameState state = GameState.WAITING;
    private boolean testMode = false;

    // Player sets
    private final Set<Player> lobbyPlayers = new HashSet<>();
    private final Set<Player> readyPlayers = new HashSet<>();
    private final Set<Player> gamePlayers = new HashSet<>(); // All in-game (alive + dead)
    private final Set<Player> alivePlayers = new HashSet<>();
    private final Set<Player> spectators = new HashSet<>();

    // Timers
    private int globalTimer;
    private int swapTimer;
    private int currentSwapInterval;

    // BossBar
    private BossBar bossBar;

    // Game loop task
    private BukkitTask gameLoopTask;

    // Track game start time for survival time stats
    private long gameStartEpoch;

    public GameInstance(DeathSwapPlugin plugin, String arenaId, ConfigManager.ArenaConfig config) {
        this.plugin = plugin;
        this.arenaId = arenaId;
        this.config = config;
    }

    // =========================================
    // LOBBY MANAGEMENT
    // =========================================

    /**
     * Add a player to this arena's lobby.
     */
    public void joinLobby(Player player) {
        // Check if game is starting or full
        if (state == GameState.STARTING) {
            sendMessage(player, "&cUne partie est en cours de lancement...");
            return;
        }
        if (state == GameState.RUNNING) {
            sendMessage(player, "&cUne partie est déjà en cours !");
            return;
        }
        if (lobbyPlayers.size() >= config.maxPlayers) {
            sendMessage(player, "&cL'arène est pleine !");
            return;
        }

        lobbyPlayers.add(player);
        readyPlayers.remove(player);
        plugin.getArenaManager().addPlayerToArena(player, arenaId);

        // Teleport to lobby world
        World lobbyWorld = Bukkit.getWorld(config.lobbyWorld);
        if (lobbyWorld != null) {
            player.teleport(lobbyWorld.getSpawnLocation());
        } else {
            // Try loading via Multiverse
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load " + config.lobbyWorld);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World w = Bukkit.getWorld(config.lobbyWorld);
                if (w != null) {
                    player.teleport(w.getSpawnLocation());
                } else {
                    sendMessage(player, "&cImpossible de charger le monde lobby !");
                }
            }, 20L);
        }

        setupLobbyPlayer(player);
        broadcastLobby("&e" + player.getName() + " &7a rejoint l'arène ! &8(" + lobbyPlayers.size() + "/"
                + config.maxPlayers + ")");
    }

    /**
     * Setup a player's state for the lobby.
     */
    public void setupLobbyPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);

        // Remove all potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Give ready items (delayed by 2 ticks for safety)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            giveLobbyItems(player);
        }, 2L);
    }

    /**
     * Give the lobby items (ready toggle + hub return).
     */
    public void giveLobbyItems(Player player) {
        player.getInventory().clear();

        // Slot 4: Not Ready
        ItemStack notReady = new ItemStack(Material.RED_CONCRETE);
        ItemMeta notReadyMeta = notReady.getItemMeta();
        notReadyMeta.displayName(Component.text("Pas Prêt ", NamedTextColor.RED)
                .append(Component.text("(Clic Droit)", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        notReady.setItemMeta(notReadyMeta);
        player.getInventory().setItem(4, notReady);

        // Slot 8: Return to Hub
        ItemStack hubReturn = new ItemStack(Material.RED_BED);
        ItemMeta hubMeta = hubReturn.getItemMeta();
        hubMeta.displayName(Component.text("Retour au Hub ", NamedTextColor.GOLD)
                .append(Component.text("(Clic Droit)", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        hubReturn.setItemMeta(hubMeta);
        player.getInventory().setItem(8, hubReturn);
    }

    /**
     * Toggle a player's ready state.
     */
    public void toggleReady(Player player) {
        if (!lobbyPlayers.contains(player))
            return;

        if (readyPlayers.contains(player)) {
            // Unready
            readyPlayers.remove(player);
            broadcastLobby("&e" + player.getName() + " &cn'est plus prêt.");

            // Update item to Not Ready
            ItemStack notReady = new ItemStack(Material.RED_CONCRETE);
            ItemMeta meta = notReady.getItemMeta();
            meta.displayName(Component.text("Pas Prêt ", NamedTextColor.RED)
                    .append(Component.text("(Clic Droit)", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
            notReady.setItemMeta(meta);
            player.getInventory().setItem(4, notReady);
        } else {
            // Ready
            readyPlayers.add(player);
            broadcastLobby("&e" + player.getName() + " &aest prêt !");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            // Update item to Ready
            ItemStack ready = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta meta = ready.getItemMeta();
            meta.displayName(Component.text("PRÊT ! ", NamedTextColor.GREEN)
                    .append(Component.text("(Clic pour annuler)", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
            ready.setItemMeta(meta);
            player.getInventory().setItem(4, ready);

            // Check if all ready
            checkAllReady();
        }
    }

    /**
     * Remove a player from this arena.
     */
    public void removePlayer(Player player) {
        lobbyPlayers.remove(player);
        readyPlayers.remove(player);
        gamePlayers.remove(player);
        alivePlayers.remove(player);
        spectators.remove(player);
        plugin.getArenaManager().removePlayer(player);

        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }

        // If game is running, check win condition
        if (state == GameState.RUNNING) {
            checkWinCondition();
        }
    }

    /**
     * Send a player back to the hub.
     */
    public void sendToHub(Player player) {
        removePlayer(player);
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);

        String hubWorld = plugin.getConfigManager().getHubWorld();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " " + hubWorld);
    }

    // =========================================
    // GAME START SEQUENCE
    // =========================================

    /**
     * Check if all lobby players are ready and enough to start.
     */
    public void checkAllReady() {
        if (state != GameState.WAITING)
            return;
        if (lobbyPlayers.size() < config.minPlayers)
            return;
        if (readyPlayers.size() != lobbyPlayers.size())
            return;

        broadcastLobby("&aTout le monde est prêt ! Lancement imminent...");
        Bukkit.getScheduler().runTaskLater(plugin, () -> startGame(false), 60L); // 3 seconds
    }

    /**
     * Start the game sequence.
     *
     * @param debug if true, bypasses minimum player checks
     */
    public void startGame(boolean debug) {
        if (state != GameState.WAITING)
            return;

        this.testMode = debug;
        state = GameState.STARTING;

        // --- Seed Voting or Random Pick ---
        if (plugin.getConfigManager().isVotingEnabled() && plugin.getVoteManager() != null
                && !config.seeds.isEmpty() && lobbyPlayers.size() >= 2) {
            // Start a vote, then continue with the winner
            plugin.getVoteManager().startVote(this, config.seeds, lobbyPlayers, (seed) -> {
                broadcastLobby("&a🗳 Résultat du vote : &l" + seed.name());
                continueStartWithSeed(seed);
            });
        } else {
            // No voting — pick random seed
            SeedEntry seed;
            if (config.seeds.isEmpty()) {
                seed = new SeedEntry("0", "Random World");
            } else {
                seed = config.seeds.get(ThreadLocalRandom.current().nextInt(config.seeds.size()));
            }
            broadcastLobby("&aStructure choisie : &l" + seed.name());
            continueStartWithSeed(seed);
        }
    }

    /**
     * Continue the start sequence after seed selection.
     */
    private void continueStartWithSeed(SeedEntry seed) {
        // Reset world via CyberWorldReset
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "cwr edit " + config.gameWorld + " setSeed " + seed.seed());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cwr reset " + config.gameWorld);
            startCountdown();
        }, 5L);
    }

    /**
     * Countdown sequence after world reset.
     */
    private void startCountdown() {
        final int waitTime = config.loadTime;
        broadcastLobby("&aGénération du terrain... Attente de " + waitTime + " secondes.");

        new BukkitRunnable() {
            int remaining = waitTime;

            @Override
            public void run() {
                if (state != GameState.STARTING) {
                    cancel();
                    return;
                }

                if (remaining <= 5 && remaining > 0) {
                    broadcastLobby("&eTéléportation dans " + remaining + "...");
                    if (plugin.getSoundManager() != null) {
                        for (Player p : lobbyPlayers) {
                            plugin.getSoundManager().playSound("countdown-tick", p);
                        }
                    }
                }

                if (remaining <= 0) {
                    cancel();
                    if (plugin.getSoundManager() != null) {
                        for (Player p : lobbyPlayers) {
                            plugin.getSoundManager().playSound("countdown-go", p);
                        }
                    }
                    onCountdownFinished();
                    return;
                }

                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Called when the countdown is finished. Load world, set rules, teleport
     * players.
     */
    private void onCountdownFinished() {
        // Load world via Multiverse
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load " + config.gameWorld);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Set game rules
            setGameRules();

            // Initialize boss bar
            currentSwapInterval = config.getNextSwapInterval();
            if (config.uiMode == UIMode.RICH) {
                bossBar = BossBar.bossBar(
                        Component.text("Prochain Swap", NamedTextColor.GOLD),
                        1.0f,
                        BossBar.Color.YELLOW,
                        BossBar.Overlay.NOTCHED_10);
            }

            // Teleport players
            teleportPlayersToGame();

            // Delayed: spread players and start game loop
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                spreadPlayers();

                // Apply spawn protection
                for (Player p : alivePlayers) {
                    p.addPotionEffect(new PotionEffect(
                            PotionEffectType.RESISTANCE, config.spawnProtection * 20, 254, false, false));
                }

                // Verify player count
                if (!testMode && alivePlayers.size() < config.minPlayers) {
                    broadcastLobby("&cPas assez de joueurs. Annulation.");
                    state = GameState.WAITING;
                    cleanup();
                    return;
                }

                // Start!
                state = GameState.RUNNING;
                globalTimer = config.maxGameTime;
                swapTimer = currentSwapInterval;
                gameStartEpoch = System.currentTimeMillis();

                // Record games played for stats
                if (plugin.getConfigManager().isStatsEnabled() && plugin.getStatsManager() != null) {
                    for (Player p : alivePlayers) {
                        plugin.getStatsManager().addGamePlayed(p.getUniqueId(), p.getName());
                    }
                }

                broadcastGame("&a&lLA PARTIE COMMENCE !");

                if (plugin.getSoundManager() != null) {
                    plugin.getSoundManager().playSoundAll("game-start", gamePlayers);
                }

                if (config.gameType == GameType.DEATHSWAP) {
                    if (config.pvpEnabled) {
                        broadcastGame("&ePvP ACTIVÉ ! &7(Invulnérabilité " + config.spawnProtection + "s)");
                    } else {
                        broadcastGame(
                                "&ePvP DÉSACTIVÉ (Mobs OK) ! &7(Invulnérabilité " + config.spawnProtection + "s)");
                    }
                    if (!config.netherEnabled || !config.endEnabled) {
                        broadcastGame("&cNether et End désactivés.");
                    }
                }

                startGameLoop();
            }, 20L);
        }, 5L);
    }

    /**
     * Set game rules on the game world via Multiverse.
     */
    /**
     * Set game rules on the game world via Bukkit API.
     */
    private void setGameRules() {
        World world = Bukkit.getWorld(config.gameWorld);
        if (world == null)
            return;

        // Apply specialized settings
        world.setPVP(config.pvpEnabled);

        // Apply gamerules from map
        for (Map.Entry<String, String> entry : config.gamerules.entrySet()) {
            GameRule<?> rule = GameRule.getByName(entry.getKey());
            if (rule != null) {
                if (rule.getType() == Boolean.class) {
                    world.setGameRule((GameRule<Boolean>) rule, Boolean.parseBoolean(entry.getValue()));
                } else if (rule.getType() == Integer.class) {
                    try {
                        world.setGameRule((GameRule<Integer>) rule, Integer.parseInt(entry.getValue()));
                    } catch (NumberFormatException e) {
                        plugin.getLogger()
                                .warning("Invalid integer for gamerule " + entry.getKey() + ": " + entry.getValue());
                    }
                }
            } else {
                // Fallback to command dispatch if unknown (maybe custom/modded rule)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "execute in " + world.getName() + " run gamerule " + entry.getKey() + " " + entry.getValue());
            }
        }
    }

    /**
     * Teleport all ready lobby players to the game world.
     */
    private void teleportPlayersToGame() {
        World gameWorld = Bukkit.getWorld(config.gameWorld);
        if (gameWorld == null) {
            plugin.getLogger().severe("Game world '" + config.gameWorld + "' is not loaded!");
            state = GameState.WAITING;
            return;
        }

        for (Player player : new HashSet<>(lobbyPlayers)) {
            if (!readyPlayers.contains(player) && !testMode)
                continue;

            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);

            // TP high up
            player.teleport(new Location(gameWorld, 0, 200, 0));

            gamePlayers.add(player);
            alivePlayers.add(player);

            // Set survival after TP
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                if (config.uiMode == UIMode.RICH && bossBar != null) {
                    player.showBossBar(bossBar);
                }
            }, 2L);
        }

        lobbyPlayers.clear();
        readyPlayers.clear();
    }

    /**
     * Spread players using spreadplayers command.
     */
    private void spreadPlayers() {
        if (alivePlayers.isEmpty())
            return;

        StringBuilder names = new StringBuilder();
        Player firstPlayer = null;
        for (Player p : alivePlayers) {
            if (firstPlayer == null)
                firstPlayer = p;
            names.append(" ").append(p.getName());
        }

        if (firstPlayer != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "execute at " + firstPlayer.getName() + " run spreadplayers 0 0 10 100 false" + names);
        }
    }

    // =========================================
    // GAME LOOP
    // =========================================

    /**
     * Start the main game loop (ticks every second).
     * Protected so subclasses can override with their own loop.
     */
    protected void startGameLoop() {
        gameLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.RUNNING) {
                    cancel();
                    return;
                }

                globalTimer--;
                swapTimer--;

                // Update UI based on mode
                if (config.uiMode == UIMode.RICH) {
                    updateBossBar();
                    updateActionBar();
                } else {
                    updateCleanUI();
                }

                // Swap check
                if (swapTimer <= 0) {
                    performSwap();

                    // Assign challenges after swap
                    if (plugin.getConfigManager().isChallengesEnabled() && plugin.getChallengeManager() != null) {
                        plugin.getChallengeManager().assignChallenges(GameInstance.this);
                    }

                    // Get next interval (random or fixed)
                    currentSwapInterval = config.getNextSwapInterval();
                    swapTimer = currentSwapInterval;
                }

                // Global time check
                if (globalTimer <= 0) {
                    broadcastGame("&cTemps écoulé ! Match Nul !");
                    stopGame();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Update the boss bar display.
     */
    private void updateBossBar() {
        if (bossBar == null)
            return;

        int minutes = swapTimer / 60;
        int seconds = swapTimer % 60;
        String timeText = String.format("%d:%02d", minutes, seconds);

        bossBar.name(Component.text("Prochain Swap : ", NamedTextColor.GOLD)
                .append(Component.text(timeText, NamedTextColor.YELLOW)));

        float progress = Math.max(0, Math.min(1, (float) swapTimer / currentSwapInterval));
        bossBar.progress(progress);

        if (swapTimer <= 10) {
            bossBar.color(BossBar.Color.RED);
        } else {
            bossBar.color(BossBar.Color.YELLOW);
        }
    }

    /**
     * Update action bar for all alive players.
     */
    private void updateActionBar() {
        for (Player p : alivePlayers) {
            if (swapTimer <= 10) {
                p.sendActionBar(Component.text("⚠ SWAP DANS " + swapTimer + " SECONDES ⚠",
                        NamedTextColor.RED, TextDecoration.BOLD));
                if (swapTimer <= 5) {
                    if (plugin.getSoundManager() != null) {
                        plugin.getSoundManager().playSound("countdown-tick", p);
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);
                    }
                }
            } else {
                p.sendActionBar(Component.text("Survivants: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(alivePlayers.size()), NamedTextColor.WHITE)));
            }
        }
    }

    // =========================================
    // SWAP MECHANIC
    // =========================================

    /**
     * Perform the swap: circular rotation of all alive players' positions.
     */
    public void performSwap() {
        broadcastGame("&6&lSWAP ! Échange des positions...");

        List<Player> survivors = new ArrayList<>(alivePlayers);
        int count = survivors.size();

        if (count < 2) {
            if (testMode) {
                broadcastGame("&7(Mode Test: Pas de swap en solo)");
            }
            return;
        }

        // Save all positions BEFORE moving anyone
        Location[] locations = new Location[count];
        for (int i = 0; i < count; i++) {
            Player p = survivors.get(i);
            p.leaveVehicle();
            locations[i] = p.getLocation().clone();
            if (plugin.getSoundManager() != null) {
                plugin.getSoundManager().playSound("swap", p);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
        }

        // Circular rotation: each player goes to the next player's position
        for (int i = 0; i < count; i++) {
            Player current = survivors.get(i);
            int nextIndex = (i + 1) % count;
            Location targetLoc = locations[nextIndex];
            Player swappedWith = survivors.get(nextIndex);

            current.teleport(targetLoc);
            current.showTitle(Title.title(
                    Component.text("SWAP !", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("Tu as pris la place de " + swappedWith.getName(), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));
        }
    }

    // =========================================
    // DEATH & SPECTATOR
    // =========================================

    /**
     * Handle a player death in-game.
     */
    public void handleDeath(Player player) {
        if (!alivePlayers.contains(player))
            return;

        alivePlayers.remove(player);
        spectators.add(player);

        // Record stats
        if (plugin.getConfigManager().isStatsEnabled() && plugin.getStatsManager() != null) {
            plugin.getStatsManager().addDeath(player.getUniqueId(), player.getName());

            // Track killer for kill stats
            Player killer = player.getKiller();
            if (killer != null && alivePlayers.contains(killer)) {
                plugin.getStatsManager().addKill(killer.getUniqueId(), killer.getName());
            }

            // Survival time
            long survivalSeconds = (System.currentTimeMillis() - gameStartEpoch) / 1000;
            plugin.getStatsManager().addSurvivalTime(player.getUniqueId(), survivalSeconds);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setGameMode(GameMode.SPECTATOR);
            broadcastGame("&c" + player.getName() + " est mort !");

            if (plugin.getSoundManager() != null) {
                plugin.getSoundManager().playSoundAll("death", gamePlayers);
            } else {
                for (Player p : gamePlayers) {
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1f);
                }
            }

            giveSpectatorTools(player);
            checkWinCondition();
        }, 1L);
    }

    /**
     * Give spectator compass and send clickable TP menu.
     */
    public void giveSpectatorTools(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getInventory().clear();

            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            meta.displayName(Component.text("Menu Spectateur ", NamedTextColor.YELLOW)
                    .append(Component.text("(Clic Droit)", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
            compass.setItemMeta(meta);
            player.getInventory().setItem(0, compass);

            sendSpectatorMenu(player);
        }, 2L);
    }

    /**
     * Send clickable teleport menu to a spectator.
     */
    public void sendSpectatorMenu(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                TextDecoration.STRIKETHROUGH));
        player.sendMessage(Component.text("MODE SPECTATEUR", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Cliquez sur un joueur pour vous téléporter :", NamedTextColor.GRAY));

        for (Player alive : alivePlayers) {
            Component tpButton = Component.text("[TP vers " + alive.getName() + "]", NamedTextColor.GREEN)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/ds tp " + alive.getName()))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            Component.text("Téléportation vers " + alive.getName(), NamedTextColor.YELLOW)));
            player.sendMessage(tpButton);
        }

        player.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                TextDecoration.STRIKETHROUGH));
    }

    // =========================================
    // WIN CONDITION
    // =========================================

    /**
     * Check if the game should end (1 or 0 survivors).
     */
    public void checkWinCondition() {
        if (state != GameState.RUNNING)
            return;

        if (testMode && alivePlayers.size() == 1) {
            return; // Solo test mode, don't end
        }

        if (alivePlayers.size() <= 1) {
            state = GameState.ENDED;

            broadcastGame("&6&lPARTIE TERMINÉE !");

            if (alivePlayers.size() == 1) {
                Player winner = alivePlayers.iterator().next();
                broadcastGame("&a&lVICTOIRE DE " + winner.getName() + " !");
                winner.showTitle(Title.title(
                        Component.text("VICTOIRE", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text("Félicitations !", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(5), Duration.ofMillis(500))));

                if (plugin.getSoundManager() != null) {
                    plugin.getSoundManager().playSound("win", winner);
                }

                // Record win stat
                if (plugin.getConfigManager().isStatsEnabled() && plugin.getStatsManager() != null) {
                    plugin.getStatsManager().addWin(winner.getUniqueId(), winner.getName());
                    // Record survival time for winner
                    long survivalSeconds = (System.currentTimeMillis() - gameStartEpoch) / 1000;
                    plugin.getStatsManager().addSurvivalTime(winner.getUniqueId(), survivalSeconds);
                }
            } else {
                broadcastGame("&7Aucun survivant.");
            }

            Bukkit.getScheduler().runTaskLater(plugin, this::stopGame, 100L); // 5 seconds
        }
    }

    // =========================================
    // GAME STOP & CLEANUP
    // =========================================

    /**
     * Stop the game and send all players to hub.
     */
    public void stopGame() {
        state = GameState.ENDED;

        // Cancel game loop
        if (gameLoopTask != null) {
            gameLoopTask.cancel();
            gameLoopTask = null;
        }

        // Clear challenges
        if (plugin.getChallengeManager() != null) {
            plugin.getChallengeManager().clearAll();
        }

        // Remove bossbar
        if (bossBar != null) {
            for (Player p : gamePlayers) {
                p.hideBossBar(bossBar);
            }
            bossBar = null;
        }

        // Send all players to hub
        String hubWorld = plugin.getConfigManager().getHubWorld();
        for (Player p : new HashSet<>(gamePlayers)) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            plugin.getArenaManager().removePlayer(p);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + p.getName() + " " + hubWorld);
        }

        cleanup();
        state = GameState.WAITING;
    }

    /**
     * Cleanup all internal state.
     */
    private void cleanup() {
        lobbyPlayers.clear();
        readyPlayers.clear();
        gamePlayers.clear();
        alivePlayers.clear();
        spectators.clear();
        testMode = false;
    }

    // =========================================
    // UTILITY
    // =========================================

    /**
     * Send a prefixed message to a specific player.
     */
    protected void sendMessage(Player player, String message) {
        String prefix = plugin.getConfigManager().getPrefix(getGameType());
        player.sendMessage(colorize(prefix + " " + message));
    }

    /**
     * Broadcast a message to all lobby players.
     */
    public void broadcastLobby(String message) {
        String prefix = plugin.getConfigManager().getPrefix(getGameType());
        Component msg = colorize(prefix + " " + message);
        for (Player p : lobbyPlayers) {
            p.sendMessage(msg);
        }
    }

    /**
     * Broadcast a message to all game players (alive + spectators).
     */
    public void broadcastGame(String message) {
        String prefix = plugin.getConfigManager().getPrefix(getGameType());
        Component msg = colorize(prefix + " " + message);
        for (Player p : gamePlayers) {
            p.sendMessage(msg);
        }
    }

    /**
     * Convert legacy color codes (&) to a Component.
     */
    protected Component colorize(String text) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(text);
    }

    // --- Getters (protected for subclass access) ---

    public GameType getGameType() {
        return config.gameType;
    }

    public DeathSwapPlugin getPlugin() {
        return plugin;
    }

    public String getArenaId() {
        return arenaId;
    }

    public GameState getState() {
        return state;
    }

    protected void setState(GameState state) {
        this.state = state;
    }

    public ConfigManager.ArenaConfig getConfig() {
        return config;
    }

    public Set<Player> getLobbyPlayers() {
        return lobbyPlayers;
    }

    public Set<Player> getReadyPlayers() {
        return readyPlayers;
    }

    public Set<Player> getGamePlayers() {
        return gamePlayers;
    }

    public Set<Player> getAlivePlayers() {
        return alivePlayers;
    }

    public Set<Player> getSpectators() {
        return spectators;
    }

    public Set<Player> getAllPlayers() {
        Set<Player> all = new HashSet<>();
        all.addAll(lobbyPlayers);
        all.addAll(gamePlayers);
        // spectators are usually in gamePlayers too if they died, but if joined as
        // spec...
        all.addAll(spectators);
        return all;
    }

    public Location getLobbyLocation() {
        World w = Bukkit.getWorld(config.lobbyWorld);
        return w != null ? w.getSpawnLocation() : null;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    /**
     * Update chat notifications for CLEAN mode.
     */
    protected void updateCleanUI() {
        // Critical swap warnings
        if (swapTimer == 60 || swapTimer == 30 || swapTimer == 10 || (swapTimer <= 5 && swapTimer > 0)) {
            broadcastGame("&c⚠ SWAP DANS " + swapTimer + " SECONDES ⚠");

            // Play sound even in CLEAN mode (user only asked for UI cleanup)
            if (swapTimer <= 5) {
                if (plugin.getSoundManager() != null) {
                    plugin.getSoundManager().playSoundAll("countdown-tick", gamePlayers);
                }
            }
        }
    }
}
