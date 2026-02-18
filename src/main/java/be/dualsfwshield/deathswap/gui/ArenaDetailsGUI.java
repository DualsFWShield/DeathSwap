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
    private static final String TITLE_PREFIX = "Admin: ";

    public ArenaDetailsGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaId) {
        // Try getting active game instance first
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        // If not loaded, check if config exists
        be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);

        if (config == null) {
            player.sendMessage(Component.text("Cette arène n'existe plus.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, Component.text(TITLE_PREFIX + arenaId));

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
        String stateName = (arena == null) ? "UNLOADED" : state.name();
        inv.setItem(10, AdminGUI.createItem(stateMat, "&eStatut: " + stateName));

        // Slot 12: Start / Stop
        if (state == GameState.RUNNING || state == GameState.STARTING) {
             // Stop button
             inv.setItem(12, AdminGUI.createItem(Material.BARRIER, "&cArrêter la partie/compte à rebours", 
                 "&7Force l'arrêt immédiat.", "&aClic G: &cSTOP"));
        } else {
             // Start button
             inv.setItem(12, AdminGUI.createItem(Material.EMERALD, "&aDémarrer l'arène", 
                 "&7Lancer la partie.", 
                 "&aClic G: &7Démarrer (Normal)", 
                 "&eShift-Clic G: &7Forcer le démarrage",
                 "&cclic D: &7Debug Start"));
        }

        // Slot 14: Swap Immediate (Variables) / Regenerate (Waiting)
        if (state == GameState.RUNNING) {
            inv.setItem(14, AdminGUI.createItem(Material.ENDER_PEARL, "&bForce Swap", 
                "&7Déclencher un swap immédiat.", "&aClic G: &7Swap !"));
        } else if (state == GameState.WAITING || state == GameState.DISABLED) {
             inv.setItem(14, AdminGUI.createItem(Material.TNT, "&cRégénérer Monde",
                    "&7Supprimer et recréer le monde de jeu", "&7IRRÉVERSIBLE!"));
        } else {
             inv.setItem(14, AdminGUI.createItem(Material.GRAY_DYE, "&7Gérer Swap", "&7Seulement en jeu."));
        }

        // Slot 16: Players
        inv.setItem(16, AdminGUI.createItem(Material.PLAYER_HEAD, "&bGérer Joueurs", "&7Voir/Kick/Ban joueurs"));

        // Slot 22: Back
        inv.setItem(22, AdminGUI.createItem(Material.ARROW, "&eRetour", "&7Vers la liste des arènes"));

        // Slot 26: Settings
        inv.setItem(26, AdminGUI.createItem(Material.COMPARATOR, "&6Configuration", "&7Modifier les paramètres"));

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
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        String arenaId = event.getView().getTitle().substring(TITLE_PREFIX.length());
        
        // We might valid config even if game instance is null
        // actions requiring game instance must check for it
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        
        int slot = event.getSlot();

        if (slot == 12) { // Start/Stop
             if (arena == null) {
                 player.sendMessage(Component.text("Instance d'arène introuvable (non chargée ?).", NamedTextColor.RED));
                 return;
             }
             GameState state = arena.getState();
             if (state == GameState.RUNNING || state == GameState.STARTING) {
                 // Stop
                 arena.stopGame();
                 player.sendMessage(Component.text("Jeu arrêté.", NamedTextColor.RED));
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
                 player.sendMessage(Component.text("Swap forcé !", NamedTextColor.AQUA));
             } else if (arena == null || arena.getState() == GameState.WAITING || arena.getState() == GameState.DISABLED) {
                 // Regenerate logic
                 be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
                 if (config == null) return;
                 
                  String gameWorld = config.gameWorld;
                  plugin.getConfirmationGUI().open(player,
                          "Régénérer " + gameWorld,
                          "Tous les joueurs seront téléportés au hub.",
                          NamedTextColor.GOLD,
                          () -> {
                              if (arena != null) {
                                  for (Player p : arena.getAllPlayers()) {
                                      arena.sendToHub(p);
                                  }
                              }
                              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cwr reset " + gameWorld);
                              player.sendMessage(
                                      Component.text("Monde '" + gameWorld + "' en cours de régénération...",
                                              NamedTextColor.GREEN));
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
