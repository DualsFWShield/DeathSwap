package be.dualsfwshield.deathswap.listeners;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Protects the lobby world: prevents block placement/breaking, damage, hunger,
 * drops.
 */
public class LobbyListener implements Listener {

    private final DeathSwapPlugin plugin;

    public LobbyListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (shouldBypass(event.getPlayer()))
            return;
        if (isInLobby(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (shouldBypass(event.getPlayer()))
            return;
        if (isInLobby(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isInLobby(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isInLobby(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (shouldBypass(event.getPlayer()))
            return;
        if (isInLobby(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (shouldBypass(player))
                return;
            if (isInLobby(player)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean shouldBypass(Player player) {
        return player.isOp() || player.getGameMode() == org.bukkit.GameMode.CREATIVE;
    }

    /**
     * Check if a player is in any arena's lobby world.
     */
    private boolean isInLobby(Player player) {
        String worldName = player.getWorld().getName();
        GameInstance arena = plugin.getArenaManager().findByLobbyWorld(worldName);
        return arena != null;
    }
}
