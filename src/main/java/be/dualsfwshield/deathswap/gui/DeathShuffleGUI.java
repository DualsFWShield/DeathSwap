package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.modes.DeathCause;
import be.dualsfwshield.deathswap.util.Lang;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeathShuffleGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private static final int INV_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    public DeathShuffleGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaId, int page) {
        ConfigManager.DeathShuffleConfig globalConfig = plugin.getConfigManager().getDeathShuffleConfig();
        ConfigManager.ArenaConfig arenaConfig = plugin.getConfigManager().getArenaConfig(arenaId);
        List<ConfigManager.DeathShuffleEntry> entries = globalConfig.getEntries();

        int totalPages = (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE);
        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        String title = Lang.get("gui-shuffle-title-deathshuffle") + " (" + page + "/"
                + (totalPages > 0 ? totalPages : 1) + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, GuiUtils.colorize(title));

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            ConfigManager.DeathShuffleEntry entry = entries.get(i);
            DeathCause dc;
            try {
                dc = DeathCause.valueOf(entry.cause());
            } catch (IllegalArgumentException e) {
                // Should not happen if config is managed correctly, but fallback
                continue;
            }

            String status = entry.enabled() ? Lang.get("gui-shuffle-enabled") : Lang.get("gui-shuffle-disabled");
            String difficultyName = getDifficultyName(entry.difficulty());

            Material icon = getIcon(dc);

            inv.setItem(i - startIndex, GuiUtils.createItem(icon,
                    "&e" + dc.getDisplayName(),
                    status,
                    Lang.get("gui-shuffle-difficulty", "%difficulty%", difficultyName),
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
            String raceStatus = arenaConfig.deathShuffleRaceMode ? Lang.get("enabled") : Lang.get("disabled");
            inv.setItem(48, GuiUtils.createItem(Material.GOLDEN_BOOTS,
                    Lang.get("gui-settings-race-mode-death-name"),
                    Lang.get("gui-settings-race-mode-death-lore"),
                    Lang.get("gui-settings-status", "%status%", raceStatus),
                    "",
                    Lang.get("gui-settings-click-toggle")));

            String uniqueStatus = arenaConfig.deathShuffleUniqueCauses ? Lang.get("enabled") : Lang.get("disabled");
            inv.setItem(50, GuiUtils.createItem(Material.TARGET,
                    Lang.get("gui-settings-unique-causes-name"),
                    Lang.get("gui-settings-unique-causes-lore"),
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
                .startsWith(ChatColor.translateAlternateColorCodes('&', Lang.get("gui-shuffle-title-deathshuffle"))))
            return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR)
            return;

        ConfigManager.DeathShuffleConfig config = plugin.getConfigManager().getDeathShuffleConfig();
        List<ConfigManager.DeathShuffleEntry> entries = config.getEntries();
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
            return;
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
            plugin.getSettingsGUI().open(player, arenaId);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }

        // Toggles
        if (slot == 48 && arenaConfig != null) {
            arenaConfig.deathShuffleRaceMode = !arenaConfig.deathShuffleRaceMode;
            plugin.getConfigManager().saveArena(arenaConfig);
            open(player, arenaId, page);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot == 50 && arenaConfig != null) {
            arenaConfig.deathShuffleUniqueCauses = !arenaConfig.deathShuffleUniqueCauses;
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

            ConfigManager.DeathShuffleEntry entry = entries.get(index);
            boolean changed = false;

            if (event.getClick().isLeftClick()) {
                // Toggle enable
                config.setEnabled(entry.cause(), !entry.enabled());
                changed = true;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            } else if (event.getClick().isRightClick()) {
                // Cycle difficulty 1->2->3->1
                int newDiff = entry.difficulty() + 1;
                if (newDiff > 3)
                    newDiff = 1;
                config.setDifficulty(entry.cause(), newDiff);
                changed = true;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            }

            if (changed) {
                config.save();
                open(player, arenaId, page);
            }
        }
    }

    private Material getIcon(DeathCause dc) {
        switch (dc) {
            case DROWNING:
                return Material.WATER_BUCKET;
            case FALL:
                return Material.FEATHER;
            case FIRE:
                return Material.FLINT_AND_STEEL;
            case CONTACT:
                return Material.CACTUS;
            case STARVATION:
                return Material.ROTTEN_FLESH;
            case SUFFOCATION:
                return Material.SAND;
            case LAVA:
                return Material.LAVA_BUCKET;
            case EXPLOSION:
                return Material.TNT;
            case PROJECTILE:
                return Material.ARROW;
            case MAGIC:
                return Material.POTION;
            case HOT_FLOOR:
                return Material.MAGMA_BLOCK;
            case FREEZE:
                return Material.POWDER_SNOW_BUCKET;
            case LIGHTNING:
                return Material.LIGHTNING_ROD;
            case FLY_INTO_WALL:
                return Material.ELYTRA;
            case FALLING_BLOCK:
                return Material.ANVIL;
            case VOID:
                return Material.ENDER_EYE;
            default:
                return Material.SKELETON_SKULL;
        }
    }
}
