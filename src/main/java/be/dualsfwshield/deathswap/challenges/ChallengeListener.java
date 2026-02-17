package be.dualsfwshield.deathswap.challenges;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;

/**
 * Listens to game events and reports progress to the ChallengeManager.
 */
public class ChallengeListener implements Listener {

    private final DeathSwapPlugin plugin;

    public ChallengeListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().isChallengesEnabled())
            return;
        Player player = event.getPlayer();
        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || game.getState() != GameState.RUNNING)
            return;
        if (game.getConfig().gameType != GameType.DEATHSWAP)
            return;

        String blockName = event.getBlock().getType().name();
        plugin.getChallengeManager().reportProgress(
                player.getUniqueId(), Challenge.ChallengeType.MINE, blockName);
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!plugin.getConfigManager().isChallengesEnabled())
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || game.getState() != GameState.RUNNING)
            return;
        if (game.getConfig().gameType != GameType.DEATHSWAP)
            return;

        if (event.getRecipe().getResult().getType() == null)
            return;
        String itemName = event.getRecipe().getResult().getType().name();
        plugin.getChallengeManager().reportProgress(
                player.getUniqueId(), Challenge.ChallengeType.CRAFT, itemName);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfigManager().isChallengesEnabled())
            return;
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null)
            return;

        GameInstance game = plugin.getArenaManager().getPlayerArena(killer);
        if (game == null || game.getState() != GameState.RUNNING)
            return;
        if (game.getConfig().gameType != GameType.DEATHSWAP)
            return;

        String entityName = entity.getType().name();
        plugin.getChallengeManager().reportProgress(
                killer.getUniqueId(), Challenge.ChallengeType.KILL, entityName);
    }
}
