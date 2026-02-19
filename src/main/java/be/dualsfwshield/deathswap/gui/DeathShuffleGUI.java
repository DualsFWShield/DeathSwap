package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.modes.DeathCause;
import be.dualsfwshield.deathswap.util.Lang;
import org.bukkit.Bukkit;
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

    public void open(Player player, int page) {
        ConfigManager.DeathShuffleConfig config = plugin.getConfigManager().getDeathShuffleConfig();
        List<ConfigManager.DeathShuffleEntry> entries = config.getEntries();

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

            // Use PAPER for death causes as they don't have specific materials
            inv.setItem(i - startIndex, GuiUtils.createItem(Material.PAPER,
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

        inv.setItem(49, GuiUtils.createItem(Material.BARRIER, Lang.get("gui-shuffle-back")));

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
        if (!event.getView().getTitle().startsWith(Lang.get("gui-shuffle-title-deathshuffle")))
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

        // Pagination & Back
        if (slot == 45 && page > 1) {
            open(player, page - 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot == 53 && page < totalPages) {
            open(player, page + 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
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
                open(player, page);
            }
        }
    }
}
