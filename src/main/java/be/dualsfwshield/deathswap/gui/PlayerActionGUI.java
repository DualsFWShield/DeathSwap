package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    /*
     * ── Inventory layout (27 slots, 3 rows) ─────────────────────────
     *
     * Row 1 │ (hidden) · · · HEAD · · · ·
     * Row 2 │ · TP · INVSEE · KICK · BAN ·
     * Row 3 │ · · · · BACK · · · ·
     */
    private static final int INV_SIZE = 27;
    private static final int SLOT_HEAD = 4;
    private static final int SLOT_TP = 10;
    private static final int SLOT_INVSEE = 12;
    private static final int SLOT_KICK = 14;
    private static final int SLOT_BAN = 16;
    private static final int SLOT_HIDDEN = 0; // stores target player name
    private static final int SLOT_BACK = 22;

    private final DeathSwapPlugin plugin;

    public PlayerActionGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, String arenaId, Player target) {
        String prefix = Lang.get("gui-player-action-title");
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, Component.text(prefix + target.getName()));

        // Slot 4: Target Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        head.setItemMeta(meta);
        inv.setItem(SLOT_HEAD, head);

        // Slot 10: Teleport
        inv.setItem(SLOT_TP, GuiUtils.createItem(Material.ENDER_PEARL, Lang.get("gui-player-action-tp-name"),
                Lang.get("gui-player-action-tp-lore")));

        // Slot 12: InvSee
        inv.setItem(SLOT_INVSEE, GuiUtils.createItem(Material.CHEST, Lang.get("gui-player-action-invsee-name"),
                Lang.get("gui-player-action-invsee-lore")));

        // Slot 14: Kick Arena
        inv.setItem(SLOT_KICK, GuiUtils.createItem(Material.IRON_BOOTS, Lang.get("gui-player-action-kick-name"),
                Lang.get("gui-player-action-kick-lore")));

        // Slot 16: Ban (placeholder)
        inv.setItem(SLOT_BAN, GuiUtils.createItem(Material.BARRIER, Lang.get("gui-player-action-ban-name"),
                Lang.get("gui-player-action-ban-lore")));

        // Slot 22: Back
        // Store arenaId in invisible item or metadata? Or just assume admin context?
        // We'll put Arena ID in a hidden item (e.g. Barrier renamed) or pass it via
        // lore of Back button.
        // Actually, listener needs to know arenaId to go back.
        // Let's put it in the Back button name? No.
        // Let's store it in a hidden item at slot 0 (gray pane with hidden name).
        ItemStack hidden = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, "ID:" + arenaId);
        inv.setItem(SLOT_HIDDEN, hidden); // Top left

        inv.setItem(SLOT_BACK, GuiUtils.createItem(Material.ARROW, Lang.get("gui-player-action-back")));

        // Fillers
        ItemStack filler = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INV_SIZE; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        admin.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin))
            return;
        String prefix = Lang.get("gui-player-action-title");
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
        String hiddenName = PlainTextComponentSerializer.plainText()
                .serialize(hidden.getItemMeta().displayName());
        String arenaId = hiddenName.replace("ID:", "").trim();

        // Target Name from Title
        String targetName = event.getView().getTitle().substring(prefix.length());
        Player target = Bukkit.getPlayer(targetName);

        if (clicked.getType() == Material.ENDER_PEARL) {
            if (target != null) {
                admin.teleport(target);
                Lang.send(admin, "gui-player-action-teleported", "%player%", targetName);
            } else {
                Lang.send(admin, "gui-player-action-offline");
            }
        } else if (clicked.getType() == Material.CHEST) {
            if (target != null) {
                admin.openInventory(target.getInventory());
            } else {
                Lang.send(admin, "gui-player-action-offline");
            }
        } else if (clicked.getType() == Material.IRON_BOOTS) {
            if (target != null) {
                GameInstance arena = plugin.getArenaManager().getArena(arenaId);
                if (arena != null) {
                    arena.sendToHub(target);
                    Lang.send(target, "gui-player-action-kick-message");
                    Lang.send(admin, "gui-player-action-kick-success", "%player%", targetName);
                }
            } else {

                Lang.send(admin, "gui-player-action-offline");
            }
            plugin.getPlayerListGUI().open(admin, arenaId); // Refresh list
        } else if (clicked.getType() == Material.BARRIER) {
            // Ban: confirmation required
            plugin.getConfirmationGUI().open(admin,
                    Lang.get("gui-player-action-ban-confirm-title", "%player%", targetName),
                    Lang.get("gui-player-action-ban-confirm-subtitle"),
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
                        Lang.send(admin, "gui-player-action-ban-success", "%player%", targetName);
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
