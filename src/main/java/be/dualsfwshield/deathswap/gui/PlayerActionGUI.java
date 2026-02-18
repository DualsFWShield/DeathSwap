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
    // Removed static TITLE_PREFIX, using key instead

    public PlayerActionGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, String arenaId, Player target) {
        String prefix = be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-title");
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(prefix + target.getName()));

        // Slot 4: Target Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        head.setItemMeta(meta);
        inv.setItem(4, head);

        // Slot 10: Teleport
        inv.setItem(10, AdminGUI.createItem(Material.ENDER_PEARL, be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-tp-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-tp-lore")));

        // Slot 12: InvSee
        inv.setItem(12, AdminGUI.createItem(Material.CHEST, be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-invsee-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-invsee-lore")));

        // Slot 14: Kick Arena
        inv.setItem(14, AdminGUI.createItem(Material.IRON_BOOTS, be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-kick-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-kick-lore")));

        // Slot 16: Ban (placeholder)
        inv.setItem(16, AdminGUI.createItem(Material.BARRIER, be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-ban-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-ban-lore")));

        // Slot 22: Back
        // Store arenaId in invisible item or metadata? Or just assume admin context?
        // We'll put Arena ID in a hidden item (e.g. Barrier renamed) or pass it via
        // lore of Back button.
        // Actually, listener needs to know arenaId to go back.
        // Let's put it in the Back button name? No.
        // Let's store it in a hidden item at slot 0 (gray pane with hidden name).
        ItemStack hidden = AdminGUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "ID:" + arenaId);
        inv.setItem(0, hidden); // Top left

        inv.setItem(22, AdminGUI.createItem(Material.ARROW, be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-back")));

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
        String prefix = be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-title");
        if (!event.getView().getTitle().startsWith(prefix))
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
        // Target Name from Title
        String targetName = event.getView().getTitle().substring(prefix.length());
        Player target = Bukkit.getPlayer(targetName);

        if (clicked.getType() == Material.ENDER_PEARL) {
            if (target != null) {
                admin.teleport(target);
                be.dualsfwshield.deathswap.util.Lang.send(admin, "gui-player-action-teleported", "%player%", targetName);
            } else {
                be.dualsfwshield.deathswap.util.Lang.send(admin, "gui-player-action-offline");
            }
        } else if (clicked.getType() == Material.CHEST) {
            if (target != null) {
                admin.openInventory(target.getInventory());
            } else {
                be.dualsfwshield.deathswap.util.Lang.send(admin, "gui-player-action-offline");
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
                    be.dualsfwshield.deathswap.util.Lang.send(target, "gui-player-action-kick-message");
                    be.dualsfwshield.deathswap.util.Lang.send(admin, "gui-player-action-kick-success", "%player%", targetName);
                }
            } else {
                // Try offline kick if stored in arena? complex.
                be.dualsfwshield.deathswap.util.Lang.send(admin, "gui-player-action-offline");
            }
            plugin.getPlayerListGUI().open(admin, arenaId); // Refresh list
        } else if (clicked.getType() == Material.BARRIER) {
            // Ban: confirmation required
            plugin.getConfirmationGUI().open(admin,
                    be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-ban-confirm-title", "%player%", targetName),
                    be.dualsfwshield.deathswap.util.Lang.get("gui-player-action-ban-confirm-subtitle"),
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
                        be.dualsfwshield.deathswap.util.Lang.send(admin, "gui-player-action-ban-success", "%player%", targetName);
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
