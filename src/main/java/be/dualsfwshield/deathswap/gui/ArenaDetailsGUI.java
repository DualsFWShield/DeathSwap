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
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) {
            player.sendMessage(Component.text("Cette arène n'existe plus.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, Component.text(TITLE_PREFIX + arenaId));

        // Slot 10: Status Item
        GameState state = arena.getState();
        Material stateMat = switch (state) {
            case WAITING -> Material.YELLOW_CONCRETE;
            case STARTING -> Material.LIME_CONCRETE;
            case RUNNING -> Material.GREEN_CONCRETE;
            case ENDED -> Material.RED_CONCRETE;
            case DISABLED -> Material.BARRIER;
        };
        inv.setItem(10, AdminGUI.createItem(stateMat, "&eStatut: " + state.name()));

        // Slot 12: Force Start/Stop
        if (state == GameState.WAITING || state == GameState.STARTING) {
            inv.setItem(12,
                    AdminGUI.createItem(Material.DIAMOND_SWORD, "&aForce Start", "&7Lancer le jeu immédiatement"));
        } else if (state == GameState.RUNNING) {
            inv.setItem(12, AdminGUI.createItem(Material.BARRIER, "&cForce Stop", "&7Arrêter le jeu immédiatement"));
        } else {
            inv.setItem(12, AdminGUI.createItem(Material.GRAY_DYE, "&7Action Indisponible"));
        }

        // Slot 14: Regenerate World
        if (state == GameState.WAITING || state == GameState.DISABLED) {
            inv.setItem(14, AdminGUI.createItem(Material.TNT, "&cRégénérer Monde",
                    "&7Supprimer et recréer le monde de jeu", "&7IRRÉVERSIBLE!"));
        } else {
            inv.setItem(14, AdminGUI.createItem(Material.GRAY_DYE, "&7Indisponible (Jeu en cours)"));
        }

        // Slot 16: Players
        inv.setItem(16, AdminGUI.createItem(Material.PLAYER_HEAD, "&bGérer Joueurs", "&7Voir/Kick/Ban joueurs"));

        // Slot 22: Back
        inv.setItem(22, AdminGUI.createItem(Material.ARROW, "&eRetour"));

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
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);

        if (arena == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();

        if (slot == 12) { // Start/Stop
            if (clicked.getType() == Material.DIAMOND_SWORD) {
                arena.startGame(false);
                player.sendMessage(Component.text("Tentative de lancement...", NamedTextColor.GREEN));
            } else if (clicked.getType() == Material.BARRIER) {
                arena.stopGame();
                player.sendMessage(Component.text("Jeu arrêté.", NamedTextColor.RED));
            }
            open(player, arenaId); // Refresh
        } else if (slot == 14) { // Regenerate
            if (clicked.getType() == Material.TNT) {
                if (arena.getState() == GameState.RUNNING || arena.getState() == GameState.STARTING) {
                    player.sendMessage(
                            Component.text("Impossible : le jeu est en cours. Arrêtez-le d'abord.",
                                    NamedTextColor.RED));
                } else {
                    String gameWorld = arena.getConfig().gameWorld;
                    plugin.getConfirmationGUI().open(player,
                            "Régénérer " + gameWorld,
                            "Tous les joueurs seront téléportés au hub.",
                            NamedTextColor.GOLD,
                            () -> {
                                // On confirm: teleport + regen
                                for (Player p : arena.getAllPlayers()) {
                                    arena.sendToHub(p);
                                }
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cwr reset " + gameWorld);
                                player.sendMessage(
                                        Component.text("Monde '" + gameWorld + "' en cours de régénération...",
                                                NamedTextColor.GREEN));
                                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                            },
                            () -> open(player, arenaId) // On cancel: re-open arena details
                    );
                }
            }
        } else if (slot == 16) { // Players
            // specific GUI needed.
            plugin.getPlayerListGUI().open(player, arenaId);
        } else if (slot == 22) { // Back
            plugin.getAdminGUI().open(player);
        }
    }
}
