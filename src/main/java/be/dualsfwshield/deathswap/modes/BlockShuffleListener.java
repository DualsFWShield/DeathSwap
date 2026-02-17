package be.dualsfwshield.deathswap.modes;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener for BlockShuffle mode.
 * Monitors player movement (stand on block) and crafting events.
 */
public class BlockShuffleListener implements Listener {

    private final DeathSwapPlugin plugin;

    public BlockShuffleListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check when the block actually changed (not just looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || game.getState() != GameState.RUNNING)
            return;
        if (game.getConfig().gameType != GameType.BLOCKSHUFFLE)
            return;
        if (!(game instanceof BlockShuffleInstance bsi))
            return;

        // Check the block below the player's feet
        Block below = player.getLocation().subtract(0, 1, 0).getBlock();
        Material blockType = below.getType();

        if (blockType != Material.AIR && blockType != Material.CAVE_AIR && blockType != Material.VOID_AIR) {
            bsi.onPlayerStandOnBlock(player, blockType);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || game.getState() != GameState.RUNNING)
            return;
        if (game.getConfig().gameType != GameType.BLOCKSHUFFLE)
            return;
        if (!(game instanceof BlockShuffleInstance bsi))
            return;

        Material craftedType = event.getRecipe().getResult().getType();
        bsi.onPlayerCraftItem(player, craftedType);
    }
}
