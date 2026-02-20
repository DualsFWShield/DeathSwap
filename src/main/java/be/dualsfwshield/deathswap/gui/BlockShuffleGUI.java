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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockShuffleGUI implements Listener {

    public enum FilterMode {
        ALL, ENABLED_ONLY, DISABLED_ONLY
    }

    private final DeathSwapPlugin plugin;
    private static final int INV_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    // State management per player
    private final Map<UUID, FilterMode> playerFilters = new HashMap<>();
    private final Map<UUID, String> playerSearches = new HashMap<>();

    public BlockShuffleGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaId, int page) {
        ConfigManager.BlockShuffleConfig globalConfig = plugin.getConfigManager().getBlockShuffleConfig();
        ConfigManager.ArenaConfig arenaConfig = plugin.getConfigManager().getArenaConfig(arenaId);
        List<ConfigManager.BlockShuffleEntry> entries = globalConfig.getEntries();

        FilterMode filter = playerFilters.getOrDefault(player.getUniqueId(), FilterMode.ALL);
        String searchQuery = playerSearches.getOrDefault(player.getUniqueId(), "");

        // Filter the entries based on search and status
        List<ConfigManager.BlockShuffleEntry> filteredEntries = entries.stream().filter(e -> {
            if (filter == FilterMode.ENABLED_ONLY && !e.enabled())
                return false;
            if (filter == FilterMode.DISABLED_ONLY && e.enabled())
                return false;
            if (!searchQuery.isEmpty() && !e.material().toLowerCase().contains(searchQuery.toLowerCase()))
                return false;
            return true;
        }).collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filteredEntries.size() / ITEMS_PER_PAGE);
        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        String title = Lang.get("gui-shuffle-title-blockshuffle") + " (" + page + "/"
                + (totalPages > 0 ? totalPages : 1) + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, GuiUtils.colorize(title));

        // Fill empty background slots
        ItemStack filler = GuiUtils.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            inv.setItem(i, filler);
        }

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredEntries.size());

        for (int i = startIndex; i < endIndex; i++) {
            ConfigManager.BlockShuffleEntry entry = filteredEntries.get(i);
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

        // New Navigation & Tools
        if (page > 1) {
            inv.setItem(45, GuiUtils.createItem(Material.ARROW, Lang.get("gui-shuffle-prev-page")));
        }

        String currentSearch = searchQuery.isEmpty() ? Lang.get("none") : searchQuery;
        inv.setItem(46, GuiUtils.createItem(Material.COMPASS,
                Lang.get("gui-shuffle-search-name"),
                Lang.get("gui-shuffle-search-lore"),
                Lang.get("gui-shuffle-search-current", "%query%", currentSearch)));

        String filterName = filter == FilterMode.ALL ? Lang.get("gui-shuffle-filter-all")
                : filter == FilterMode.ENABLED_ONLY ? Lang.get("gui-shuffle-filter-enabled")
                        : Lang.get("gui-shuffle-filter-disabled");
        inv.setItem(47, GuiUtils.createItem(Material.HOPPER,
                Lang.get("gui-shuffle-filter-name"),
                Lang.get("gui-settings-status", "%status%", filterName)));

        if (page < totalPages) {
            inv.setItem(53, GuiUtils.createItem(Material.ARROW, Lang.get("gui-shuffle-next-page")));
        }

        inv.setItem(51, GuiUtils.createItem(Material.REDSTONE_BLOCK, Lang.get("gui-shuffle-disable-all-name")));
        inv.setItem(52, GuiUtils.createItem(Material.EMERALD_BLOCK, Lang.get("gui-shuffle-enable-all-name")));

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
        FilterMode filter = playerFilters.getOrDefault(player.getUniqueId(), FilterMode.ALL);
        String searchQuery = playerSearches.getOrDefault(player.getUniqueId(), "");

        List<ConfigManager.BlockShuffleEntry> filteredEntries = entries.stream().filter(e -> {
            if (filter == FilterMode.ENABLED_ONLY && !e.enabled())
                return false;
            if (filter == FilterMode.DISABLED_ONLY && e.enabled())
                return false;
            if (!searchQuery.isEmpty() && !e.material().toLowerCase().contains(searchQuery.toLowerCase()))
                return false;
            return true;
        }).collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) filteredEntries.size() / ITEMS_PER_PAGE);

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

        // Search
        if (slot == 46) {
            String finalArenaId = arenaId;
            plugin.getChatInputListener().requestInput(player, Lang.get("gui-shuffle-search-prompt"), (input) -> {
                if (input.equalsIgnoreCase("clear") || input.isEmpty()) {
                    playerSearches.put(player.getUniqueId(), "");
                } else {
                    playerSearches.put(player.getUniqueId(), input);
                }
                open(player, finalArenaId, 1);
            });
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }

        // Filter
        if (slot == 47) {
            FilterMode nextMode = filter == FilterMode.ALL ? FilterMode.ENABLED_ONLY
                    : filter == FilterMode.ENABLED_ONLY ? FilterMode.DISABLED_ONLY : FilterMode.ALL;
            playerFilters.put(player.getUniqueId(), nextMode);
            open(player, arenaId, 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1.2f);
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

        // Mass Actions
        if (slot == 51) {
            for (ConfigManager.BlockShuffleEntry e : filteredEntries) {
                config.setEnabled(e.material(), false);
            }
            config.save();
            open(player, arenaId, page);
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1, 0.8f);
            return;
        }
        if (slot == 52) {
            for (ConfigManager.BlockShuffleEntry e : filteredEntries) {
                config.setEnabled(e.material(), true);
            }
            config.save();
            open(player, arenaId, page);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2f);
            return;
        }

        // Click on item
        if (slot < ITEMS_PER_PAGE && current.getType() != Material.BLACK_STAINED_GLASS_PANE) {
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            if (index >= filteredEntries.size())
                return;

            ConfigManager.BlockShuffleEntry entry = filteredEntries.get(index);
            boolean changed = false;

            if (event.getClick().isLeftClick()) {
                // Toggle enable
                boolean newState = !entry.enabled();
                config.setEnabled(entry.material(), newState);
                changed = true;
                player.playSound(player.getLocation(),
                        newState ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.BLOCK_WOOD_BREAK, 1, 1);
            } else if (event.getClick().isRightClick()) {
                // Cycle difficulty 1->2->3->1
                int newDiff = entry.difficulty() + 1;
                if (newDiff > 3)
                    newDiff = 1;
                config.setDifficulty(entry.material(), newDiff);
                changed = true;

                float pitch = newDiff == 1 ? 1.0f : newDiff == 2 ? 1.3f : 1.6f;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, pitch);
            }

            if (changed) {
                config.save();
                open(player, arenaId, page);
            }
        }
    }
}
