package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GamerulesGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private final String GUI_TITLE = "Gamerules - ";

    public GamerulesGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaName) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaName);
        if (config == null) {
            player.sendMessage(Component.text("Arena introuvable.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, Component.text(GUI_TITLE + arenaName));

        // Define boolean rules to manage
        List<String> rules = Arrays.asList(
                "keep_inventory",
                "immediate_respawn",
                "advance_time",
                "advance_weather",
                "mob_griefing",
                "natural_health_regeneration",
                "spawn_mobs",
                "send_command_feedback",
                "log_admin_commands");

        int slot = 0;
        for (String rule : rules) {
            String valueStr = config.gamerules.getOrDefault(rule, "false");
            boolean enabled = Boolean.parseBoolean(valueStr);

            ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(rule, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Valeur actuelle: ", NamedTextColor.GRAY)
                    .append(Component.text(enabled ? "ON" : "OFF",
                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
            lore.add(Component.text("clic pour basculer", NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Retour", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        inv.setItem(22, back); // Bottom middle

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().toString().contains(GUI_TITLE))
            return;
        // Adventure components in title might need robust check, checking raw string
        // containment key
        // Simple check: if title starts with "Gamerules - " (but Adventure titles are
        // diff)
        // Bukkit.createInventory(..., Component) sets title. event.getView().title()
        // returns Component.
        // We should check string serialization or legacy.
        // For simplicity in this project context (where we used legacy or string titles
        // before),
        // let's assume adventure text components are stringified or we check
        // differently.
        // Actually, older code used String titles?
        // SettingsGUI used: `Bukkit.createInventory(null, 54,
        // Component.text("Paramètres Arène: " + arenaName));`
        // And check: `if (!event.getView().title().equals(Component.text("Paramètres
        // Arène: " + ...)))` is hard because arena name variable.
        // SettingsGUI used `event.getView().getTitle()` (Legacy).
        // I will use `event.getView().getTitle().startsWith("Gamerules - ")`.

        if (!event.getView().getTitle().startsWith(GUI_TITLE))
            return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String arenaName = title.substring(GUI_TITLE.length());

        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaName);
        if (config == null)
            return;

        ItemStack clicked = event.getCurrentItem();

        if (clicked.getType() == Material.ARROW) {
            // Back
            plugin.getSettingsGUI().open(player, arenaName);
            return;
        }

        if (clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.GRAY_DYE) {
            String rule = ((net.kyori.adventure.text.TextComponent) clicked.getItemMeta().displayName()).content();
            // Casting check or use legacy name
            // Adventure returns Component.
            // Better to use ChatColor.stripColor?
            // `clicked.getItemMeta().getDisplayName()` (Legacy)

            // Re-fetch logic simplicity:
            // Use legacy display name for logic triggers
            String ruleName = clicked.getItemMeta().getDisplayName();
            // Strip colors
            ruleName = net.md_5.bungee.api.ChatColor.stripColor(ruleName);

            // Toggle
            String current = config.gamerules.getOrDefault(ruleName, "false");
            boolean newVal = !Boolean.parseBoolean(current);
            config.gamerules.put(ruleName, String.valueOf(newVal));

            plugin.getConfigManager().saveArena(config);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

            // Refresh
            open(player, arenaName);
        }
    }
}
