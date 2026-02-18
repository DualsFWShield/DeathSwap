package be.dualsfwshield.deathswap.listeners;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * Handles in-game events: death, portal blocking, PvP control, fall protection,
 * spectator restrictions.
 */
public class GameListener implements Listener {

    private final DeathSwapPlugin plugin;

    public GameListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle player death in a game world.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameInstance arena = plugin.getArenaManager().findByGameWorld(player.getWorld().getName());
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

        arena.handleDeath(player);
    }

    /**
     * Block Nether/End portals in game worlds.
     */
    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        GameInstance arena = plugin.getArenaManager().findByGameWorld(player.getWorld().getName());
        if (arena == null)
            return;

        if (!arena.getConfig().netherEnabled || !arena.getConfig().endEnabled) {
            event.setCancelled(true);
            player.sendMessage(Component.text("[DeathSwap] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("Le Nether et l'End sont désactivés pour cette partie !",
                            NamedTextColor.RED)));
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

        GameInstance arena = plugin.getArenaManager().findByGameWorld(attacker.getWorld().getName());
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

        GameInstance arena = plugin.getArenaManager().findByGameWorld(player.getWorld().getName());
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
        GameInstance arena = plugin.getArenaManager().findByGameWorld(player.getWorld().getName());
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

        GameInstance arena = plugin.getArenaManager().findByGameWorld(player.getWorld().getName());
        if (arena == null)
            return;

        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }
}
