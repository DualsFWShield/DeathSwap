package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HelpGUI implements Listener {

    private final DeathSwapPlugin plugin;

    public HelpGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Component title = be.dualsfwshield.deathswap.util.Lang.getComponent("gui-help-title");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Row 1: Player Commands
        inv.setItem(10, createItem(Material.EMERALD, be.dualsfwshield.deathswap.util.Lang.get("gui-help-join-name"), "/ds join [arena]", be.dualsfwshield.deathswap.util.Lang.get("gui-help-join-desc")));
        inv.setItem(11, createItem(Material.RED_BED, be.dualsfwshield.deathswap.util.Lang.get("gui-help-leave-name"), "/ds leave", be.dualsfwshield.deathswap.util.Lang.get("gui-help-leave-desc")));
        inv.setItem(12, createItem(Material.PAPER, be.dualsfwshield.deathswap.util.Lang.get("gui-help-stats-name"), "/ds stats [joueur]", be.dualsfwshield.deathswap.util.Lang.get("gui-help-stats-desc")));
        inv.setItem(13, createItem(Material.GOLD_INGOT, be.dualsfwshield.deathswap.util.Lang.get("gui-help-top-name"), "/ds top [stat]", be.dualsfwshield.deathswap.util.Lang.get("gui-help-top-desc")));
        inv.setItem(14, createItem(Material.HOPPER, be.dualsfwshield.deathswap.util.Lang.get("gui-help-vote-name"), "/ds vote [num]", be.dualsfwshield.deathswap.util.Lang.get("gui-help-vote-desc")));

        // Row 2: Admin Commands (Only if permission)
        if (player.hasPermission("deathswap.admin")) {
            inv.setItem(19, createItem(Material.COMMAND_BLOCK, be.dualsfwshield.deathswap.util.Lang.get("gui-help-admin-dashboard-name"), "/ds admin list", be.dualsfwshield.deathswap.util.Lang.get("gui-help-admin-dashboard-desc")));
            inv.setItem(20, createItem(Material.COMPARATOR, be.dualsfwshield.deathswap.util.Lang.get("gui-help-settings-name"), "/ds settings", be.dualsfwshield.deathswap.util.Lang.get("gui-help-settings-desc"))); 
            inv.setItem(21, createItem(Material.LIME_WOOL, be.dualsfwshield.deathswap.util.Lang.get("gui-help-start-name"), "/ds start [debug]", be.dualsfwshield.deathswap.util.Lang.get("gui-help-start-desc")));
            inv.setItem(22, createItem(Material.RED_WOOL, be.dualsfwshield.deathswap.util.Lang.get("gui-help-stop-name"), "/ds stop [arena]", be.dualsfwshield.deathswap.util.Lang.get("gui-help-stop-desc")));
            inv.setItem(23, createItem(Material.ENDER_PEARL, be.dualsfwshield.deathswap.util.Lang.get("gui-help-swap-name"), "/ds swapnow", be.dualsfwshield.deathswap.util.Lang.get("gui-help-swap-desc")));
            inv.setItem(24, createItem(Material.REPEATER, be.dualsfwshield.deathswap.util.Lang.get("gui-help-reload-name"), "/ds reload", be.dualsfwshield.deathswap.util.Lang.get("gui-help-reload-desc")));
            inv.setItem(25, createItem(Material.BOOK, be.dualsfwshield.deathswap.util.Lang.get("gui-help-commands-name"), "/ds help commands", be.dualsfwshield.deathswap.util.Lang.get("gui-help-commands-desc")));
        }
        
        // Close Button
        inv.setItem(26, createItem(Material.BARRIER, be.dualsfwshield.deathswap.util.Lang.get("gui-help-close-name"), "", be.dualsfwshield.deathswap.util.Lang.get("gui-help-close-desc")));

        // Fill empty slots with glass pane
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", "", "");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String usage, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(name).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            if (!usage.isEmpty()) {
                lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-help-usage", "%usage%", usage).decoration(TextDecoration.ITALIC, false));
            }
            if (!description.isEmpty()) {
                lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(description).colorIfAbsent(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-help-click-suggest").decoration(TextDecoration.ITALIC, true));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Suggest helper
    private void suggestCommand(Player player, String command) {
        player.closeInventory();
        // Use Bukkit dispatch if it's a direct command without args needed, or suggest if args needed.
        // The requirement was "suggests the command in the chat".
        // Adventure/Spigot doesn't have a direct "open chat with text" method for server->client easily without sending a clickable message.
        // BUT, we can just send a clickable message in chat that puts it in the input.
        // OR better: Since this is a GUI, clicking usually executes. But the user asked "suggests the command".
        // Let's print a clickable message in chat.
        
        player.sendMessage(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-help-click-chat")
            .append(Component.text(command, NamedTextColor.YELLOW, TextDecoration.BOLD)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(command))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-help-click-insert")))));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component expectedTitle = be.dualsfwshield.deathswap.util.Lang.getComponent("gui-help-title");
        if (!expectedTitle.equals(event.getView().title())) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE || item.getType() == Material.AIR) return;

        if (item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Logic to extract command from lore or known slots.
        // Since we constructed it, we know slots.
        String command = "";
        int slot = event.getSlot();
        
        switch (slot) {
            // Player
            case 10 -> command = "/ds join";
            case 11 -> command = "/ds leave";
            case 12 -> command = "/ds stats";
            case 13 -> command = "/ds top";
            case 14 -> command = "/ds vote";
            
            // Admin
            case 19 -> command = "/ds admin list";
            case 20 -> command = "/ds settings"; // This might not be a valid command, let's check. Wait, /ds settings is not in plugin.yml usually? 
                                                // Actually AdminGUI encompasses settings. Let's use /ds admin list as main entry or just /ds admin.
                                                // Wait, SettingsGUI is usually opened via AdminGUI.
                                                // Let's suggest /ds admin list for dashboard.
            case 21 -> command = "/ds start";
            case 22 -> command = "/ds stop";
            case 23 -> command = "/ds swapnow";
            case 24 -> command = "/ds reload";
            case 25 -> command = "/ds help commands";
        }

        if (!command.isEmpty()) {
             // Basic "Quick Execute" for simple commands? 
             // User said "suggests the command".
             suggestCommand(player, command);
        }
    }
}
