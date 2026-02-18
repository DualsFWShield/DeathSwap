package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.GameType;
import be.dualsfwshield.deathswap.UIMode;
import be.dualsfwshield.deathswap.util.Lang;
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
import java.util.List;

/**
 * Main settings GUI for configuring an arena (54 slots / 6 rows).
 * Acts as the advanced Admin Configuration for arenas.
 */
public class SettingsGUI implements Listener {

    /*
     * ── Inventory layout (54 slots, 6 rows) ─────────────────────────
     *
     * Row 1 │ TYPE · LOBBY · WORLD · RULES · UI
     * Row 2 │ · SWAP · MAX_T · LOAD · PVP
     * Row 3 │ · · MIN_P · MAX_P · PROT
     * Row 4 │ · NETHER · END · SEEDS
     * Row 5 │ · TP_CMD · RST_CMD· RESIL
     * Row 6 │ INFO · · · STOP · · · · BACK
     */
    private static final int INV_SIZE = 54;

    // Row 1 (0-8): Core settings
    private static final int SLOT_GAME_TYPE = 0;
    private static final int SLOT_LOBBY_WORLD = 2;
    private static final int SLOT_GAME_WORLD = 4;
    private static final int SLOT_GAMERULES = 6;
    private static final int SLOT_UI_MODE = 8;

    // Row 2 (9-17): Timers & limits
    private static final int SLOT_SWAP_TIMER = 10;
    private static final int SLOT_MAX_GAME = 12;
    private static final int SLOT_LOAD_TIME = 14;
    private static final int SLOT_PVP = 16;

    // Row 3 (18-26): Players & limits
    private static final int SLOT_MIN_PLAYERS = 20;
    private static final int SLOT_MAX_PLAYERS = 22;
    private static final int SLOT_SPAWN_PROT = 24;

    // Row 4 (27-35): Dimensions & misc
    private static final int SLOT_NETHER = 28;
    private static final int SLOT_END = 30;
    private static final int SLOT_SEEDS = 32;

    // Row 5 (36-44): Commands & resilience
    private static final int SLOT_TP_CMD = 37;
    private static final int SLOT_RESET_CMD = 39;
    private static final int SLOT_RESILIENCE = 41;

    // Row 6 (45-53): Footer
    private static final int SLOT_ARENA_INFO = 45;
    private static final int SLOT_STOP = 49;
    private static final int SLOT_BACK = 53;

    private final DeathSwapPlugin plugin;

    public SettingsGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the settings GUI for a specific arena.
     */
    public void open(Player player, String arenaId) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            Lang.send(player, "gui-settings-error-arena-not-found", "%arena%", arenaId);
            return;
        }

        Component title = Lang.getComponent("gui-settings-title");
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);

        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        boolean isActive = game != null
                && (game.getState() == GameState.RUNNING || game.getState() == GameState.STARTING);

        // Row 1 (0-8): Core Settings
        inv.setItem(SLOT_GAME_TYPE,
                createItem(Material.matchMaterial("PAINTING") != null ? Material.PAINTING : Material.ITEM_FRAME,
                        Lang.get("gui-settings-gametype-name"),
                        Lang.get("gui-settings-gametype-current", "%type%", config.gameType.name()),
                        "",
                        Lang.get("gui-settings-click-change")));

        inv.setItem(SLOT_LOBBY_WORLD, createItem(Material.COMPASS, Lang.get("gui-settings-lobby-name"),
                Lang.get("gui-settings-lobby-current", "%world%", config.lobbyWorld),
                "",
                Lang.get("gui-settings-click-chat")));

        inv.setItem(SLOT_GAME_WORLD, createItem(Material.GRASS_BLOCK, Lang.get("gui-settings-game-name"),
                Lang.get("gui-settings-game-current", "%world%", config.gameWorld),
                "",
                Lang.get("gui-settings-click-chat")));

        inv.setItem(SLOT_GAMERULES, createItem(Material.COMMAND_BLOCK, Lang.get("gui-settings-gamerules-name"),
                Lang.get("gui-settings-gamerules-lore-1"),
                Lang.get("gui-settings-gamerules-lore-2", "%count%", String.valueOf(config.gamerules.size())),
                "",
                Lang.get("gui-settings-click-open")));

        inv.setItem(SLOT_UI_MODE, createItem(Material.ITEM_FRAME, Lang.get("gui-settings-uimode-name"),
                Lang.get("gui-settings-uimode-current", "%mode%", config.uiMode.name()),
                "",
                Lang.get("gui-settings-uimode-rich"),
                Lang.get("gui-settings-uimode-clean"),
                "",
                Lang.get("gui-settings-click-change")));

        // Row 2 (9-17): Timers & Limits
        inv.setItem(SLOT_SWAP_TIMER, createItem(Material.CLOCK, Lang.get("gui-settings-timer-name"),
                Lang.get("gui-settings-timer-mode", "%mode%", config.swapMode.name()),
                config.swapMode.name().equals("FIXED")
                        ? Lang.get("gui-settings-timer-interval", "%time%", GuiUtils.formatTime(config.swapInterval))
                        : Lang.get("gui-settings-timer-minmax", "%min%", GuiUtils.formatTime(config.swapMin), "%max%",
                                GuiUtils.formatTime(config.swapMax)),
                "",
                Lang.get("gui-settings-click-config")));

        inv.setItem(SLOT_MAX_GAME, createItem(Material.RECOVERY_COMPASS, Lang.get("gui-settings-maxgame-name"),
                Lang.get("gui-settings-maxgame-current", "%time%", GuiUtils.formatTime(config.maxGameTime)),
                "",
                Lang.get("gui-settings-click-add-min"),
                Lang.get("gui-settings-click-sub-min")));

        inv.setItem(SLOT_LOAD_TIME, createItem(Material.HOPPER, Lang.get("gui-settings-loadtime-name"),
                Lang.get("gui-settings-loadtime-current", "%time%", String.valueOf(config.loadTime)),
                "",
                Lang.get("gui-settings-click-add-10s"),
                Lang.get("gui-settings-click-sub-10s")));

        inv.setItem(SLOT_PVP, createItem(
                config.pvpEnabled ? Material.DIAMOND_SWORD : Material.SHIELD,
                Lang.get("gui-settings-pvp-name"),
                config.pvpEnabled ? Lang.get("gui-settings-pvp-enabled") : Lang.get("gui-settings-pvp-disabled"),
                "",
                Lang.get("gui-settings-click-toggle")));

        // Row 3 (18-26): Players & Limits
        inv.setItem(SLOT_MIN_PLAYERS, createItem(Material.PLAYER_HEAD, Lang.get("gui-settings-minplayers-name"),
                Lang.get("gui-settings-minplayers-current", "%count%", String.valueOf(config.minPlayers)),
                "",
                Lang.get("gui-settings-click-add-1"),
                Lang.get("gui-settings-click-sub-1")));

        inv.setItem(SLOT_MAX_PLAYERS, createItem(Material.PLAYER_HEAD, Lang.get("gui-settings-maxplayers-name"),
                Lang.get("gui-settings-maxplayers-current", "%count%", String.valueOf(config.maxPlayers)),
                "",
                Lang.get("gui-settings-click-add-1"),
                Lang.get("gui-settings-click-sub-1")));

        inv.setItem(SLOT_SPAWN_PROT, createItem(Material.GOLDEN_APPLE, Lang.get("gui-settings-spawnprot-name"),
                Lang.get("gui-settings-spawnprot-current", "%time%", String.valueOf(config.spawnProtection)),
                "",
                Lang.get("gui-settings-click-add-5s"),
                Lang.get("gui-settings-click-sub-5s")));

        // Row 4 (27-35): Dimensions & Misc
        inv.setItem(SLOT_NETHER, createItem(
                config.netherEnabled ? Material.NETHERRACK : Material.BARRIER,
                Lang.get("gui-settings-nether-name"),
                config.netherEnabled ? Lang.get("gui-settings-nether-enabled")
                        : Lang.get("gui-settings-nether-disabled"),
                "",
                Lang.get("gui-settings-click-toggle")));

        inv.setItem(SLOT_END, createItem(
                config.endEnabled ? Material.END_STONE : Material.BARRIER,
                Lang.get("gui-settings-end-name"),
                config.endEnabled ? Lang.get("gui-settings-end-enabled") : Lang.get("gui-settings-end-disabled"),
                "",
                Lang.get("gui-settings-click-toggle")));

        inv.setItem(SLOT_SEEDS, createItem(Material.WHEAT_SEEDS, Lang.get("gui-settings-seeds-name"),
                Lang.get("gui-settings-seeds-total", "%count%", String.valueOf(config.seeds.size())),
                "",
                Lang.get("gui-settings-seeds-lore-1"),
                Lang.get("gui-settings-seeds-lore-2", "%arena%", arenaId)));

        // Row 5 (36-44): Commands & Resilience
        String tpCmdPreview = config.teleportCommand != null && !config.teleportCommand.isEmpty()
                ? (config.teleportCommand.length() > 30 ? config.teleportCommand.substring(0, 27) + "..."
                        : config.teleportCommand)
                : Lang.get("gui-settings-tp-default");

        inv.setItem(SLOT_TP_CMD, createItem(Material.ENDER_PEARL, Lang.get("gui-settings-tp-name"),
                Lang.get("gui-settings-tp-current", "%cmd%", tpCmdPreview),
                "",
                Lang.get("gui-settings-tp-click-set"),
                Lang.get("gui-settings-tp-click-reset")));

        String resetCmd = (config.worldResetCommands != null && !config.worldResetCommands.isEmpty())
                ? Lang.get("gui-settings-reset-active", "%count%", String.valueOf(config.worldResetCommands.size()))
                : Lang.get("gui-settings-reset-inactive");

        inv.setItem(SLOT_RESET_CMD, createItem(Material.TNT, Lang.get("gui-settings-reset-name"),
                config.worldResetCommands != null && !config.worldResetCommands.isEmpty()
                        ? Lang.get("gui-settings-reset-active", "%count%",
                                String.valueOf(config.worldResetCommands.size()))
                        : Lang.get("gui-settings-reset-inactive"),
                "",
                Lang.get("gui-settings-reset-click-cycle"),
                Lang.get("gui-settings-reset-click-custom")));

        inv.setItem(SLOT_RESILIENCE, createItem(Material.TOTEM_OF_UNDYING, Lang.get("gui-settings-resilience-name"),
                Lang.get("gui-settings-resilience-start", "%bool%", String.valueOf(config.startIfMinPlayersMet)),
                Lang.get("gui-settings-resilience-cancel", "%bool%",
                        String.valueOf(config.preventCancelAfterCountdown)),
                "",
                Lang.get("gui-settings-resilience-click")));

        // Row 6 (45-53): Footer
        inv.setItem(SLOT_ARENA_INFO,
                createItem(Material.NAME_TAG, Lang.get("gui-settings-arena-info", "%arena%", arenaId)));

        if (isActive) {
            inv.setItem(SLOT_STOP, createItem(Material.BARRIER, Lang.get("gui-settings-stop-name"),
                    Lang.get("gui-settings-stop-lore"),
                    "",
                    Lang.get("gui-settings-stop-click")));
        }

        inv.setItem(SLOT_BACK,
                createItem(Material.ARROW, Lang.get("gui-settings-back-name"), Lang.get("gui-settings-back-lore")));

        // Fill empty slots with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INV_SIZE; i++) {
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
        Component expectedTitle = Lang.getComponent("gui-settings-title");
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
            case SLOT_GAME_TYPE -> { // Game Type
                GameType[] types = GameType.values();
                int ordinal = config.gameType.ordinal();
                config.gameType = types[(ordinal + 1) % types.length];
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_LOBBY_WORLD -> { // Lobby World
                plugin.getChatInputListener().requestInput(player,
                        Lang.get("gui-settings-input-lobby"),
                        (input) -> {
                            config.lobbyWorld = input;
                            plugin.getConfigManager().saveArena(config);
                            Bukkit.getScheduler().runTask(plugin, () -> open(player, arenaId));
                        });
            }
            case SLOT_GAME_WORLD -> { // Game World
                plugin.getChatInputListener().requestInput(player,
                        Lang.get("gui-settings-input-game"),
                        (input) -> {
                            config.gameWorld = input;
                            plugin.getConfigManager().saveArena(config);
                            Bukkit.getScheduler().runTask(plugin, () -> open(player, arenaId));
                        });
            }
            case SLOT_GAMERULES -> { // Gamerules
                player.closeInventory();
                plugin.getGamerulesGUI().open(player, arenaId);
            }
            case SLOT_UI_MODE -> { // UI Mode
                config.uiMode = (config.uiMode == UIMode.RICH) ? UIMode.CLEAN : UIMode.RICH;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_SWAP_TIMER -> { // Swap Timer
                player.closeInventory();
                plugin.getSwapTimerGUI().open(player, arenaId);
            }
            case SLOT_MAX_GAME -> { // Max game time
                config.maxGameTime += isLeftClick ? 60 : -60;
                config.maxGameTime = Math.max(60, config.maxGameTime);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_LOAD_TIME -> { // Load time
                config.loadTime += isLeftClick ? 10 : -10;
                config.loadTime = Math.max(10, config.loadTime);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_PVP -> { // PvP
                config.pvpEnabled = !config.pvpEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_MIN_PLAYERS -> { // Min players
                config.minPlayers += isLeftClick ? 1 : -1;
                config.minPlayers = Math.max(1, config.minPlayers);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_MAX_PLAYERS -> { // Max players
                config.maxPlayers += isLeftClick ? 1 : -1;
                config.maxPlayers = Math.max(config.minPlayers, config.maxPlayers);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_SPAWN_PROT -> { // Spawn protection
                config.spawnProtection += isLeftClick ? 5 : -5;
                config.spawnProtection = Math.max(0, config.spawnProtection);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_NETHER -> { // Nether
                config.netherEnabled = !config.netherEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_END -> { // End
                config.endEnabled = !config.endEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_TP_CMD -> { // Teleport Cmd
                if (isLeftClick) {
                    plugin.getChatInputListener().requestInput(player,
                            Lang.get("gui-settings-input-tp", "%placeholders%",
                                    "%player%, %world%, %x%, %y%, %z%, %yaw%, %pitch%"),
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
            case SLOT_RESET_CMD -> { // Reset Cmd
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
                        Lang.send(player, "gui-settings-reset-set-cwr");
                    } else if (nextType == 2) { // MV
                        config.worldResetCommands.add("mv regen %world% -s %seed%");
                        config.worldResetCommands.add("mv confirm"); // just in case
                        Lang.send(player, "gui-settings-reset-set-mv");
                    } else { // None
                        Lang.send(player, "gui-settings-reset-set-none");
                    }
                    plugin.getConfigManager().saveArena(config);
                    open(player, arenaId);
                } else if (isRightClick) {
                    // Custom Input
                    plugin.getChatInputListener().requestInput(player,
                            Lang.get("gui-settings-input-reset", "%world%", "%world%",
                                    "%seed%", "%seed%"),
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
            case SLOT_RESILIENCE -> { // Resilience
                boolean value = !config.startIfMinPlayersMet;
                config.startIfMinPlayersMet = value;
                config.preventCancelAfterCountdown = value;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_STOP -> { // Stop Arena
                GameInstance game = plugin.getArenaManager().getArena(arenaId);
                if (game != null) {
                    game.stopGame();
                    Lang.send(player, "gui-settings-stop-success");
                    open(player, arenaId);
                }
            }
            case SLOT_BACK -> {
                if (plugin.getArenaDetailsGUI() != null) {
                    plugin.getArenaDetailsGUI().open(player, arenaId);
                } else {
                    player.closeInventory();
                }
            }
        }
    }

    private String getArenaIdFromInventory(Inventory inv) {
        return GuiUtils.getArenaIdFromInventory(inv, 45);
    }

    static ItemStack createItem(Material material, String name, String... lore) {
        return GuiUtils.createItem(material, name, lore);
    }

    static Component colorize(String text) {
        return GuiUtils.colorize(text);
    }

    static String formatTime(int seconds) {
        return GuiUtils.formatTime(seconds);
    }
}
