package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.UIMode;
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

/**
 * Main settings GUI for configuring an arena (54 slots / 6 rows).
 */
public class SettingsGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private static final Component TITLE = Component.text("⚙ DeathSwap Settings", NamedTextColor.GOLD,
            TextDecoration.BOLD);

    public SettingsGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the settings GUI for a specific arena.
     */
    public void open(Player player, String arenaId) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            player.sendMessage(Component.text("Arena not found: " + arenaId, NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Row 1: Timers
        inv.setItem(10, createItem(Material.CLOCK, "&6Swap Timer",
                "&7Mode: &e" + config.swapMode.name(),
                config.swapMode.name().equals("FIXED")
                        ? "&7Intervalle: &e" + formatTime(config.swapInterval)
                        : "&7Min: &e" + formatTime(config.swapMin) + " &7Max: &e" + formatTime(config.swapMax),
                "",
                "&aCliquez pour configurer"));

        inv.setItem(12, createItem(Material.RECOVERY_COMPASS, "&6Durée Max Partie",
                "&7Actuel: &e" + formatTime(config.maxGameTime),
                "",
                "&aClic G: &7+1 min",
                "&cClic D: &7-1 min"));

        inv.setItem(14, createItem(Material.HOPPER, "&6Temps de Génération",
                "&7Actuel: &e" + config.loadTime + "s",
                "",
                "&aClic G: &7+10s",
                "&cClic D: &7-10s"));

        inv.setItem(16, createItem(
                config.pvpEnabled ? Material.DIAMOND_SWORD : Material.SHIELD,
                "&6PvP",
                config.pvpEnabled ? "&aPvP Activé (joueurs + mobs)" : "&ePvP Désactivé (mobs uniquement)",
                "",
                "&7Cliquez pour basculer"));

        // Row 2: Modes
        inv.setItem(22, createItem(Material.PAINTING, "&6Mode UI",
                "&7Actuel: &e" + config.uiMode.name(),
                "",
                "&eRICH: &7BossBar + Actionbar",
                "&eCLEAN: &7Chat uniquement",
                "",
                "&aCliquez pour changer"));

        inv.setItem(24, createItem(Material.COMMAND_BLOCK, "&6Gamerules",
                "&7Configurer les règles de jeu",
                "&7(Auto-regen, KeepInv, etc.)",
                "",
                "&aCliquez pour ouvrir"));

        // Row 3: World settings
        inv.setItem(28, createItem(Material.GRASS_BLOCK, "&6Monde de Jeu",
                "&7Actuel: &e" + config.gameWorld));

        inv.setItem(30, createItem(Material.COMPASS, "&6Monde Lobby",
                "&7Actuel: &e" + config.lobbyWorld));

        inv.setItem(32, createItem(Material.RED_BED, "&6Monde Hub",
                "&7Actuel: &e" + plugin.getConfigManager().getHubWorld()));

        inv.setItem(34, createItem(Material.WHEAT_SEEDS, "&6Seeds",
                "&7Total: &e" + config.seeds.size() + " seeds",
                "",
                "\u00a77Les seeds sont configurables",
                "\u00a77dans arenas/" + arenaId + ".yml"));

        // Row 4: Player limits
        inv.setItem(37, createItem(Material.PLAYER_HEAD, "&6Joueurs Min",
                "&7Actuel: &e" + config.minPlayers,
                "",
                "&aClic G: &7+1",
                "&cClic D: &7-1"));

        inv.setItem(39, createItem(Material.PLAYER_HEAD, "&6Joueurs Max",
                "&7Actuel: &e" + config.maxPlayers,
                "",
                "&aClic G: &7+1",
                "&cClic D: &7-1"));

        inv.setItem(41, createItem(Material.GOLDEN_APPLE, "&6Protection Spawn",
                "&7Actuel: &e" + config.spawnProtection + "s",
                "",
                "&aClic G: &7+5s",
                "&cClic D: &7-5s"));

        // Nether/End toggles
        inv.setItem(43, createItem(
                config.netherEnabled ? Material.NETHERRACK : Material.BARRIER,
                "&6Nether",
                config.netherEnabled ? "&aActivé" : "&cDésactivé",
                "",
                "&7Cliquez pour basculer"));

        inv.setItem(44, createItem(
                config.endEnabled ? Material.END_STONE : Material.BARRIER,
                "&6End",
                config.endEnabled ? "&aActivé" : "&cDésactivé",
                "",
                "&7Cliquez pour basculer"));

        // Bottom: Arena ID info + close
        inv.setItem(45, createItem(Material.NAME_TAG, "&eArène: &6" + arenaId));
        inv.setItem(49, createItem(Material.BARRIER, "&cFermer"));

        // Fill empty slots with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
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

        // Find the arena this player is configuring
        String arenaId = getArenaIdFromInventory(event.getView().getTopInventory());
        if (arenaId == null)
            return;
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null)
            return;

        boolean isLeftClick = event.isLeftClick();
        int slot = event.getRawSlot();

        switch (slot) {
            case 10 -> {
                // Swap timer -> open sub-GUI
                player.closeInventory();
                plugin.getSwapTimerGUI().open(player, arenaId);
            }
            case 12 -> {
                // Max game time: +/- 60s
                config.maxGameTime += isLeftClick ? 60 : -60;
                config.maxGameTime = Math.max(60, config.maxGameTime);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId); // Refresh
            }
            case 14 -> {
                // Load time: +/- 10s
                config.loadTime += isLeftClick ? 10 : -10;
                config.loadTime = Math.max(10, config.loadTime);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 16 -> {
                // Toggle PvP
                config.pvpEnabled = !config.pvpEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 22 -> {
                // Toggle UI Mode
                config.uiMode = (config.uiMode == UIMode.RICH) ? UIMode.CLEAN : UIMode.RICH;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 24 -> {
                // Open Gamerules GUI
                player.closeInventory();
                plugin.getGamerulesGUI().open(player, arenaId);
            }
            case 37 -> {
                // Min players: +/- 1
                config.minPlayers += isLeftClick ? 1 : -1;
                config.minPlayers = Math.max(1, config.minPlayers);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 39 -> {
                // Max players: +/- 1
                config.maxPlayers += isLeftClick ? 1 : -1;
                config.maxPlayers = Math.max(config.minPlayers, config.maxPlayers);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 41 -> {
                // Spawn protection: +/- 5s
                config.spawnProtection += isLeftClick ? 5 : -5;
                config.spawnProtection = Math.max(0, config.spawnProtection);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 43 -> {
                // Toggle Nether
                config.netherEnabled = !config.netherEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 44 -> {
                // Toggle End
                config.endEnabled = !config.endEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 49 -> player.closeInventory();
        }
    }

    /**
     * Extract arena ID from the name tag item in slot 45.
     */
    private String getArenaIdFromInventory(Inventory inv) {
        ItemStack nameTag = inv.getItem(45);
        if (nameTag == null || !nameTag.hasItemMeta())
            return "default";

        // Parse from lore or just use default
        // Since we store it in display name, extract it
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

    // =========================================
    // HELPERS
    // =========================================

    static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name).decoration(TextDecoration.ITALIC, false));

        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(colorize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreComponents);
        }

        item.setItemMeta(meta);
        return item;
    }

    static Component colorize(String text) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(text);
    }

    static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return m + "min" + (s > 0 ? " " + s + "s" : "");
    }
}
