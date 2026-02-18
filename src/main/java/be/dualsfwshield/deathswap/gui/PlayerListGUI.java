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

public class PlayerListGUI implements Listener {

    private final DeathSwapPlugin plugin;
    // Removed static TITLE_PREFIX, using key instead

    public PlayerListGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, String arenaId) {
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null)
            return;

        String prefix = be.dualsfwshield.deathswap.util.Lang.get("gui-player-list-title");
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(prefix + arenaId));

        int slot = 0;
        for (Player p : arena.getAllPlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            meta.displayName(Component.text(p.getName(), NamedTextColor.AQUA, TextDecoration.BOLD));
            // Lore: Health, etc.
            meta.lore(java.util.List.of(
                    be.dualsfwshield.deathswap.util.Lang.getComponent("gui-player-list-health", "%health%", String.valueOf((int) p.getHealth())).color(NamedTextColor.RED),
                    be.dualsfwshield.deathswap.util.Lang.getComponent("gui-player-list-food", "%food%", String.valueOf(p.getFoodLevel())).color(NamedTextColor.GOLD),
                    Component.empty(),
                    be.dualsfwshield.deathswap.util.Lang.getComponent("gui-player-list-action-hint").color(NamedTextColor.YELLOW)));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        // Back button
        ItemStack back = AdminGUI.createItem(Material.ARROW, be.dualsfwshield.deathswap.util.Lang.get("gui-player-list-back"));
        inv.setItem(53, back);

        admin.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin))
            return;
        String prefix = be.dualsfwshield.deathswap.util.Lang.get("gui-player-list-title");
        if (!event.getView().getTitle().startsWith(prefix))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        String arenaId = event.getView().getTitle().substring(prefix.length());

        if (clicked.getType() == Material.ARROW && event.getSlot() == 53) {
            plugin.getArenaDetailsGUI().open(admin, arenaId);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            // Open Action Menu for this player
            // But verify player is still online/valid?
            // Get name from head
            String playerName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(clicked.getItemMeta().displayName());
            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                // Open PlayerActionGUI (To implement)
                plugin.getPlayerActionGUI().open(admin, arenaId, target);
            } else {
                be.dualsfwshield.deathswap.util.Lang.send(admin, "gui-player-list-offline");
            }
        }
    }
}
