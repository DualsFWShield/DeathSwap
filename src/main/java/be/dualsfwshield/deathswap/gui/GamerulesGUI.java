package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Paginated GUI for toggling boolean and integer gamerules.
 * <p>
 * Layout (54 slots = 6 rows):
 * Rows 1-5 (45 slots): gamerule items
 * Row 6: [Prev] · · [Reset] · [Back] · · [Next]
 */
public class GamerulesGUI implements Listener {

    private static final int INV_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows × 9

    private final DeathSwapPlugin plugin;

    /** Tracks the current page per player. */
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public GamerulesGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Build the combined rule list (sorted: booleans then integers) ────

    private List<RuleEntry> buildRuleList() {
        List<RuleEntry> boolRules = new ArrayList<>();
        List<RuleEntry> intRules = new ArrayList<>();

        for (GameRule<?> rule : org.bukkit.Registry.GAME_RULE) {
            if (rule == null)
                continue;
            String key = rule.getKey().getKey();
            if (key.equalsIgnoreCase("pvp"))
                continue;

            if (rule.getType() == Boolean.class) {
                boolRules.add(new RuleEntry(key, RuleType.BOOLEAN));
            } else if (rule.getType() == Integer.class) {
                intRules.add(new RuleEntry(key, RuleType.INTEGER));
            }
        }

        boolRules.sort((a, b) -> a.key.compareToIgnoreCase(b.key));
        intRules.sort((a, b) -> a.key.compareToIgnoreCase(b.key));

        List<RuleEntry> all = new ArrayList<>(boolRules);
        all.addAll(intRules);
        return all;
    }

    // ── Open the GUI on a specific page ─────────────────────────────────

    public void open(Player player, String arenaName) {
        open(player, arenaName, playerPages.getOrDefault(player.getUniqueId(), 0));
    }

    public void open(Player player, String arenaName, int page) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaName);
        if (config == null) {
            Lang.send(player, "gui-gamerules-error-arena");
            return;
        }

        List<RuleEntry> allRules = buildRuleList();
        int totalPages = Math.max(1, (int) Math.ceil((double) allRules.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        String title = Lang.get("gui-gamerules-title", "%arena%", arenaName);
        String pageInfo = " (" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, Component.text(title + pageInfo));

        // Fill rule items for this page
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allRules.size());

        for (int i = startIndex; i < endIndex; i++) {
            RuleEntry entry = allRules.get(i);
            int slot = i - startIndex;

            if (entry.type == RuleType.BOOLEAN) {
                inv.setItem(slot, buildBoolItem(entry.key, config));
            } else {
                inv.setItem(slot, buildIntItem(entry.key, config));
            }
        }

        // ── Bottom row (slots 45-53) ────────────────────────────────

        // Previous page (slot 45)
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("◀ Page " + page, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        // Reset button (slot 48)
        ItemStack reset = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta resetMeta = reset.getItemMeta();
        resetMeta.displayName(Component.text("Reset Gamerules", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
        List<Component> resetLore = new ArrayList<>();
        resetLore.add(Component.empty());
        resetLore.add(Component.text("Clic G: ", NamedTextColor.GREEN)
                .append(Component.text("Preset DeathSwap", NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        resetLore.add(Component.text("Clic D: ", NamedTextColor.RED)
                .append(Component.text("Défaut Minecraft", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        resetMeta.lore(resetLore);
        reset.setItemMeta(resetMeta);
        inv.setItem(48, reset);

        // Back button (slot 49)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Lang.getComponent("gui-gamerules-back")
                .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

        // Next page (slot 53)
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Page " + (page + 2) + " ▶", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    // ── Item builders ───────────────────────────────────────────────────

    private ItemStack buildBoolItem(String rule, ConfigManager.ArenaConfig config) {
        String valueStr = config.gamerules.getOrDefault(rule, "false");
        boolean enabled = Boolean.parseBoolean(valueStr);

        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(rule, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Lang.getComponent("gui-gamerules-current", "%value%", "")
                .append(Component.text(enabled ? "True" : "False",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.empty());
        lore.add(Lang.getComponent("gui-gamerules-click-toggle")
                .color(NamedTextColor.YELLOW));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildIntItem(String rule, ConfigManager.ArenaConfig config) {
        String valueStr = config.gamerules.getOrDefault(rule, "0");
        int currentValue;
        try {
            currentValue = Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            currentValue = 0;
        }

        ItemStack item = new ItemStack(Material.REPEATER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(rule, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Lang.getComponent("gui-gamerules-current", "%value%", "")
                .append(Component.text(String.valueOf(currentValue), NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        lore.add(Component.text("Clic G: ", NamedTextColor.GREEN)
                .append(Component.text("+1", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Clic D: ", NamedTextColor.RED)
                .append(Component.text("-1", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Clic G: ", NamedTextColor.GREEN)
                .append(Component.text("+10", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Clic D: ", NamedTextColor.RED)
                .append(Component.text("-10", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Click handler ───────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String expectedTitlePrefix = Lang.get("gui-gamerules-title").replace("%arena%", "");
        String rawTitle = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (!rawTitle.contains(expectedTitlePrefix))
            return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
            return;

        Player player = (Player) event.getWhoClicked();

        // Extract arena name (title format: "prefix<arena> (x/y)")
        String afterPrefix = rawTitle.substring(expectedTitlePrefix.length());
        // Remove the page suffix " (x/y)"
        String arenaName = afterPrefix.replaceAll("\\s*\\(\\d+/\\d+\\)$", "");

        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaName);
        if (config == null)
            return;

        ItemStack clicked = event.getCurrentItem();
        int slot = event.getRawSlot();

        // ── Navigation (bottom row) ─────────────────────────────────

        // Back button
        if (slot == 49 && clicked.getType() == Material.ARROW) {
            playerPages.remove(player.getUniqueId());
            plugin.getSettingsGUI().open(player, arenaName);
            return;
        }

        // Previous page
        if (slot == 45 && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            open(player, arenaName, currentPage - 1);
            return;
        }

        // Next page
        if (slot == 53 && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            open(player, arenaName, currentPage + 1);
            return;
        }

        // Reset button
        if (slot == 48 && clicked.getType() == Material.STRUCTURE_VOID) {
            ClickType click = event.getClick();
            if (click.isLeftClick()) {
                resetToPreset(config);
                player.sendMessage(Component.text("[DeathSwap] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Gamerules reset to DeathSwap preset!", NamedTextColor.GREEN)));
            } else if (click.isRightClick()) {
                resetToVanilla(config);
                player.sendMessage(Component.text("[DeathSwap] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Gamerules reset to Minecraft defaults!", NamedTextColor.AQUA)));
            }
            plugin.getConfigManager().saveArena(config);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            open(player, arenaName);
            return;
        }

        // ── Gamerule items (slots 0-44) ─────────────────────────────

        if (slot >= ITEMS_PER_PAGE)
            return; // Ignore bottom row clicks on empty slots

        String ruleName = PlainTextComponentSerializer.plainText()
                .serialize(clicked.getItemMeta().displayName());

        // Boolean gamerule toggle
        if (clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.GRAY_DYE) {
            String current = config.gamerules.getOrDefault(ruleName, "false");
            boolean newVal = !Boolean.parseBoolean(current);
            config.gamerules.put(ruleName, String.valueOf(newVal));

            plugin.getConfigManager().saveArena(config);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            open(player, arenaName);
        }

        // Integer gamerule +/- adjustment
        if (clicked.getType() == Material.REPEATER) {
            String currentStr = config.gamerules.getOrDefault(ruleName, "0");
            int currentVal;
            try {
                currentVal = Integer.parseInt(currentStr);
            } catch (NumberFormatException e) {
                currentVal = 0;
            }

            ClickType click = event.getClick();
            if (click == ClickType.LEFT) {
                currentVal += 1;
            } else if (click == ClickType.RIGHT) {
                currentVal -= 1;
            } else if (click == ClickType.SHIFT_LEFT) {
                currentVal += 10;
            } else if (click == ClickType.SHIFT_RIGHT) {
                currentVal -= 10;
            }

            // Allow -1 (unlimited/disabled for some rules) but not lower
            if (currentVal < -1)
                currentVal = -1;

            config.gamerules.put(ruleName, String.valueOf(currentVal));
            plugin.getConfigManager().saveArena(config);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            open(player, arenaName);
        }
    }

    // ── Reset helpers ───────────────────────────────────────────────────

    /**
     * Reset gamerules to the DeathSwap plugin preset (ArenaConfig constructor
     * defaults).
     */
    private void resetToPreset(ConfigManager.ArenaConfig config) {
        ConfigManager.ArenaConfig defaults = new ConfigManager.ArenaConfig();
        config.gamerules.clear();
        config.gamerules.putAll(defaults.gamerules);
    }

    /**
     * Reset gamerules to Minecraft vanilla defaults by reading from a loaded world.
     */
    @SuppressWarnings("unchecked")
    private void resetToVanilla(ConfigManager.ArenaConfig config) {
        config.gamerules.clear();

        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        for (GameRule<?> rule : org.bukkit.Registry.GAME_RULE) {
            if (rule == null)
                continue;
            String key = rule.getKey().getKey();

            if (rule.getType() == Boolean.class) {
                boolean defaultVal = getVanillaDefault((GameRule<Boolean>) rule, world);
                config.gamerules.put(key, String.valueOf(defaultVal));
            } else if (rule.getType() == Integer.class) {
                int defaultVal = getVanillaIntDefault((GameRule<Integer>) rule, world);
                config.gamerules.put(key, String.valueOf(defaultVal));
            }
        }
    }

    private boolean getVanillaDefault(GameRule<Boolean> rule, World world) {
        if (world != null) {
            Boolean val = world.getGameRuleDefault(rule);
            if (val != null)
                return val;
        }
        return true; // Most boolean gamerules default to true
    }

    private int getVanillaIntDefault(GameRule<Integer> rule, World world) {
        if (world != null) {
            Integer val = world.getGameRuleDefault(rule);
            if (val != null)
                return val;
        }
        String name = rule.getName();
        if (name.equals("randomTickSpeed"))
            return 3;
        if (name.equals("maxEntityCramming"))
            return 24;
        if (name.equals("maxCommandChainLength"))
            return 65536;
        if (name.equals("spawnRadius"))
            return 10;
        return 0;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private enum RuleType {
        BOOLEAN, INTEGER
    }

    private static class RuleEntry {
        final String key;
        final RuleType type;

        RuleEntry(String key, RuleType type) {
            this.key = key;
            this.type = type;
        }
    }
}
