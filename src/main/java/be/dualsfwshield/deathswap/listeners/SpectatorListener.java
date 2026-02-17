package be.dualsfwshield.deathswap.listeners;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles spectator compass interactions in the game world.
 */
public class SpectatorListener implements Listener {

    private final DeathSwapPlugin plugin;

    public SpectatorListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!event.getAction().isRightClick())
            return;
        if (player.getGameMode() != GameMode.SPECTATOR)
            return;

        GameInstance arena = plugin.getArenaManager().findByGameWorld(player.getWorld().getName());
        if (arena == null || arena.getState() != GameState.RUNNING)
            return;

        if (event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
            event.setCancelled(true);
            arena.sendSpectatorMenu(player);
        }
    }
}
