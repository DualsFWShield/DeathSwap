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
    private static final String TITLE_PREFIX = "Players: ";

    public PlayerListGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, String arenaId) {
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE_PREFIX + arenaId));

        int slot = 0;
        for (Player p : arena.getAllPlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            meta.displayName(Component.text(p.getName(), NamedTextColor.AQUA, TextDecoration.BOLD));
            // Lore: Health, etc.
            meta.lore(java.util.List.of(
                    Component.text("Vie: " + (int) p.getHealth() + " HP", NamedTextColor.RED),
                    Component.text("Nourriture: " + p.getFoodLevel(), NamedTextColor.GOLD),
                    Component.empty(),
                    Component.text("Clic G: Actions Joueur", NamedTextColor.YELLOW)));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        // Back button
        ItemStack back = AdminGUI.createItem(Material.ARROW, "&eRetour");
        inv.setItem(53, back);

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
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        String arenaId = event.getView().getTitle().substring(TITLE_PREFIX.length());

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
                admin.sendMessage(Component.text("Joueur hors ligne.", NamedTextColor.RED));
            }
        }
    }
}
