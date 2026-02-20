package be.dualsfwshield.deathswap.listeners;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.util.Lang;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles ready/unready item interactions in the lobby and player quit.
 */
public class ReadyListener implements Listener {

    private final DeathSwapPlugin plugin;

    public ReadyListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    private final Set<UUID> interactionCooldowns = new HashSet<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Only handle right clicks and main hand
        if (!event.getAction().isRightClick())
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        // Check cooldown to prevent double firing
        if (interactionCooldowns.contains(player.getUniqueId())) {
            return;
        }

        // Check if player is in a lobby world
        GameInstance arena = plugin.getArenaManager().findByLobbyWorld(player.getWorld().getName());
        if (arena == null)
            return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().displayName() == null)
            return;

        // Add cooldown
        interactionCooldowns.add(player.getUniqueId());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            interactionCooldowns.remove(player.getUniqueId());
        }, 5L); // 250ms cooldown

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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameInstance arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena != null) {
            if (arena.getState() == GameState.RUNNING && arena.getAlivePlayers().contains(player)) {
                arena.broadcastGame(Lang.get("game-quit-forfeit", "%player%", player.getName()));
            }
            arena.removePlayer(player);
        }
        interactionCooldowns.remove(player.getUniqueId());
    }
}
