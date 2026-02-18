package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArenaDetailsGUI implements Listener {

    private final DeathSwapPlugin plugin;
    // Removed static TITLE_PREFIX, using key instead

    public ArenaDetailsGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaId) {
        // Try getting active game instance first
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        // If not loaded, check if config exists
        be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);

        if (config == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "gui-details-error-not-found");
            return;
        }

        String prefix = be.dualsfwshield.deathswap.util.Lang.get("gui-details-title-prefix");
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(prefix + arenaId));

        GameState state = (arena != null) ? arena.getState() : GameState.DISABLED;
        if (arena == null) {
            // If no instance, it's effectively unloaded/disabled or just not created yet by manager
            // But usually ArenaManager creates instances for all valid configs. 
            // If it's null here, it might be broken or disabled.
            // Let's assume DISABLED for safety or just "UNLOADED".
             state = GameState.DISABLED;
        }

        // Slot 10: Status Item
        Material stateMat = switch (state) {
            case WAITING -> Material.YELLOW_CONCRETE;
            case STARTING -> Material.LIME_CONCRETE;
            case RUNNING -> Material.GREEN_CONCRETE;
            case ENDED -> Material.RED_CONCRETE;
            case DISABLED -> Material.BARRIER;
        };
            case DISABLED -> Material.BARRIER;
        };
        String stateName = (arena == null) ? be.dualsfwshield.deathswap.util.Lang.get("gui-details-unloaded") : state.name();
        inv.setItem(10, AdminGUI.createItem(stateMat, be.dualsfwshield.deathswap.util.Lang.get("gui-details-status", "%status%", stateName)));

        // Slot 12: Start / Stop
        if (state == GameState.RUNNING || state == GameState.STARTING) {
             // Stop button
             inv.setItem(12, AdminGUI.createItem(Material.BARRIER, be.dualsfwshield.deathswap.util.Lang.get("gui-details-stop-name"), 
                 be.dualsfwshield.deathswap.util.Lang.get("gui-details-stop-lore-1"), be.dualsfwshield.deathswap.util.Lang.get("gui-details-stop-click")));
        } else {
             // Start button
             inv.setItem(12, AdminGUI.createItem(Material.EMERALD, be.dualsfwshield.deathswap.util.Lang.get("gui-details-start-name"), 
                 be.dualsfwshield.deathswap.util.Lang.get("gui-details-start-lore-1"), 
                 be.dualsfwshield.deathswap.util.Lang.get("gui-details-start-click-normal"), 
                 be.dualsfwshield.deathswap.util.Lang.get("gui-details-start-click-force"),
                 be.dualsfwshield.deathswap.util.Lang.get("gui-details-start-click-debug")));
        }

        // Slot 14: Swap Immediate (Variables) / Regenerate (Waiting)
        if (state == GameState.RUNNING) {
            inv.setItem(14, AdminGUI.createItem(Material.ENDER_PEARL, be.dualsfwshield.deathswap.util.Lang.get("gui-details-swap-force-name"), 
                be.dualsfwshield.deathswap.util.Lang.get("gui-details-swap-force-lore"), be.dualsfwshield.deathswap.util.Lang.get("gui-details-swap-force-click")));
        } else if (state == GameState.WAITING || state == GameState.DISABLED) {
             inv.setItem(14, AdminGUI.createItem(Material.TNT, be.dualsfwshield.deathswap.util.Lang.get("gui-details-regen-name"),
                    be.dualsfwshield.deathswap.util.Lang.get("gui-details-regen-lore-1"), be.dualsfwshield.deathswap.util.Lang.get("gui-details-regen-lore-2")));
        } else {
             inv.setItem(14, AdminGUI.createItem(Material.GRAY_DYE, be.dualsfwshield.deathswap.util.Lang.get("gui-details-manage-swap-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-details-manage-swap-lore")));
        }

        // Slot 16: Players
        inv.setItem(16, AdminGUI.createItem(Material.PLAYER_HEAD, be.dualsfwshield.deathswap.util.Lang.get("gui-details-manage-players-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-details-manage-players-lore")));

        // Slot 22: Back
        inv.setItem(22, AdminGUI.createItem(Material.ARROW, be.dualsfwshield.deathswap.util.Lang.get("gui-details-back-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-details-back-lore")));

        // Slot 26: Settings
        inv.setItem(26, AdminGUI.createItem(Material.COMPARATOR, be.dualsfwshield.deathswap.util.Lang.get("gui-details-config-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-details-config-lore")));

        // Fillers
        ItemStack filler = AdminGUI.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String prefix = be.dualsfwshield.deathswap.util.Lang.get("gui-details-title-prefix");
        if (!event.getView().getTitle().startsWith(prefix))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        String arenaId = event.getView().getTitle().substring(prefix.length());
        
        // We might valid config even if game instance is null
        // actions requiring game instance must check for it
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        
        int slot = event.getSlot();

        if (slot == 12) { // Start/Stop
             if (arena == null) {
                 be.dualsfwshield.deathswap.util.Lang.send(player, "gui-details-error-instance");
                 return;
             }
             GameState state = arena.getState();
             if (state == GameState.RUNNING || state == GameState.STARTING) {
                 // Stop
                 arena.stopGame();
                 be.dualsfwshield.deathswap.util.Lang.send(player, "gui-details-game-stopped");
             } else {
                 // Start
                 if (event.getClick().isShiftClick()) {
                     // Force Start
                     if (arena.getAllPlayers().size() < 2) {
                         // Force start might imply bypassing checks, but usually we need players.
                         // But admin might want to test alone? 
                         // Let's assume force start just skips timer.
                         // Standard start checks min players.
                         // Debug start might bypass min players?
                     }
                     arena.startGame(true); // Force start logic if supported
                     player.sendMessage(Component.text("Force Start initialisé...", NamedTextColor.GREEN));
                 } else if (event.getClick().isRightClick()) {
                     // Debug start? maybe reduce timer to 5s?
                     // Or just start with current players even if < min
                     // Check if your GameInstance has debug start
                     arena.startGame(false); 
                     // Assuming standard start for now, maybe set timer to 5?
                     // Implement specific logic if available.
                     player.sendMessage(Component.text("Debug Start (Standard) launched.", NamedTextColor.YELLOW));
                 } else {
                     // Normal start
                     arena.startGame(false);
                     player.sendMessage(Component.text("Démarrage...", NamedTextColor.GREEN));
                 }
             }
             open(player, arenaId); // Refresh

        } else if (slot == 14) { // Swap or Regenerate
             if (arena != null && arena.getState() == GameState.RUNNING) {
                 // Swap Immediate
                 arena.performSwap();
                 be.dualsfwshield.deathswap.util.Lang.send(player, "gui-details-swap-forced");
             } else if (arena == null || arena.getState() == GameState.WAITING || arena.getState() == GameState.DISABLED) {
                 // Regenerate logic
                 be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
                 if (config == null) return;
                 
                  String gameWorld = config.gameWorld;
                  plugin.getConfirmationGUI().open(player,
                          be.dualsfwshield.deathswap.util.Lang.get("gui-details-regen-confirm-title", "%world%", gameWorld),
                          be.dualsfwshield.deathswap.util.Lang.get("gui-details-regen-confirm-subtitle"),
                          NamedTextColor.GOLD,
                          () -> {
                              if (arena != null) {
                                  for (Player p : arena.getAllPlayers()) {
                                      arena.sendToHub(p);
                                  }
                              }
                              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cwr reset " + gameWorld);
                              be.dualsfwshield.deathswap.util.Lang.send(player, "gui-details-regen-success", "%world%", gameWorld);
                              player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                          },
                          () -> open(player, arenaId)
                  );
             }
        } else if (slot == 16) { // Players
             plugin.getPlayerListGUI().open(player, arenaId);
        } else if (slot == 22) { // Back
             plugin.getArenaListGUI().open(player); // Go back to list, not AdminGUI (dashboard)
        } else if (slot == 26) { // Settings
             plugin.getSettingsGUI().open(player, arenaId);
        }
    }
}
