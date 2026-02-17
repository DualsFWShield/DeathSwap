package be.dualsfwshield.deathswap.modes;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Listener for DeathShuffle mode.
 * Intercepts death events to check if the death cause matches the current
 * challenge.
 * Handles respawn logic to keep players alive for the next round.
 */
public class DeathShuffleListener implements Listener {

    private final DeathSwapPlugin plugin;

    public DeathShuffleListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || game.getState() != GameState.RUNNING)
            return;
        if (game.getConfig().gameType != GameType.DEATHSHUFFLE)
            return;
        if (!(game instanceof DeathShuffleInstance dsi))
            return;

        // Get the last damage cause
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        EntityDamageEvent.DamageCause cause = lastDamage != null ? lastDamage.getCause()
                : EntityDamageEvent.DamageCause.CUSTOM;

        // Let the DeathShuffleInstance handle it
        boolean shouldRespawn = dsi.onPlayerDeath(player, cause);

        if (shouldRespawn) {
            // Cancel death message, force respawn
            event.deathMessage(null);
            // Keep inventory for shuffle modes
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || game.getState() != GameState.RUNNING)
            return;
        if (game.getConfig().gameType != GameType.DEATHSHUFFLE)
            return;
        if (!(game instanceof DeathShuffleInstance dsi))
            return;

        if (dsi.isPendingRespawn(player.getUniqueId())) {
            dsi.consumePendingRespawn(player.getUniqueId());

            // Respawn in the game world at their last location
            if (player.getBedSpawnLocation() != null) {
                event.setRespawnLocation(player.getBedSpawnLocation());
            } else {
                org.bukkit.World gameWorld = Bukkit.getWorld(game.getConfig().gameWorld);
                if (gameWorld != null) {
                    event.setRespawnLocation(gameWorld.getSpawnLocation());
                }
            }

            // Restore health after respawn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && game.getAlivePlayers().contains(player)) {
                    player.setHealth(20);
                    player.setFoodLevel(20);
                    player.setSaturation(20);
                }
            }, 2L);
        }
    }
}
