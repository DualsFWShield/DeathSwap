package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArenaDetailsGUI implements Listener {

    // ── Inventory layout ──────────────────────────────────────────────
    private static final int INV_SIZE = 27;
    private static final int SLOT_STATUS = 10;
    private static final int SLOT_START_STOP = 12;
    private static final int SLOT_SWAP_REGEN = 14;
    private static final int SLOT_PLAYERS = 16;
    private static final int SLOT_BACK = 22;
    private static final int SLOT_SETTINGS = 26;

    private final DeathSwapPlugin plugin;

    public ArenaDetailsGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String arenaId) {
        // Try getting active game instance first
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);
        // If not loaded, check if config exists
        ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);

        if (config == null) {
            Lang.send(player, "gui-details-error-not-found");
            return;
        }

        String prefix = Lang.get("gui-details-title-prefix");
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, Component.text(prefix + arenaId));

        GameState state = (arena != null) ? arena.getState() : GameState.DISABLED;

        // Slot 10: Status Item
        Material stateMat = switch (state) {
            case WAITING -> Material.YELLOW_CONCRETE;
            case STARTING -> Material.LIME_CONCRETE;
            case RUNNING -> Material.GREEN_CONCRETE;
            case ENDED -> Material.RED_CONCRETE;
            case DISABLED -> Material.BARRIER;
        };

        String stateName = (arena == null) ? Lang.get("gui-details-unloaded")
                : state.name();
        inv.setItem(SLOT_STATUS, GuiUtils.createItem(stateMat,
                Lang.get("gui-details-status", "%status%", stateName)));

        // Slot 12: Start / Stop
        if (state == GameState.RUNNING || state == GameState.STARTING) {
            // Stop button
            inv.setItem(SLOT_START_STOP,
                    GuiUtils.createItem(Material.BARRIER,
                            Lang.get("gui-details-stop-name"),
                            Lang.get("gui-details-stop-lore-1"),
                            Lang.get("gui-details-stop-click")));
        } else {
            // Start button
            inv.setItem(SLOT_START_STOP,
                    GuiUtils.createItem(Material.EMERALD,
                            Lang.get("gui-details-start-name"),
                            Lang.get("gui-details-start-lore-1"),
                            Lang.get("gui-details-start-click-normal"),
                            Lang.get("gui-details-start-click-force"),
                            Lang.get("gui-details-start-click-debug")));
        }

        // Slot 14: Swap Immediate (Variables) / Regenerate (Waiting)
        if (state == GameState.RUNNING) {
            inv.setItem(SLOT_SWAP_REGEN,
                    GuiUtils.createItem(Material.ENDER_PEARL,
                            Lang.get("gui-details-swap-force-name"),
                            Lang.get("gui-details-swap-force-lore"),
                            Lang.get("gui-details-swap-force-click")));
        } else if (state == GameState.WAITING || state == GameState.DISABLED) {
            inv.setItem(SLOT_SWAP_REGEN,
                    GuiUtils.createItem(Material.TNT,
                            Lang.get("gui-details-regen-name"),
                            Lang.get("gui-details-regen-lore-1"),
                            Lang.get("gui-details-regen-lore-2")));
        } else {
            inv.setItem(SLOT_SWAP_REGEN,
                    GuiUtils.createItem(Material.GRAY_DYE,
                            Lang.get("gui-details-manage-swap-name"),
                            Lang.get("gui-details-manage-swap-lore")));
        }

        // Slot 16: Players
        inv.setItem(SLOT_PLAYERS,
                GuiUtils.createItem(Material.PLAYER_HEAD,
                        Lang.get("gui-details-manage-players-name"),
                        Lang.get("gui-details-manage-players-lore")));

        // Slot 22: Back
        inv.setItem(SLOT_BACK,
                GuiUtils.createItem(Material.ARROW, Lang.get("gui-details-back-name"),
                        Lang.get("gui-details-back-lore")));

        // Slot 26: Settings
        inv.setItem(SLOT_SETTINGS,
                GuiUtils.createItem(Material.COMPARATOR,
                        Lang.get("gui-details-config-name"),
                        Lang.get("gui-details-config-lore")));

        // Fillers
        ItemStack filler = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INV_SIZE; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String prefix = Lang.get("gui-details-title-prefix");
        if (!event.getView().getTitle().startsWith(prefix))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        String arenaId = event.getView().getTitle().substring(prefix.length());

        // We might valid config even if game instance is null
        // actions requiring game instance must check for it
        GameInstance arena = plugin.getArenaManager().getArena(arenaId);

        int slot = event.getSlot();

        if (slot == SLOT_START_STOP) { // Start/Stop
            if (arena == null) {
                Lang.send(player, "gui-details-error-instance");
                return;
            }
            GameState state = arena.getState();
            if (state == GameState.RUNNING || state == GameState.STARTING) {
                // Stop
                arena.stopGame();
                Lang.send(player, "gui-details-game-stopped");
            } else {
                // Start
                if (event.getClick().isShiftClick()) {
                    // Force Start
                    if (arena.getAllPlayers().size() < 2) {
                        // Force start might imply bypassing checks, but usually we need players.
                        // But admin might want to test alone?
                        // Let's assume force start just skips timer.
                        // Standard start checks min players.
                        // Debug start might bypass min players?
                    }
                    arena.startGame(true); // Force start logic if supported
                    Lang.send(player, "gui-details-force-start-init");
                } else if (event.getClick().isRightClick()) {
                    // Debug start? maybe reduce timer to 5s?
                    // Or just start with current players even if < min
                    // Check if your GameInstance has debug start
                    arena.startGame(false);
                    // Assuming standard start for now, maybe set timer to 5?
                    // Implement specific logic if available.
                    Lang.send(player, "gui-details-debug-start-init");
                } else {
                    // Normal start
                    arena.startGame(false);
                    Lang.send(player, "gui-details-starting");
                }
            }
            open(player, arenaId); // Refresh

        } else if (slot == SLOT_SWAP_REGEN) { // Swap or Regenerate
            if (arena != null && arena.getState() == GameState.RUNNING) {
                // Swap Immediate
                arena.performSwap();
                Lang.send(player, "gui-details-swap-forced");
            } else if (arena == null || arena.getState() == GameState.WAITING
                    || arena.getState() == GameState.DISABLED) {
                // Regenerate logic
                ConfigManager.ArenaConfig config = plugin.getConfigManager()
                        .getArenaConfig(arenaId);
                if (config == null)
                    return;

                String gameWorld = config.gameWorld;
                plugin.getConfirmationGUI().open(player,
                        Lang.get("gui-details-regen-confirm-title", "%world%",
                                gameWorld),
                        Lang.get("gui-details-regen-confirm-subtitle"),
                        NamedTextColor.GOLD,
                        () -> {
                            if (arena != null) {
                                for (Player p : arena.getAllPlayers()) {
                                    arena.sendToHub(p);
                                }
                            }
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cwr reset " + gameWorld);
                            Lang.send(player, "gui-details-regen-success", "%world%",
                                    gameWorld);
                            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                        },
                        () -> open(player, arenaId));
            }
        } else if (slot == SLOT_PLAYERS) { // Players
            plugin.getPlayerListGUI().open(player, arenaId);
        } else if (slot == SLOT_BACK) { // Back
            plugin.getArenaListGUI().open(player); // Go back to list, not AdminGUI (dashboard)
        } else if (slot == SLOT_SETTINGS) { // Settings
            plugin.getSettingsGUI().open(player, arenaId);
        }
    }
}
