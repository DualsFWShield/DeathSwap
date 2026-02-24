package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
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

    /*
     * ── Inventory layout (27 slots, 3 rows) ─────────────────────────
     *
     * Row 1 │ [gamerule toggles 0..8]
     * Row 2 │ [gamerule toggle 9 if present, rest empty]
     * Row 3 │ · · · · BACK · · · ·
     */
    private static final int INV_SIZE = 27;
    private static final int SLOT_BACK = 22;

    private final DeathSwapPlugin plugin;

    public GamerulesGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaName) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaName);
        if (config == null) {
            Lang.send(player, "gui-gamerules-error-arena");
            return;
        }

        String title = Lang.get("gui-gamerules-title", "%arena%", arenaName);
        // Dynamically fetch all boolean gamerules by reading them from a default world
        List<String> rules = new ArrayList<>();
        for (org.bukkit.GameRule<?> rule : org.bukkit.Registry.GAME_RULE) {
            if (rule != null && rule.getType() == Boolean.class) {
                rules.add(rule.getKey().getKey());
            }
        }

        // We could sort alphabetically
        rules.sort(String::compareToIgnoreCase);

        // Adjust GUI size if too many rules. 54 slots handles ~45 rules safely.
        int requiredSize = Math.max(27, ((rules.size() / 9) + 2) * 9);
        if (requiredSize > 54)
            requiredSize = 54;

        Inventory inv = Bukkit.createInventory(null, requiredSize, Component.text(title));

        int slot = 0;
        for (String rule : rules) {
            if (slot >= requiredSize - 9)
                break; // Leave bottom row for back button

            // Config might still have snake_case from old default
            String valueStr = config.gamerules.getOrDefault(rule, "false");
            boolean enabled = Boolean.parseBoolean(valueStr);

            ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(rule, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Lang.getComponent("gui-gamerules-current", "%value%", "")
                    .append(Component.text(enabled ? "True" : "False",
                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
            lore.add(Component.empty());
            lore.add(Lang.getComponent("gui-gamerules-click-toggle")
                    .color(NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Lang.getComponent("gui-gamerules-back")
                .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        inv.setItem(SLOT_BACK, back); // Bottom middle

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String expectedTitlePrefix = Lang.get("gui-gamerules-title").replace("%arena%",
                "");
        String rawTitle = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (!rawTitle.contains(expectedTitlePrefix))
            return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
            return;

        Player player = (Player) event.getWhoClicked();
        String arenaName = rawTitle.substring(expectedTitlePrefix.length());

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

            // Re-fetch logic simplicity:
            // Use legacy display name for logic triggers
            String ruleName = clicked.getItemMeta().getDisplayName();
            // Strip colors
            ruleName = ChatColor.stripColor(ruleName);

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
