package be.dualsfwshield.deathswap.listeners;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import be.dualsfwshield.deathswap.util.Lang;

/**
 * Handles in-game events: death, portal redirection, PvP control, fall
 * protection,
 * spectator restrictions.
 */
public class GameListener implements Listener {

    private final DeathSwapPlugin plugin;

    public GameListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle player death in a game world (including dedicated nether/end).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameInstance arena = plugin.getArenaManager().findByAnyGameWorld(player.getWorld().getName());
        if (arena == null || arena.getState() != GameState.RUNNING)
            return;

        // Suppress default death message (we handle our own)
        event.deathMessage(null);

        // Double check grace period (should not happen with listeners, but safety
        // first)
        if (arena.isGracePeriod()) {
            event.setCancelled(true);
            return;
        }

        // Shuffle modes have their own death listeners — skip generic handleDeath
        if (arena.getConfig().gameType == GameType.DEATHSHUFFLE
                || arena.getConfig().gameType == GameType.BLOCKSHUFFLE) {
            return;
        }

        arena.handleDeath(player);
    }

    /**
     * Handle portal events: redirect to dedicated dimension worlds or block if
     * disabled.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        GameInstance arena = plugin.getArenaManager().findByAnyGameWorld(player.getWorld().getName());
        if (arena == null || arena.getState() != GameState.RUNNING)
            return;

        String currentWorld = player.getWorld().getName();
        String gameWorld = arena.getConfig().gameWorld;
        TeleportCause cause = event.getCause();

        if (cause == TeleportCause.NETHER_PORTAL) {
            if (!arena.getConfig().netherEnabled) {
                event.setCancelled(true);
                Lang.send(player, "portal-nether-disabled");
                return;
            }

            // Determine target world: overworld ↔ nether
            String targetWorldName;
            if (currentWorld.equals(gameWorld + "_nether")) {
                targetWorldName = gameWorld; // Nether → Overworld
            } else {
                targetWorldName = gameWorld + "_nether"; // Overworld → Nether
            }

            World target = Bukkit.getWorld(targetWorldName);
            if (target != null && event.getTo() != null) {
                Location to = event.getTo();
                event.setTo(new Location(target, to.getX(), to.getY(), to.getZ(),
                        to.getYaw(), to.getPitch()));
            } else {
                event.setCancelled(true);
                Lang.send(player, "portal-nether-not-loaded");
            }

        } else if (cause == TeleportCause.END_PORTAL) {
            if (!arena.getConfig().endEnabled) {
                event.setCancelled(true);
                Lang.send(player, "portal-end-disabled");
                return;
            }

            // Determine target world: overworld ↔ end
            String targetWorldName;
            if (currentWorld.equals(gameWorld + "_the_end")) {
                targetWorldName = gameWorld; // End → Overworld (spawn)
            } else {
                targetWorldName = gameWorld + "_the_end"; // Overworld → End
            }

            World target = Bukkit.getWorld(targetWorldName);
            if (target != null) {
                if (currentWorld.equals(gameWorld + "_the_end")) {
                    // Returning from End → spawn at overworld spawn
                    event.setTo(target.getSpawnLocation());
                } else if (event.getTo() != null) {
                    Location to = event.getTo();
                    event.setTo(new Location(target, to.getX(), to.getY(), to.getZ(),
                            to.getYaw(), to.getPitch()));
                }
            } else {
                event.setCancelled(true);
                Lang.send(player, "portal-end-not-loaded");
            }
        }
    }

    /**
     * PvP control: if pvp-enabled is false, cancel player-vs-player damage but
     * allow player-vs-mob.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only care about player attackers
        if (!(event.getDamager() instanceof Player attacker))
            return;
        if (!(event.getEntity() instanceof Player victim))
            return;

        GameInstance arena = plugin.getArenaManager().findByAnyGameWorld(attacker.getWorld().getName());
        if (arena == null || arena.getState() != GameState.RUNNING)
            return;

        // If PvP is disabled, cancel player-vs-player only
        if (!arena.getConfig().pvpEnabled) {
            event.setCancelled(true);
            return;
        }

        // Grace period check (no PvP during spawn protection)
        if (arena.isGracePeriod()) {
            event.setCancelled(true);
        }
    }

    /**
     * Fall damage protection at the start of the game (first N seconds).
     */
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;

        GameInstance arena = plugin.getArenaManager().findByAnyGameWorld(player.getWorld().getName());
        if (arena == null || arena.getState() != GameState.RUNNING)
            return;

        // Resistance potion handles this, but as a backup safety net
        // (player has resistance 255 for spawn-protection seconds)
        if (arena.isGracePeriod()) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent spectators from dropping items in game world.
     */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameInstance arena = plugin.getArenaManager().findByAnyGameWorld(player.getWorld().getName());
        if (arena == null)
            return;

        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent spectators from losing hunger in game world.
     */
    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        GameInstance arena = plugin.getArenaManager().findByAnyGameWorld(player.getWorld().getName());
        if (arena == null)
            return;

        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle when a player leaves the game/dimension worlds via commands like
     * /lobby or /hub. Allows travel between game world and its dedicated
     * dimensions.
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        GameInstance arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null)
            return;

        // If they moved to a world that is NEITHER the game world, NOR the lobby world,
        // NOR a dedicated dimension world, they left.
        String newWorld = player.getWorld().getName();
        String gameWorld = arena.getConfig().gameWorld;
        if (!newWorld.equals(gameWorld)
                && !newWorld.equals(arena.getConfig().lobbyWorld)
                && !newWorld.equals(gameWorld + "_nether")
                && !newWorld.equals(gameWorld + "_the_end")) {
            // Player left the arena context entirely
            arena.handleDisconnectForfeit(player);
        }
    }
}
