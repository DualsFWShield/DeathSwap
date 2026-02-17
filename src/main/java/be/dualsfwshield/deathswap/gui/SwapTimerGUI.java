package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.SwapMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static be.dualsfwshield.deathswap.gui.SettingsGUI.createItem;
import static be.dualsfwshield.deathswap.gui.SettingsGUI.colorize;
import static be.dualsfwshield.deathswap.gui.SettingsGUI.formatTime;

/**
 * Sub-GUI for configuring swap timer presets and mode (Fixed / Random).
 * 27 slots (3 rows).
 */
public class SwapTimerGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private static final Component TITLE = Component.text("⏱ Swap Timer", NamedTextColor.GOLD, TextDecoration.BOLD);

    // Preset values in minutes
    private static final int[] PRESETS = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    public SwapTimerGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the swap timer GUI for a specific arena.
     */
    public void open(Player player, String arenaId) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Row 1: Preset buttons (slots 0-9) — 1 to 10 minutes
        Material[] clockMaterials = {
                Material.WHITE_CONCRETE, Material.ORANGE_CONCRETE, Material.MAGENTA_CONCRETE,
                Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
                Material.PINK_CONCRETE, Material.CYAN_CONCRETE, Material.PURPLE_CONCRETE,
                Material.BLUE_CONCRETE
        };

        for (int i = 0; i < PRESETS.length; i++) {
            int minutes = PRESETS[i];
            int seconds = minutes * 60;
            boolean isSelected = config.swapMode == SwapMode.FIXED && config.swapInterval == seconds;

            ItemStack item = new ItemStack(clockMaterials[i], Math.min(minutes, 64));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(colorize(isSelected ? "&a&l" + minutes + " min ✓" : "&e" + minutes + " min")
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(colorize("&7= " + seconds + " secondes"));
            if (isSelected) {
                lore.add(Component.empty());
                lore.add(colorize("&a► Sélectionné"));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(Component.empty());
                lore.add(colorize("&7Cliquez pour sélectionner"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        // Row 2: Mode selection
        // Slot 12: Fixed mode
        boolean isFixed = config.swapMode == SwapMode.FIXED;
        ItemStack fixedItem = createItem(Material.IRON_BLOCK,
                isFixed ? "&a&lMode Fixe ✓" : "&7Mode Fixe",
                "&7Intervalle constant entre",
                "&7chaque swap.",
                "",
                isFixed ? "&a► Actif" : "&7Cliquez pour activer");
        if (isFixed) {
            ItemMeta fixedMeta = fixedItem.getItemMeta();
            fixedMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            fixedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            fixedItem.setItemMeta(fixedMeta);
        }
        inv.setItem(12, fixedItem);

        // Slot 14: Random mode
        boolean isRandom = config.swapMode == SwapMode.RANDOM;
        ItemStack randomItem = createItem(Material.GOLD_BLOCK,
                isRandom ? "&a&lMode Random ✓" : "&7Mode Random",
                "&7Intervalle aléatoire entre",
                "&7un minimum et un maximum.",
                "",
                isRandom ? "&a► Actif" : "&7Cliquez pour activer");
        if (isRandom) {
            ItemMeta randomMeta = randomItem.getItemMeta();
            randomMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            randomMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            randomItem.setItemMeta(randomMeta);
        }
        inv.setItem(14, randomItem);

        // Row 3: Random min/max controls (only visible/active if RANDOM mode)
        if (isRandom) {
            inv.setItem(21, createItem(Material.RED_CONCRETE, "&cMinimum",
                    "&7Actuel: &e" + formatTime(config.swapMin),
                    "",
                    "&aClic G: &7+30s",
                    "&cClic D: &7-30s"));

            inv.setItem(23, createItem(Material.GREEN_CONCRETE, "&aMaximum",
                    "&7Actuel: &e" + formatTime(config.swapMax),
                    "",
                    "&aClic G: &7+30s",
                    "&cClic D: &7-30s"));
        }

        // Slot 18: Arena ID tag (hidden data)
        inv.setItem(18, createItem(Material.NAME_TAG, "&eArène: &6" + arenaId));

        // Slot 26: Back to main settings
        inv.setItem(26, createItem(Material.ARROW, "&7← Retour"));

        // Fill empty slots
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getView().title() == null)
            return;
        if (!event.getView().title().equals(TITLE))
            return;

        event.setCancelled(true);

        String arenaId = getArenaIdFromInventory(event.getView().getTopInventory());
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null)
            return;

        int slot = event.getRawSlot();
        boolean isLeftClick = event.isLeftClick();

        // Preset buttons (slots 0-9)
        if (slot >= 0 && slot <= 9) {
            int minutes = PRESETS[slot];
            config.swapMode = SwapMode.FIXED;
            config.swapInterval = minutes * 60;
            plugin.getConfigManager().save();
            open(player, arenaId); // Refresh
            return;
        }

        switch (slot) {
            case 12 -> {
                // Switch to FIXED mode
                config.swapMode = SwapMode.FIXED;
                plugin.getConfigManager().save();
                open(player, arenaId);
            }
            case 14 -> {
                // Switch to RANDOM mode
                config.swapMode = SwapMode.RANDOM;
                plugin.getConfigManager().save();
                open(player, arenaId);
            }
            case 21 -> {
                // Random MIN: +/- 30s
                config.swapMin += isLeftClick ? 30 : -30;
                config.swapMin = Math.max(30, config.swapMin);
                if (config.swapMin > config.swapMax) {
                    config.swapMax = config.swapMin;
                }
                plugin.getConfigManager().save();
                open(player, arenaId);
            }
            case 23 -> {
                // Random MAX: +/- 30s
                config.swapMax += isLeftClick ? 30 : -30;
                config.swapMax = Math.max(config.swapMin, config.swapMax);
                plugin.getConfigManager().save();
                open(player, arenaId);
            }
            case 26 -> {
                // Back to main settings
                player.closeInventory();
                plugin.getSettingsGUI().open(player, arenaId);
            }
        }
    }

    /**
     * Extract arena ID from the name tag item in slot 18.
     */
    private String getArenaIdFromInventory(Inventory inv) {
        ItemStack nameTag = inv.getItem(18);
        if (nameTag == null || !nameTag.hasItemMeta())
            return "default";

        Component display = nameTag.getItemMeta().displayName();
        if (display == null)
            return "default";

        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(display);
        if (plain.contains(": ")) {
            return plain.substring(plain.indexOf(": ") + 2).trim();
        }
        return "default";
    }
}
