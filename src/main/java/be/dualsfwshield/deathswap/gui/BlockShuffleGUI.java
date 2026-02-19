package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.util.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BlockShuffleGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private static final int INV_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    public BlockShuffleGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaId, int page) {
        ConfigManager.BlockShuffleConfig globalConfig = plugin.getConfigManager().getBlockShuffleConfig();
        ConfigManager.ArenaConfig arenaConfig = plugin.getConfigManager().getArenaConfig(arenaId);
        List<ConfigManager.BlockShuffleEntry> entries = globalConfig.getEntries();

        int totalPages = (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE);
        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        String title = Lang.get("gui-shuffle-title-blockshuffle") + " (" + page + "/"
                + (totalPages > 0 ? totalPages : 1) + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, GuiUtils.colorize(title));

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            ConfigManager.BlockShuffleEntry entry = entries.get(i);
            Material mat = Material.matchMaterial(entry.material());
            if (mat == null)
                mat = Material.BEDROCK;

            String status = entry.enabled() ? Lang.get("gui-shuffle-enabled") : Lang.get("gui-shuffle-disabled");
            String difficultyName = getDifficultyName(entry.difficulty());

            inv.setItem(i - startIndex, GuiUtils.createItem(mat,
                    "&e" + mat.name(),
                    status,
                    Lang.get("gui-shuffle-difficulty", "%difficulty%", difficultyName),
                    Lang.get("gui-shuffle-type", "%type%", entry.type()),
                    "",
                    Lang.get("gui-shuffle-click-toggle"),
                    Lang.get("gui-shuffle-click-difficulty")));
        }

        // Navigation
        if (page > 1) {
            inv.setItem(45, GuiUtils.createItem(Material.ARROW, Lang.get("gui-shuffle-prev-page")));
        }
        if (page < totalPages) {
            inv.setItem(53, GuiUtils.createItem(Material.ARROW, Lang.get("gui-shuffle-next-page")));
        }

        // Toggles
        if (arenaConfig != null) {
            String raceStatus = arenaConfig.blockShuffleRaceMode ? Lang.get("enabled") : Lang.get("disabled");
            inv.setItem(48, GuiUtils.createItem(Material.GOLDEN_BOOTS,
                    Lang.get("gui-settings-race-mode-block-name"),
                    Lang.get("gui-settings-race-mode-block-lore"),
                    Lang.get("gui-settings-status", "%status%", raceStatus),
                    "",
                    Lang.get("gui-settings-click-toggle")));

            String uniqueStatus = arenaConfig.blockShuffleUniqueTargets ? Lang.get("enabled") : Lang.get("disabled");
            inv.setItem(50, GuiUtils.createItem(Material.TARGET,
                    Lang.get("gui-settings-unique-targets-name"),
                    Lang.get("gui-settings-unique-targets-lore"),
                    Lang.get("gui-settings-status", "%status%", uniqueStatus),
                    "",
                    Lang.get("gui-settings-click-toggle")));
        }

        inv.setItem(49, GuiUtils.createItem(Material.BARRIER, Lang.get("gui-shuffle-back"), "&8Arena: " + arenaId));

        player.openInventory(inv);
    }

    private String getDifficultyName(int difficulty) {
        if (difficulty == 1)
            return Lang.get("gui-shuffle-difficulty-easy");
        if (difficulty == 2)
            return Lang.get("gui-shuffle-difficulty-medium");
        return Lang.get("gui-shuffle-difficulty-hard");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle()
                .startsWith(ChatColor.translateAlternateColorCodes('&', Lang.get("gui-shuffle-title-blockshuffle"))))
            return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR)
            return;

        ConfigManager.BlockShuffleConfig config = plugin.getConfigManager().getBlockShuffleConfig();
        List<ConfigManager.BlockShuffleEntry> entries = config.getEntries();
        int totalPages = (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE);

        // Parse page from title
        String title = event.getView().getTitle();
        int page = 1;
        try {
            String pagePart = title.substring(title.lastIndexOf("(") + 1, title.lastIndexOf("/"));
            page = Integer.parseInt(pagePart);
        } catch (Exception e) {
            // ignore
        }

        int slot = event.getSlot();

        // Get Arena ID from Back Button (Slot 49) Lore
        ItemStack backItem = event.getClickedInventory().getItem(49);
        String arenaId = null;
        if (backItem != null && backItem.hasItemMeta() && backItem.getItemMeta().hasLore()) {
            List<String> lore = backItem.getItemMeta().getLore();
            for (String line : lore) {
                if (line.contains("Arena: ")) {
                    arenaId = ChatColor.stripColor(line).replace("Arena: ", "").trim();
                    break;
                }
            }
        }

        if (arenaId == null)
            return; // Should not happen if opened via this GUI
        ConfigManager.ArenaConfig arenaConfig = plugin.getConfigManager().getArenaConfig(arenaId);

        // Pagination & Back
        if (slot == 45 && page > 1) {
            open(player, arenaId, page - 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot == 53 && page < totalPages) {
            open(player, arenaId, page + 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            // Optionally go back to SettingsGUI
            plugin.getSettingsGUI().open(player, arenaId);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }

        // Toggles
        if (slot == 48 && arenaConfig != null) {
            arenaConfig.blockShuffleRaceMode = !arenaConfig.blockShuffleRaceMode;
            plugin.getConfigManager().saveArena(arenaConfig);
            open(player, arenaId, page);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot == 50 && arenaConfig != null) {
            arenaConfig.blockShuffleUniqueTargets = !arenaConfig.blockShuffleUniqueTargets;
            plugin.getConfigManager().saveArena(arenaConfig);
            open(player, arenaId, page);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }

        // Click on item
        if (slot < ITEMS_PER_PAGE) {
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            if (index >= entries.size())
                return;

            ConfigManager.BlockShuffleEntry entry = entries.get(index);
            boolean changed = false;

            if (event.getClick().isLeftClick()) {
                // Toggle enable
                config.setEnabled(entry.material(), !entry.enabled());
                changed = true;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            } else if (event.getClick().isRightClick()) {
                // Cycle difficulty 1->2->3->1
                int newDiff = entry.difficulty() + 1;
                if (newDiff > 3)
                    newDiff = 1;
                config.setDifficulty(entry.material(), newDiff);
                changed = true;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            }

            if (changed) {
                config.save();
                open(player, arenaId, page);
            }
        }
    }
}
