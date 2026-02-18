package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameType;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.UIMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main settings GUI for configuring an arena (54 slots / 6 rows).
 * Acts as the advanced Admin Configuration for arenas.
 */
public class SettingsGUI implements Listener {

    private final DeathSwapPlugin plugin;
    // Removed static TITLE, using key instead

    public SettingsGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the settings GUI for a specific arena.
     */
    public void open(Player player, String arenaId) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "gui-settings-error-arena-not-found", "%arena%", arenaId);
            return;
        }

        Component title = be.dualsfwshield.deathswap.util.Lang.getComponent("gui-settings-title");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        boolean isActive = game != null
                && (game.getState() == GameState.RUNNING || game.getState() == GameState.STARTING);

        // Row 1 (0-8): Core Settings
        inv.setItem(0,
                createItem(Material.matchMaterial("PAINTING") != null ? Material.PAINTING : Material.ITEM_FRAME,
                        be.dualsfwshield.deathswap.util.Lang.get("gui-settings-gametype-name"),
                        be.dualsfwshield.deathswap.util.Lang.get("gui-settings-gametype-current", "%type%", config.gameType.name()),
                        "",
                        be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-change")));

        inv.setItem(2, createItem(Material.COMPASS, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-lobby-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-lobby-current", "%world%", config.lobbyWorld),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-chat")));

        inv.setItem(4, createItem(Material.GRASS_BLOCK, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-game-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-game-current", "%world%", config.gameWorld),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-chat")));

        inv.setItem(6, createItem(Material.COMMAND_BLOCK, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-gamerules-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-gamerules-lore-1"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-gamerules-lore-2", "%count%", String.valueOf(config.gamerules.size())),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-open")));

        inv.setItem(8, createItem(Material.ITEM_FRAME, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-uimode-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-uimode-current", "%mode%", config.uiMode.name()),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-uimode-rich"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-uimode-clean"),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-change")));

        // Row 2 (9-17): Timers & Limits
        inv.setItem(10, createItem(Material.CLOCK, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-timer-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-timer-mode", "%mode%", config.swapMode.name()),
                config.swapMode.name().equals("FIXED")
                        ? be.dualsfwshield.deathswap.util.Lang.get("gui-settings-timer-interval", "%time%", formatTime(config.swapInterval))
                        : be.dualsfwshield.deathswap.util.Lang.get("gui-settings-timer-minmax", "%min%", formatTime(config.swapMin), "%max%", formatTime(config.swapMax)),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-config")));

        inv.setItem(12, createItem(Material.RECOVERY_COMPASS, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-maxgame-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-maxgame-current", "%time%", formatTime(config.maxGameTime)),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-add-min"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-sub-min")));

        inv.setItem(14, createItem(Material.HOPPER, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-loadtime-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-loadtime-current", "%time%", String.valueOf(config.loadTime)),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-add-10s"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-sub-10s")));

        inv.setItem(16, createItem(
                config.pvpEnabled ? Material.DIAMOND_SWORD : Material.SHIELD,
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-pvp-name"),
                config.pvpEnabled ? be.dualsfwshield.deathswap.util.Lang.get("gui-settings-pvp-enabled") : be.dualsfwshield.deathswap.util.Lang.get("gui-settings-pvp-disabled"),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-toggle")));

        // Row 3 (18-26): Players & Limits
        inv.setItem(20, createItem(Material.PLAYER_HEAD, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-minplayers-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-minplayers-current", "%count%", String.valueOf(config.minPlayers)),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-add-1"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-sub-1")));

        inv.setItem(22, createItem(Material.PLAYER_HEAD, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-maxplayers-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-maxplayers-current", "%count%", String.valueOf(config.maxPlayers)),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-add-1"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-sub-1")));

        inv.setItem(24, createItem(Material.GOLDEN_APPLE, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-spawnprot-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-spawnprot-current", "%time%", String.valueOf(config.spawnProtection)),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-add-5s"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-sub-5s")));

        // Row 4 (27-35): Dimensions & Misc
        inv.setItem(28, createItem(
                config.netherEnabled ? Material.NETHERRACK : Material.BARRIER,
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-nether-name"),
                config.netherEnabled ? be.dualsfwshield.deathswap.util.Lang.get("gui-settings-nether-enabled") : be.dualsfwshield.deathswap.util.Lang.get("gui-settings-nether-disabled"),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-toggle")));

        inv.setItem(30, createItem(
                config.endEnabled ? Material.END_STONE : Material.BARRIER,
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-end-name"),
                config.endEnabled ? be.dualsfwshield.deathswap.util.Lang.get("gui-settings-end-enabled") : be.dualsfwshield.deathswap.util.Lang.get("gui-settings-end-disabled"),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-click-toggle")));

        inv.setItem(32, createItem(Material.WHEAT_SEEDS, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-seeds-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-seeds-total", "%count%", String.valueOf(config.seeds.size())),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-seeds-lore-1"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-seeds-lore-2", "%arena%", arenaId)));

        // Row 5 (36-44): Commands & Resilience
        String tpCmdPreview = config.teleportCommand != null && !config.teleportCommand.isEmpty()
                ? (config.teleportCommand.length() > 30 ? config.teleportCommand.substring(0, 27) + "..."
                        : config.teleportCommand)
                : be.dualsfwshield.deathswap.util.Lang.get("gui-settings-tp-default");

        inv.setItem(37, createItem(Material.ENDER_PEARL, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-tp-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-tp-current", "%cmd%", tpCmdPreview),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-tp-click-set"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-tp-click-reset")));

        String resetCmd = (config.worldResetCommands != null && !config.worldResetCommands.isEmpty())
                ? be.dualsfwshield.deathswap.util.Lang.get("gui-settings-reset-active", "%count%", String.valueOf(config.worldResetCommands.size()))
                : be.dualsfwshield.deathswap.util.Lang.get("gui-settings-reset-inactive");

        inv.setItem(39, createItem(Material.TNT, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-reset-name"),
                config.worldResetCommands != null && !config.worldResetCommands.isEmpty() ? be.dualsfwshield.deathswap.util.Lang.get("gui-settings-reset-active", "%count%", String.valueOf(config.worldResetCommands.size()))
                    : be.dualsfwshield.deathswap.util.Lang.get("gui-settings-reset-inactive"),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-reset-click-cycle"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-reset-click-custom")));

        inv.setItem(41, createItem(Material.TOTEM_OF_UNDYING, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-resilience-name"),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-resilience-start", "%bool%", String.valueOf(config.startIfMinPlayersMet)),
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-resilience-cancel", "%bool%", String.valueOf(config.preventCancelAfterCountdown)),
                "",
                be.dualsfwshield.deathswap.util.Lang.get("gui-settings-resilience-click")));

        // Row 6 (45-53): Footer
        inv.setItem(45, createItem(Material.NAME_TAG, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-arena-info", "%arena%", arenaId)));

        if (isActive) {
            inv.setItem(49, createItem(Material.BARRIER, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-stop-name"),
                    be.dualsfwshield.deathswap.util.Lang.get("gui-settings-stop-lore"),
                    "",
                    be.dualsfwshield.deathswap.util.Lang.get("gui-settings-stop-click")));
        }

        inv.setItem(53, createItem(Material.ARROW, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-back-name"), be.dualsfwshield.deathswap.util.Lang.get("gui-settings-back-lore")));

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
        Component expectedTitle = be.dualsfwshield.deathswap.util.Lang.getComponent("gui-settings-title");
        if (!expectedTitle.equals(event.getView().title()))
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
        boolean isRightClick = event.isRightClick();
        int slot = event.getRawSlot();

        switch (slot) {
            case 0 -> { // Game Type
                GameType[] types = GameType.values();
                int ordinal = config.gameType.ordinal();
                config.gameType = types[(ordinal + 1) % types.length];
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 2 -> { // Lobby World
                plugin.getChatInputListener().requestInput(player, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-input-lobby"),
                        (input) -> {
                            config.lobbyWorld = input;
                            plugin.getConfigManager().saveArena(config);
                            Bukkit.getScheduler().runTask(plugin, () -> open(player, arenaId));
                        });
            }
            case 4 -> { // Game World
                plugin.getChatInputListener().requestInput(player, be.dualsfwshield.deathswap.util.Lang.get("gui-settings-input-game"),
                        (input) -> {
                            config.gameWorld = input;
                            plugin.getConfigManager().saveArena(config);
                            Bukkit.getScheduler().runTask(plugin, () -> open(player, arenaId));
                        });
            }
            case 6 -> { // Gamerules
                player.closeInventory();
                plugin.getGamerulesGUI().open(player, arenaId);
            }
            case 8 -> { // UI Mode
                config.uiMode = (config.uiMode == UIMode.RICH) ? UIMode.CLEAN : UIMode.RICH;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 10 -> { // Swap Timer
                player.closeInventory();
                plugin.getSwapTimerGUI().open(player, arenaId);
            }
            case 12 -> { // Max game time
                config.maxGameTime += isLeftClick ? 60 : -60;
                config.maxGameTime = Math.max(60, config.maxGameTime);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 14 -> { // Load time
                config.loadTime += isLeftClick ? 10 : -10;
                config.loadTime = Math.max(10, config.loadTime);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 16 -> { // PvP
                config.pvpEnabled = !config.pvpEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 20 -> { // Min players
                config.minPlayers += isLeftClick ? 1 : -1;
                config.minPlayers = Math.max(1, config.minPlayers);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 22 -> { // Max players
                config.maxPlayers += isLeftClick ? 1 : -1;
                config.maxPlayers = Math.max(config.minPlayers, config.maxPlayers);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 24 -> { // Spawn protection
                config.spawnProtection += isLeftClick ? 5 : -5;
                config.spawnProtection = Math.max(0, config.spawnProtection);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 28 -> { // Nether
                config.netherEnabled = !config.netherEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 30 -> { // End
                config.endEnabled = !config.endEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 37 -> { // Teleport Cmd
                if (isLeftClick) {
                    plugin.getChatInputListener().requestInput(player,
                            be.dualsfwshield.deathswap.util.Lang.get("gui-settings-input-tp", "%placeholders%", "%player%, %world%, %x%, %y%, %z%, %yaw%, %pitch%"),
                            (input) -> {
                                config.teleportCommand = input;
                                plugin.getConfigManager().saveArena(config);
                                Bukkit.getScheduler().runTask(plugin, () -> open(player, arenaId));
                            });
                } else if (isRightClick) {
                    config.teleportCommand = null; // Revert to global default
                    plugin.getConfigManager().saveArena(config);
                    open(player, arenaId);
                }
            }
            case 39 -> { // Reset Cmd
                if (isLeftClick) {
                    // Cycle Presets
                    // Current logic: Check first command to guess type, or check is empty
                    // Type 0: None (Empty)
                    // Type 1: CWR (starts with cwr)
                    // Type 2: MV (starts with mv)
                    int currentType = 0;
                    if (config.worldResetCommands != null && !config.worldResetCommands.isEmpty()) {
                        String first = config.worldResetCommands.get(0).toLowerCase();
                        if (first.startsWith("cwr"))
                            currentType = 1;
                        else if (first.startsWith("mv"))
                            currentType = 2;
                        else
                            currentType = 3; // Custom
                    }

                    int nextType = (currentType + 1) % 3; // Cycle 0->1->2->0 (Skip custom in cycle)

                    config.worldResetCommands = new ArrayList<>();
                    if (nextType == 1) { // CWR
                        config.worldResetCommands.add("cwr edit %world% setSeed %seed%");
                        config.worldResetCommands.add("cwr reset %world%");
                        be.dualsfwshield.deathswap.util.Lang.send(player, "gui-settings-reset-set-cwr");
                    } else if (nextType == 2) { // MV
                        config.worldResetCommands.add("mv regen %world% -s %seed%");
                        config.worldResetCommands.add("mv confirm"); // just in case
                        be.dualsfwshield.deathswap.util.Lang.send(player, "gui-settings-reset-set-mv");
                    } else { // None
                        be.dualsfwshield.deathswap.util.Lang.send(player, "gui-settings-reset-set-none");
                    }
                    plugin.getConfigManager().saveArena(config);
                    open(player, arenaId);
                } else if (isRightClick) {
                    // Custom Input
                    plugin.getChatInputListener().requestInput(player,
                            be.dualsfwshield.deathswap.util.Lang.get("gui-settings-input-reset", "%world%", "%world%", "%seed%", "%seed%"),
                            (input) -> {
                                config.worldResetCommands = new ArrayList<>();
                                if (!input.equalsIgnoreCase("none") && !input.isEmpty()) {
                                    String[] cmds = input.split(";");
                                    for (String cmd : cmds) {
                                        config.worldResetCommands.add(cmd.trim());
                                    }
                                }
                                plugin.getConfigManager().saveArena(config);
                                Bukkit.getScheduler().runTask(plugin, () -> open(player, arenaId));
                            });
                }
            }
            case 41 -> { // Resilience
                boolean value = !config.startIfMinPlayersMet;
                config.startIfMinPlayersMet = value;
                config.preventCancelAfterCountdown = value;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case 49 -> { // Stop Arena
                GameInstance game = plugin.getArenaManager().getArena(arenaId);
                if (game != null) {
                    game.stopGame();
                    be.dualsfwshield.deathswap.util.Lang.send(player, "gui-settings-stop-success");
                    open(player, arenaId);
                }
            }
            case 53 -> {
                 if (plugin.getArenaDetailsGUI() != null) {
                     plugin.getArenaDetailsGUI().open(player, arenaId);
                 } else {
                     player.closeInventory();
                 }
            }
        }
    }

    private String getArenaIdFromInventory(Inventory inv) {
        ItemStack nameTag = inv.getItem(45);
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
