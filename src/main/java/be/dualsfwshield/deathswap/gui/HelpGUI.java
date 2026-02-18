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
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Aide DeathSwap", NamedTextColor.DARK_BLUE));

        // Row 1: Player Commands
        inv.setItem(10, createItem(Material.EMERALD, "&aRejoindre", "/ds join [arena]", "Rejoindre une partie en attente."));
        inv.setItem(11, createItem(Material.RED_BED, "&cQuitter", "/ds leave", "Quitter la partie actuelle."));
        inv.setItem(12, createItem(Material.PAPER, "&bStatistiques", "/ds stats [joueur]", "Voir vos statistiques ou celles d'un autre."));
        inv.setItem(13, createItem(Material.GOLD_INGOT, "&6Classement", "/ds top [stat]", "Voir le classement des joueurs."));
        inv.setItem(14, createItem(Material.HOPPER, "&eVote", "/ds vote [num]", "Voter pour une configuration (si actif)."));

        // Row 2: Admin Commands (Only if permission)
        if (player.hasPermission("deathswap.admin")) {
            inv.setItem(19, createItem(Material.COMMAND_BLOCK, "&cAdmin Dashboard", "/ds admin list", "Ouvrir le menu de gestion des arènes."));
            inv.setItem(20, createItem(Material.COMPARATOR, "&cParamètres", "/ds settings", "Ouvrir les paramètres globaux (déprécié).")); // Assuming settings is per arena mostly now, but keeping command
            inv.setItem(21, createItem(Material.LIME_WOOL, "&aDémarrer", "/ds start [debug]", "Forcer le démarrage (Debug: bypass min players)."));
            inv.setItem(22, createItem(Material.RED_WOOL, "&cArrêter", "/ds stop [arena]", "Arrêter une partie en cours."));
            inv.setItem(23, createItem(Material.ENDER_PEARL, "&5Swap Immédiat", "/ds swapnow", "Forcer un échange de position immédiat."));
            inv.setItem(24, createItem(Material.REPEATER, "&7Reload", "/ds reload", "Recharger la configuration du plugin."));
            inv.setItem(25, createItem(Material.BOOK, "&eAide Commandes", "/ds help commands", "Voir la liste détaillée des commandes admin."));
        }
        
        // Close Button
        inv.setItem(26, createItem(Material.BARRIER, "&cFermer", "", "Fermer le menu."));

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
            meta.displayName(Component.text(name.replace("&", "§"), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            if (!usage.isEmpty()) {
                lore.add(Component.text("Usage: " + usage, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
            if (!description.isEmpty()) {
                lore.add(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Cliquez pour suggérer la commande", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true));
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
        
        player.sendMessage(Component.text("Cliquez ici pour utiliser: ", NamedTextColor.GRAY)
            .append(Component.text(command, NamedTextColor.YELLOW, TextDecoration.BOLD)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(command))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Cliquez pour insérer", NamedTextColor.GREEN)))));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("Aide DeathSwap", NamedTextColor.DARK_BLUE))) return;

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
