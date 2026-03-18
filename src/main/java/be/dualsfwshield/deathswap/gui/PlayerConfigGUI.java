package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Limited configuration panel for regular players in the lobby.
 * Allows modifying a subset of arena settings without admin permissions.
 */
public class PlayerConfigGUI implements Listener {

    private static final int INV_SIZE = 27; // 3 rows

    // Row 1: Core gameplay
    private static final int SLOT_SWAP_TIMER = 2;
    private static final int SLOT_PVP = 4;
    private static final int SLOT_SPAWN_PROT = 6;

    // Row 2: World & Teams
    private static final int SLOT_SPAWN_RADIUS = 10;
    private static final int SLOT_BLINDNESS = 12;
    private static final int SLOT_TEAMS_TOGGLE = 14;
    private static final int SLOT_TEAM_SIZE = 16;

    // Row 2 continued: Dimensions
    private static final int SLOT_NETHER = 19;
    private static final int SLOT_END = 21;

    // Row 3: Navigation
    private static final int SLOT_BACK = 26;

    private final DeathSwapPlugin plugin;

    public PlayerConfigGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the player config GUI.
     */
    public void open(Player player, String arenaId) {
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            Lang.send(player, "gui-settings-error-arena-not-found", "%arena%", arenaId);
            return;
        }

        Component title = Lang.getComponent("gui-playerconfig-title");
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);

        // Swap Timer
        String timerInfo = config.swapMode.name().equals("FIXED")
                ? GuiUtils.formatTime(config.swapInterval)
                : GuiUtils.formatTime(config.swapMin) + " - " + GuiUtils.formatTime(config.swapMax);
        inv.setItem(SLOT_SWAP_TIMER, GuiUtils.createItem(Material.CLOCK,
                Lang.get("gui-playerconfig-swap-name"),
                Lang.get("gui-playerconfig-swap-current", "%time%", timerInfo),
                "",
                Lang.get("gui-playerconfig-click-add-30s"),
                Lang.get("gui-playerconfig-click-sub-30s")));

        // PvP
        inv.setItem(SLOT_PVP, GuiUtils.createItem(
                config.pvpEnabled ? Material.DIAMOND_SWORD : Material.SHIELD,
                Lang.get("gui-playerconfig-pvp-name"),
                config.pvpEnabled ? Lang.get("gui-settings-pvp-enabled") : Lang.get("gui-settings-pvp-disabled"),
                "",
                Lang.get("gui-settings-click-toggle")));

        // Spawn Protection
        inv.setItem(SLOT_SPAWN_PROT, GuiUtils.createItem(Material.GOLDEN_APPLE,
                Lang.get("gui-playerconfig-spawnprot-name"),
                Lang.get("gui-settings-spawnprot-current", "%time%", String.valueOf(config.spawnProtection)),
                "",
                Lang.get("gui-settings-click-add-5s"),
                Lang.get("gui-settings-click-sub-5s")));

        // Spawn Radius
        inv.setItem(SLOT_SPAWN_RADIUS, GuiUtils.createItem(Material.ENDER_EYE,
                Lang.get("gui-playerconfig-spawnradius-name"),
                Lang.get("gui-settings-spawnradius-current", "%radius%", String.valueOf(config.spawnRadius)),
                "",
                Lang.get("gui-settings-click-add-50"),
                Lang.get("gui-settings-click-sub-50")));

        // Blindness Duration
        inv.setItem(SLOT_BLINDNESS, GuiUtils.createItem(Material.INK_SAC,
                Lang.get("gui-playerconfig-blindness-name"),
                Lang.get("gui-playerconfig-blindness-current", "%time%", String.valueOf(config.swapBlindnessDuration)),
                "",
                Lang.get("gui-playerconfig-click-add-1s"),
                Lang.get("gui-playerconfig-click-sub-1s")));

        // Teams Toggle
        inv.setItem(SLOT_TEAMS_TOGGLE, GuiUtils.createItem(
                config.teamsEnabled ? Material.LIME_WOOL : Material.RED_WOOL,
                Lang.get("gui-playerconfig-teams-name"),
                config.teamsEnabled ? Lang.get("enabled") : Lang.get("disabled"),
                "",
                Lang.get("gui-settings-click-toggle")));

        // Team Size
        String sizeStr = config.teamSize <= 0 ? Lang.get("gui-playerconfig-teams-dynamic") : String.valueOf(config.teamSize);
        inv.setItem(SLOT_TEAM_SIZE, GuiUtils.createItem(Material.PLAYER_HEAD,
                Lang.get("gui-playerconfig-teamsize-name"),
                Lang.get("gui-playerconfig-teamsize-current", "%size%", sizeStr),
                "",
                Lang.get("gui-settings-click-add-1"),
                Lang.get("gui-settings-click-sub-1")));

        // Nether Toggle
        inv.setItem(SLOT_NETHER, GuiUtils.createItem(
                config.netherEnabled ? Material.NETHERRACK : Material.BARRIER,
                Lang.get("gui-settings-nether-name"),
                config.netherEnabled ? Lang.get("gui-settings-nether-enabled") : Lang.get("gui-settings-nether-disabled"),
                "",
                Lang.get("gui-settings-click-toggle")));

        // End Toggle
        inv.setItem(SLOT_END, GuiUtils.createItem(
                config.endEnabled ? Material.END_STONE : Material.BARRIER,
                Lang.get("gui-settings-end-name"),
                config.endEnabled ? Lang.get("gui-settings-end-enabled") : Lang.get("gui-settings-end-disabled"),
                "",
                Lang.get("gui-settings-click-toggle")));

        // Arena info tag (hidden, for arena ID extraction)
        inv.setItem(25, GuiUtils.createItem(Material.NAME_TAG,
                Lang.get("gui-settings-arena-info", "%arena%", arenaId)));

        // Back
        inv.setItem(SLOT_BACK, GuiUtils.createItem(Material.ARROW,
                Lang.get("gui-playerconfig-back")));

        // Fill empty slots
        ItemStack filler = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INV_SIZE; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title() == null) return;

        Component expectedTitle = Lang.getComponent("gui-playerconfig-title");
        if (!expectedTitle.equals(event.getView().title())) return;

        event.setCancelled(true);

        // Find arena from inventory
        String arenaId = GuiUtils.getArenaIdFromInventory(event.getView().getTopInventory(), 25);
        if (arenaId == null) return;
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) return;

        // Block changes during game
        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        if (game != null && (game.getState() == GameState.RUNNING || game.getState() == GameState.STARTING)) {
            Lang.send(player, "cmd-settings-in-game");
            return;
        }

        boolean isLeftClick = event.isLeftClick();
        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_SWAP_TIMER -> {
                if (config.swapMode.name().equals("FIXED")) {
                    config.swapInterval += isLeftClick ? 30 : -30;
                    config.swapInterval = Math.max(30, config.swapInterval);
                } else {
                    config.swapMin += isLeftClick ? 30 : -30;
                    config.swapMax += isLeftClick ? 30 : -30;
                    config.swapMin = Math.max(30, config.swapMin);
                    config.swapMax = Math.max(config.swapMin + 30, config.swapMax);
                }
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_PVP -> {
                config.pvpEnabled = !config.pvpEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_SPAWN_PROT -> {
                config.spawnProtection += isLeftClick ? 5 : -5;
                config.spawnProtection = Math.max(0, config.spawnProtection);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_SPAWN_RADIUS -> {
                config.spawnRadius += isLeftClick ? 50 : -50;
                config.spawnRadius = Math.max(50, config.spawnRadius);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_BLINDNESS -> {
                config.swapBlindnessDuration += isLeftClick ? 1 : -1;
                config.swapBlindnessDuration = Math.max(0, config.swapBlindnessDuration);
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_TEAMS_TOGGLE -> {
                config.teamsEnabled = !config.teamsEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_TEAM_SIZE -> {
                config.teamSize += isLeftClick ? 1 : -1;
                config.teamSize = Math.max(0, config.teamSize); // 0 = dynamic
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_NETHER -> {
                config.netherEnabled = !config.netherEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_END -> {
                config.endEnabled = !config.endEnabled;
                plugin.getConfigManager().saveArena(config);
                open(player, arenaId);
            }
            case SLOT_BACK -> player.closeInventory();
        }
    }
}
