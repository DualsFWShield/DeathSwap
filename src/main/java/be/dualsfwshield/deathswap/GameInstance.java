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
    private long gracePeriodEndTime = 0;

    public boolean isGracePeriod() {
        return System.currentTimeMillis() < gracePeriodEndTime;
    }

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
            sendMessage(player, be.dualsfwshield.deathswap.util.Lang.get("game-start-countdown"));
            return;
        }
        if (state == GameState.RUNNING) {
            sendMessage(player, be.dualsfwshield.deathswap.util.Lang.get("game-started"));
            return;
        }
        if (lobbyPlayers.size() >= config.maxPlayers) {
            sendMessage(player, be.dualsfwshield.deathswap.util.Lang.get("game-join-full"));
            return;
        }

        lobbyPlayers.add(player);
        readyPlayers.remove(player);
        plugin.getArenaManager().addPlayerToArena(player, arenaId);

        // Teleport to lobby world via Multiverse exact destination
        // Teleport to lobby world via Multiverse exact destination
        World lobbyWorld = Bukkit.getWorld(config.lobbyWorld);
        if (lobbyWorld == null) {
            // Force load to get the custom spawn location
            lobbyWorld = Bukkit.createWorld(new org.bukkit.WorldCreator(config.lobbyWorld));
        }

        if (lobbyWorld != null) {
            mvtp(player, config.lobbyWorld, lobbyWorld.getSpawnLocation());
        } else {
            sendMessage(player, be.dualsfwshield.deathswap.util.Lang.get("error-lobby-world-not-found", "%world%", config.lobbyWorld));
        }

        setupLobbyPlayer(player);
        broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("game-join", "%player%", player.getName(), "%count%", String.valueOf(lobbyPlayers.size()), "%max%", String.valueOf(config.maxPlayers)));
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
        notReadyMeta.displayName(be.dualsfwshield.deathswap.util.Lang.getComponent("item-not-ready")
                .decoration(TextDecoration.ITALIC, false));
        notReady.setItemMeta(notReadyMeta);
        player.getInventory().setItem(4, notReady);

        // Slot 8: Return to Hub
        ItemStack hubReturn = new ItemStack(Material.RED_BED);
        ItemMeta hubMeta = hubReturn.getItemMeta();
        hubMeta.displayName(be.dualsfwshield.deathswap.util.Lang.getComponent("item-hub-return")
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

        // Prevent unready during countdown if configured
        if (state == GameState.STARTING && config.preventCancelAfterCountdown) {
            sendMessage(player, be.dualsfwshield.deathswap.util.Lang.get("lobby-already-starting"));
            return;
        }

        if (readyPlayers.contains(player)) {
            // Unready
            readyPlayers.remove(player);
            broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("lobby-unready", "%player%", player.getName()));

            // Update item to Not Ready
            ItemStack notReady = new ItemStack(Material.RED_CONCRETE);
            ItemMeta meta = notReady.getItemMeta();
            meta.displayName(be.dualsfwshield.deathswap.util.Lang.getComponent("emoji-not-ready")
                    .color(NamedTextColor.RED)
                    .append(be.dualsfwshield.deathswap.util.Lang.getComponent("item-click-right").color(NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
            notReady.setItemMeta(meta);
            player.getInventory().setItem(4, notReady);

            // If game was starting, maybe cancel?
            if (state == GameState.STARTING && !config.startIfMinPlayersMet) {
                // If config says cancel on unready, we cancel
                // But current logic doesn't explicitly cancel task here, just relies on check
                // later?
                // Actually startCountdown() doesn't check ready status during run.
                // onCountdownFinished() checks alivePlayers size, not ready status.
                // So unready during countdown has no effect unless we add code here.
                // Let's force cancel if we drop below min players and !startIfMinPlayersMet
                if (readyPlayers.size() < config.minPlayers) {
                    broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("lobby-cancel-not-enough"));
                    state = GameState.WAITING;
                    // Note: The countdown task checks state != STARTING and will cancel itself.
                }
            }

        } else {
            // Ready
            readyPlayers.add(player);
            broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("lobby-ready", "%player%", player.getName()));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            // Update item to Ready
            ItemStack ready = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta meta = ready.getItemMeta();
            meta.displayName(be.dualsfwshield.deathswap.util.Lang.getComponent("emoji-ready")
                    .color(NamedTextColor.GREEN)
                    .append(be.dualsfwshield.deathswap.util.Lang.getComponent("item-click-cancel").color(NamedTextColor.GRAY))
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
        World hub = Bukkit.getWorld(hubWorld);
        if (hub != null) {
            mvtp(player, hubWorld, hub.getSpawnLocation());
        } else {
            // Fallback: basic mv tp
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "mvtp " + player.getName() + " e:" + hubWorld + ":0,64,0");
        }
    }

    /**
     * Teleport a player to an exact location in a Multiverse world.
     * Uses: /mvtp <player> e:<world>:<x>,<y>,<z>:<yaw>:<pitch>
     */
    /**
     * Teleport a player to an exact location in a Multiverse world.
     * Uses: /mvtp <player> e:<world>:<x>,<y>,<z>:<yaw>:<pitch>
     */
    private void mvtp(Player player, String worldName, Location loc) {
        // Try arena config first, then global
        String cmd = config.teleportCommand;
        if (cmd == null || cmd.isEmpty()) {
            cmd = plugin.getConfigManager().getTeleportCommand();
        }
        if (cmd == null || cmd.isEmpty()) {
            cmd = "mvtp %player% e:%world%:%x%,%y%,%z%:%yaw%:%pitch%";
        }

        cmd = cmd.replace("%player%", player.getName())
                .replace("%world%", worldName)
                .replace("%x%", String.valueOf(loc.getX()))
                .replace("%y%", String.valueOf(loc.getY()))
                .replace("%z%", String.valueOf(loc.getZ()))
                .replace("%yaw%", String.valueOf(loc.getYaw()))
                .replace("%pitch%", String.valueOf(loc.getPitch()));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
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

        broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("lobby-all-ready"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> startGame(false), 60L); // 3 seconds
    }

    /**
     * Start the game sequence.
     *
     * @param debug if true, bypasses minimum player checks
     */
    public void startGame(boolean debug) {
        if (state == GameState.RUNNING || state == GameState.STARTING) {
            return;
        }
        if (state != GameState.WAITING)
            return;

        this.testMode = debug;
        state = GameState.STARTING;

        // --- Seed Voting or Random Pick ---
        if (plugin.getConfigManager().isVotingEnabled() && plugin.getVoteManager() != null
                && !config.seeds.isEmpty() && lobbyPlayers.size() >= 2) {
            // Start a vote, then continue with the winner
            plugin.getVoteManager().startVote(this, config.seeds, lobbyPlayers, (seed) -> {
                broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("vote-result", "%seed%", seed.name()));
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
            broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("vote-random", "%seed%", seed.name()));
            continueStartWithSeed(seed);
        }
    }

    /**
     * Continue the start sequence after seed selection.
     */
    /**
     * Continue the start sequence after seed selection.
     */
    private void continueStartWithSeed(SeedEntry seed) {
        // Execute configured world reset commands
        List<String> commands = config.worldResetCommands;
        if (commands == null) {
            commands = plugin.getConfigManager().getWorldResetCommands();
        }

        if (commands != null && !commands.isEmpty()) {
            for (String cmd : commands) {
                String finalCmd = cmd.replace("%world%", config.gameWorld)
                        .replace("%seed%", seed.seed());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }
        }

        // Wait a bit before counting down (give time for async resets if any)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startCountdown();
        }, 20L); // Increased delay slightly to be safe
    }

    /**
     * Countdown sequence after world reset.
     */
    private void startCountdown() {
        final int waitTime = config.loadTime;
        broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("game-generating", "%time%", String.valueOf(waitTime)));

        new BukkitRunnable() {
            int remaining = waitTime;

            @Override
            public void run() {
                if (state != GameState.STARTING) {
                    cancel();
                    return;
                }

                if (remaining <= 5 && remaining > 0) {
                    broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("game-teleporting", "%time%", String.valueOf(remaining)));
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
                        be.dualsfwshield.deathswap.util.Lang.getComponent("bossbar-next-swap")
                                .color(NamedTextColor.GOLD),
                        1.0f,
                        BossBar.Color.YELLOW,
                        BossBar.Overlay.NOTCHED_10);
            }

            // Initialize grace period BEFORE teleport to ensure coverage
            long protectionMillis = config.spawnProtection * 1000L;
            // Add buffer for spread/loading logic
            gracePeriodEndTime = System.currentTimeMillis() + protectionMillis + 15000L;

            // Teleport players (handles spreading internal)
            teleportPlayersToGame();

            // No longer needed to call spreadPlayers delayed, handled in teleportPlayersToGame
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
                // Fallback to Multiverse command for unknown/modded rules
                plugin.getLogger().warning("Unknown GameRule '" + entry.getKey() + "', trying via Multiverse...");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "mv gamerule set " + entry.getKey() + " " + entry.getValue() + " " + world.getName());
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

        // Initialize teleportation tracker
        final int totalPlayers = lobbyPlayers.size();
        final java.util.concurrent.atomic.AtomicInteger teleportedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // We will clear lobbyPlayers and readyPlayers AFTER everyone is teleported or process individually
        Set<Player> playersToTeleport = new HashSet<>(lobbyPlayers);
        
        // Clear lobby sets now to prevent re-triggering? 
        // Better to clear them at the end or use a temp set.
        // The original code iterated `new HashSet<>(lobbyPlayers)`.
        
        lobbyPlayers.clear();
        readyPlayers.clear();

        for (Player player : playersToTeleport) {
            if (!this.testMode && !playersToTeleport.contains(player)) continue; // Safety

            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);

            gamePlayers.add(player);
            alivePlayers.add(player);
            
            // Async RTP
            teleportRandomlyAsync(player, gameWorld).thenAccept(success -> {
                 if (success) {
                     Bukkit.getScheduler().runTask(plugin, () -> {
                         player.setGameMode(GameMode.SURVIVAL);
                         if (config.uiMode == UIMode.RICH && bossBar != null) {
                             player.showBossBar(bossBar);
                         }
                         
                         // Apply spawn protection immediately upon arrival
                         int prot = config.spawnProtection;
                         player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, prot * 20, 255, false, false));
                         player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, prot * 20, 0, false, false));
                         player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, prot * 20, 0, false, false));
                         player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, prot * 20, 255, false, false));
                         
                         if (teleportedCount.incrementAndGet() == totalPlayers) {
                             // All teleported - trigger game start logic that was previously in spreadPlayers or delayed
                             // But wait, the original logic had a delayed task calling spreadPlayers() then doing game start.
                             // We should probably trigger that "post-teleport" logic here.
                             onAllPlayersTeleported();
                         }
                     });
                 } else {
                     // Retry or fail?
                     plugin.getLogger().warning("Failed to RTP player " + player.getName());
                 }
            });
        }
    }
    
    private void onAllPlayersTeleported() {
         // Verify player count
        if (!testMode) {
            if (alivePlayers.size() < config.minPlayers) {
                plugin.getLogger().warning("Not enough players to start (" + alivePlayers.size() + "/" + config.minPlayers + ")");
                broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("game-not-enough-players", "%count%", String.valueOf(alivePlayers.size()), "%min%", String.valueOf(config.minPlayers)));
                state = GameState.WAITING;
                cleanup();
                return;
            }
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

        broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-started"));

        if (plugin.getSoundManager() != null) {
            plugin.getSoundManager().playSoundAll("game-start", gamePlayers);
        }

        if (config.gameType == GameType.DEATHSWAP) {
            if (config.pvpEnabled) {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-pvp-on", "%time%", String.valueOf(config.spawnProtection)));
            } else {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-pvp-off", "%time%", String.valueOf(config.spawnProtection)));
            }
            if (!config.netherEnabled || !config.endEnabled) {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-nether-end-disabled"));
            }
        }

        startGameLoop();
    }

    /**
     * Async random teleport logic.
     */
    private java.util.concurrent.CompletableFuture<Boolean> teleportRandomlyAsync(Player player, World world) {
        java.util.concurrent.CompletableFuture<Boolean> future = new java.util.concurrent.CompletableFuture<>();
        
        findSafeLocationAsync(world, 5000).thenAccept(loc -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (loc != null) {
                    player.teleport(loc);
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            });
        });
        
        return future;
    }
    
    private java.util.concurrent.CompletableFuture<Location> findSafeLocationAsync(World world, int radius) {
        java.util.concurrent.CompletableFuture<Location> future = new java.util.concurrent.CompletableFuture<>();
        findSafeLocationRecursive(world, radius, 10, future);
        return future;
    }

    private void findSafeLocationRecursive(World world, int radius, int attempts, java.util.concurrent.CompletableFuture<Location> future) {
        if (attempts <= 0) {
            plugin.getLogger().warning("Could not find safe RTP location after retries.");
            future.complete(world.getSpawnLocation()); // Fallback
            return;
        }

        int x = ThreadLocalRandom.current().nextInt(-radius, radius);
        int z = ThreadLocalRandom.current().nextInt(-radius, radius);
        
        // Load chunk async
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
            
            Material block = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType();
            if (isSafeBlock(block)) {
                future.complete(loc);
            } else {
                // Retry
                findSafeLocationRecursive(world, radius, attempts - 1, future);
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            future.complete(world.getSpawnLocation());
            return null;
        });
    }

    private boolean isSafeBlock(Material mat) {
        if (mat == Material.LAVA || mat == Material.WATER || mat == Material.CACTUS || mat == Material.MAGMA_BLOCK) return false;
        if (mat.isAir()) return false;
        // Avoid leaves/trees for now?
        return mat.isSolid();
    }
    
    // Removed spreadPlayers() as it is replaced by RTP logic

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
                // plugin.getLogger().info("[Debug] GameLoop tick. State: " + state + ", Timer:
                // " + swapTimer);
                if (state != GameState.RUNNING) {
                    plugin.getLogger().warning("[Debug] Game loop running but state is " + state + ". Cancelling.");
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
                    broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-timeout"));
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

        bossBar.name(be.dualsfwshield.deathswap.util.Lang.getComponent("bossbar-next-swap")
                .color(NamedTextColor.GOLD)
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
                p.sendActionBar(be.dualsfwshield.deathswap.util.Lang.getComponent("actionbar-swap").replaceText(b -> b.matchLiteral("%time%").replacement(String.valueOf(swapTimer)))
                        .color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
                if (swapTimer <= 5) {
                    if (plugin.getSoundManager() != null) {
                        plugin.getSoundManager().playSound("countdown-tick", p);
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);
                    }
                }
            } else {
                p.sendActionBar(be.dualsfwshield.deathswap.util.Lang.getComponent("actionbar-survivors")
                        .color(NamedTextColor.GRAY)
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
        broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("swap-blindness"));

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
                    be.dualsfwshield.deathswap.util.Lang.getComponent("title-swap")
                            .color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true),
                    be.dualsfwshield.deathswap.util.Lang.getComponent("subtitle-swap").replaceText(b -> b.matchLiteral("%player%").replacement(swappedWith.getName()))
                            .color(NamedTextColor.YELLOW),
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
            broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-death", "%player%", player.getName()));

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
            meta.displayName(be.dualsfwshield.deathswap.util.Lang.getComponent("spectator-item-teleport")
                    .append(be.dualsfwshield.deathswap.util.Lang.getComponent("item-click-right").color(NamedTextColor.GRAY))
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
            Component tpButton = Component.text("[TP -> " + alive.getName() + "]", NamedTextColor.GREEN)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/ds tp " + alive.getName()))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            be.dualsfwshield.deathswap.util.Lang.getComponent("spectator-tp-hover", "%player%", alive.getName())));
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

            broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-ended"));

            if (alivePlayers.size() == 1) {
                Player winner = alivePlayers.iterator().next();
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-winner", "%winner%", winner.getName()));
                winner.showTitle(Title.title(
                        be.dualsfwshield.deathswap.util.Lang.getComponent("title-victory")
                                .color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true),
                        be.dualsfwshield.deathswap.util.Lang.getComponent("subtitle-victory")
                                .color(NamedTextColor.YELLOW),
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
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-draw"));
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
        World hub = Bukkit.getWorld(hubWorld);
        if (hub == null) {
            hub = Bukkit.createWorld(new org.bukkit.WorldCreator(hubWorld));
        }

        Location hubSpawn = (hub != null) ? hub.getSpawnLocation() : null;

        for (Player p : new HashSet<>(gamePlayers)) {
            p.setGameMode(org.bukkit.GameMode.ADVENTURE);
            p.getInventory().clear();
            plugin.getArenaManager().removePlayer(p);

            if (hubSpawn != null) {
                mvtp(p, hubWorld, hubSpawn);
            } else {
                p.sendMessage(Component.text("Erreur: Monde Hub introuvable (" + hubWorld + ")", NamedTextColor.RED));
            }
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
            broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("swap-warning", "%time%", String.valueOf(swapTimer)));

            // Play sound even in CLEAN mode (user only asked for UI cleanup)
            if (swapTimer <= 5) {
                if (plugin.getSoundManager() != null) {
                    plugin.getSoundManager().playSoundAll("countdown-tick", gamePlayers);
                }
            }
        }
    }

    // =========================================
    // ASYNC RTP LOGIC
    // =========================================

    private void onAllPlayersTeleported() {
         // Verify player count
        if (!testMode) {
            if (alivePlayers.size() < config.minPlayers) {
                plugin.getLogger().warning("Not enough players to start (" + alivePlayers.size() + "/" + config.minPlayers + ")");
                broadcastLobby(be.dualsfwshield.deathswap.util.Lang.get("game-not-enough-players", "%count%", String.valueOf(alivePlayers.size()), "%min%", String.valueOf(config.minPlayers)));
                state = GameState.WAITING;
                cleanup();
                return;
            }
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

        broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-started"));

        if (plugin.getSoundManager() != null) {
            plugin.getSoundManager().playSoundAll("game-start", gamePlayers);
        }

        if (config.gameType == GameType.DEATHSWAP) {
            if (config.pvpEnabled) {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-pvp-on", "%time%", String.valueOf(config.spawnProtection)));
            } else {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-pvp-off", "%time%", String.valueOf(config.spawnProtection)));
            }
            if (!config.netherEnabled || !config.endEnabled) {
                broadcastGame(be.dualsfwshield.deathswap.util.Lang.get("game-nether-end-disabled"));
            }
        }

        startGameLoop();
    }

    /**
     * Async random teleport logic.
     */
    private java.util.concurrent.CompletableFuture<Boolean> teleportRandomlyAsync(Player player, World world) {
        java.util.concurrent.CompletableFuture<Boolean> future = new java.util.concurrent.CompletableFuture<>();
        
        findSafeLocationAsync(world, 5000).thenAccept(loc -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (loc != null) {
                    player.teleport(loc);
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            });
        });
        
        return future;
    }
    
    private java.util.concurrent.CompletableFuture<Location> findSafeLocationAsync(World world, int radius) {
        java.util.concurrent.CompletableFuture<Location> future = new java.util.concurrent.CompletableFuture<>();
        findSafeLocationRecursive(world, radius, 10, future);
        return future;
    }

    private void findSafeLocationRecursive(World world, int radius, int attempts, java.util.concurrent.CompletableFuture<Location> future) {
        if (attempts <= 0) {
            plugin.getLogger().warning("Could not find safe RTP location after retries.");
            future.complete(world.getSpawnLocation()); // Fallback
            return;
        }

        int x = ThreadLocalRandom.current().nextInt(-radius, radius);
        int z = ThreadLocalRandom.current().nextInt(-radius, radius);
        
        // Optimistic async chunk load + check
        try {
            world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
                int y = world.getHighestBlockYAt(x, z);
                Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
                
                Material block = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType();
                if (isSafeBlock(block)) {
                    future.complete(loc);
                } else {
                    // Retry
                    findSafeLocationRecursive(world, radius, attempts - 1, future);
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                future.complete(world.getSpawnLocation());
                return null;
            });
        } catch (Exception e) {
             // Fallback for non-Paper
             future.complete(world.getSpawnLocation());
        }
    }

    private boolean isSafeBlock(Material mat) {
        if (mat == Material.LAVA || mat == Material.WATER || mat == Material.CACTUS || mat == Material.MAGMA_BLOCK) return false;
        if (mat.isAir()) return false;
        return mat.isSolid();
    }
}
