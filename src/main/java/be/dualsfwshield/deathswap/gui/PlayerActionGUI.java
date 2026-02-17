package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class PlayerActionGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private static final String TITLE_PREFIX = "Action: ";

    public PlayerActionGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, String arenaId, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(TITLE_PREFIX + target.getName()));

        // Slot 4: Target Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        head.setItemMeta(meta);
        inv.setItem(4, head);

        // Slot 10: Teleport
        inv.setItem(10, AdminGUI.createItem(Material.ENDER_PEARL, "&bTéléporter", "&7Aller à la position du joueur"));

        // Slot 12: InvSee
        inv.setItem(12, AdminGUI.createItem(Material.CHEST, "&eVoir Inventaire", "&7Ouvrir l'inventaire du joueur"));

        // Slot 14: Kick Arena
        inv.setItem(14, AdminGUI.createItem(Material.IRON_BOOTS, "&cKick de l'Arène", "&7Renvoyer au Hub"));

        // Slot 16: Ban (placeholder)
        inv.setItem(16, AdminGUI.createItem(Material.BARRIER, "&4Bannir (CMD)", "&7Exécuter /ban"));

        // Slot 22: Back
        // Store arenaId in invisible item or metadata? Or just assume admin context?
        // We'll put Arena ID in a hidden item (e.g. Barrier renamed) or pass it via
        // lore of Back button.
        // Actually, listener needs to know arenaId to go back.
        // Let's put it in the Back button name? No.
        // Let's store it in a hidden item at slot 0 (gray pane with hidden name).
        ItemStack hidden = AdminGUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "ID:" + arenaId);
        inv.setItem(0, hidden); // Top left

        inv.setItem(22, AdminGUI.createItem(Material.ARROW, "&eRetour"));

        // Fillers
        ItemStack filler = AdminGUI.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        admin.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin))
            return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        // Retrieve Arena ID from hidden slot 0
        ItemStack hidden = event.getInventory().getItem(0);
        if (hidden == null || !hidden.hasItemMeta()) {
            admin.closeInventory();
            return;
        }
        String hiddenName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(hidden.getItemMeta().displayName());
        String arenaId = hiddenName.replace("ID:", "").trim();

        // Target Name from Title
        String targetName = event.getView().getTitle().substring(TITLE_PREFIX.length());
        Player target = Bukkit.getPlayer(targetName);

        if (clicked.getType() == Material.ENDER_PEARL) {
            if (target != null) {
                admin.teleport(target);
                admin.sendMessage(Component.text("Téléporté à " + targetName, NamedTextColor.GREEN));
            } else {
                admin.sendMessage(Component.text("Joueur hors ligne.", NamedTextColor.RED));
            }
        } else if (clicked.getType() == Material.CHEST) {
            if (target != null) {
                admin.openInventory(target.getInventory());
            } else {
                admin.sendMessage(Component.text("Joueur hors ligne.", NamedTextColor.RED));
            }
        } else if (clicked.getType() == Material.IRON_BOOTS) {
            if (target != null) {
                be.dualsfwshield.deathswap.GameInstance arena = plugin.getArenaManager().getArena(arenaId);
                if (arena != null) {
                    arena.removePlayer(target);
                    plugin.getArenaManager().removePlayer(target); // Force remove from manager tracking
                    // TP to Hub
                    if (plugin.getConfigManager().getHubWorld() != null) {
                        org.bukkit.World hub = Bukkit.getWorld(plugin.getConfigManager().getHubWorld());
                        if (hub != null)
                            target.teleport(hub.getSpawnLocation());
                    }
                    target.sendMessage(
                            Component.text("Vous avez été exclu de l'arène par un admin.", NamedTextColor.RED));
                    admin.sendMessage(Component.text(targetName + " exclu de l'arène.", NamedTextColor.GREEN));
                }
            } else {
                // Try offline kick if stored in arena? complex.
                admin.sendMessage(Component.text("Joueur hors ligne.", NamedTextColor.RED));
            }
            plugin.getPlayerListGUI().open(admin, arenaId); // Refresh list
        } else if (clicked.getType() == Material.BARRIER) {
            // Ban: confirmation required
            plugin.getConfirmationGUI().open(admin,
                    "Bannir " + targetName,
                    "Le joueur sera kick + banni du serveur.",
                    NamedTextColor.DARK_RED,
                    () -> {
                        // On confirm
                        Player onlineTarget = Bukkit.getPlayerExact(targetName);
                        if (onlineTarget != null) {
                            GameInstance a = plugin.getArenaManager().getArena(arenaId);
                            if (a != null) {
                                a.sendToHub(onlineTarget);
                            }
                        }
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                "ban " + targetName + " Banni via Admin Dashboard par " + admin.getName());
                        admin.sendMessage(Component.text(targetName + " banni du serveur.", NamedTextColor.DARK_RED));
                    },
                    () -> {
                        // On cancel: re-open action GUI
                        Player t = Bukkit.getPlayerExact(targetName);
                        if (t != null) {
                            plugin.getPlayerActionGUI().open(admin, arenaId, t);
                        } else {
                            plugin.getPlayerListGUI().open(admin, arenaId);
                        }
                    });
            return; // Don't refresh list yet — confirmation GUI is open
        } else if (clicked.getType() == Material.ARROW) {
            plugin.getPlayerListGUI().open(admin, arenaId);
        }
    }
}
