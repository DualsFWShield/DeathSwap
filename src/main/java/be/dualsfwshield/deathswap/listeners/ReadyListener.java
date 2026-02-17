package be.dualsfwshield.deathswap.listeners;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles ready/unready item interactions in the lobby and player quit.
 */
public class ReadyListener implements Listener {

    private final DeathSwapPlugin plugin;

    public ReadyListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Only handle right clicks
        if (!event.getAction().isRightClick())
            return;

        // Check if player is in a lobby world
        GameInstance arena = plugin.getArenaManager().findByLobbyWorld(player.getWorld().getName());
        if (arena == null)
            return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().displayName() == null)
            return;

        event.setCancelled(true);
        Material type = item.getType();

        // Ready toggle (slot 4: red concrete / lime concrete)
        if (type == Material.RED_CONCRETE || type == Material.LIME_CONCRETE) {
            arena.toggleReady(player);
            return;
        }

        // Return to hub (slot 8: red bed)
        if (type == Material.RED_BED) {
            arena.sendToHub(player);
        }
    }

    /**
     * When a player enters a lobby world directly (e.g. via /mv tp), register them.
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String newWorld = player.getWorld().getName();

        // Check if the new world is a lobby world
        GameInstance arena = plugin.getArenaManager().findByLobbyWorld(newWorld);
        if (arena != null) {
            // If player isn't registered in this arena, register them
            if (plugin.getArenaManager().getPlayerArena(player) == null) {
                arena.getLobbyPlayers().add(player);
                plugin.getArenaManager().addPlayerToArena(player, arena.getArenaId());
            }
            arena.setupLobbyPlayer(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameInstance arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena != null) {
            arena.removePlayer(player);
        }
    }
}
