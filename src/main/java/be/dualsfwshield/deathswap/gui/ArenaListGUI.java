package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import java.util.Map;

/**
 * Config-level arena browser GUI.
 * Lists ALL arena configs (not just active game instances).
 * Left click: edit settings, Right click: delete with confirmation.
 */
public class ArenaListGUI implements Listener {

    /*
     * ── Inventory layout (dynamic size) ─────────────────────────────
     * Inventory size adapts to arena count (9 per row, min 18 slots).
     *
     * Rows 1..N-1 │ [arena items fill dynamically]
     * Last row │ INFO · · · CREATE · · · CLOSE
     *
     * Offsets are relative to `lastRow = size - 9`.
     */
    private static final int BTN_INFO_OFFSET = 0;
    private static final int BTN_CREATE_OFFSET = 4;
    private static final int BTN_CLOSE_OFFSET = 8;

    private final DeathSwapPlugin plugin;

    public ArenaListGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Map<String, ConfigManager.ArenaConfig> allConfigs = plugin.getConfigManager().getAllArenaConfigs();
        int size = Math.min(54, ((allConfigs.size() / 9) + 2) * 9); // Dynamic size, min 2 rows
        size = Math.max(27, size);

        Component title = Lang.getComponent("gui-list-title");
        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (Map.Entry<String, ConfigManager.ArenaConfig> entry : allConfigs.entrySet()) {
            if (slot >= size - 9)
                break; // Reserve last row for buttons

            String id = entry.getKey();
            ConfigManager.ArenaConfig config = entry.getValue();

            // Check if this arena has an active game instance
            GameInstance game = plugin.getArenaManager().getArena(id);
            boolean isActive = game != null && (game.getState() == GameState.RUNNING
                    || game.getState() == GameState.STARTING);
            boolean isWaiting = game != null && game.getState() == GameState.WAITING;

            Material mat;
            String statusText;
            NamedTextColor statusColor;

            if (isActive) {
                mat = Material.GREEN_CONCRETE;
                statusText = Lang.get("gui-list-status-running");
            } else if (isWaiting) {
                mat = Material.YELLOW_CONCRETE;
                statusText = Lang.get("gui-list-status-waiting");
            } else {
                mat = Material.CYAN_CONCRETE;
                statusText = Lang.get("gui-list-status-inactive");
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(id, NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Lang.getComponent("gui-list-item-status")
                    .append(Lang.colorize(statusText)));
            lore.add(Lang.getComponent("gui-list-item-type")
                    .append(Component.text(config.gameType.name(), NamedTextColor.AQUA)));
            lore.add(Lang.getComponent("gui-list-item-players")
                    .append(Component.text(config.minPlayers + "-" + config.maxPlayers, NamedTextColor.WHITE)));

            if (game != null) {
                lore.add(Lang.getComponent("gui-list-item-online")
                        .append(Component.text(String.valueOf(game.getAllPlayers().size()), NamedTextColor.WHITE)));
            }

            lore.add(Component.empty());
            lore.add(Lang.getComponent("gui-list-item-lobby")
                    .append(Component.text(config.lobbyWorld, NamedTextColor.GRAY)));
            lore.add(Lang.getComponent("gui-list-item-game")
                    .append(Component.text(config.gameWorld, NamedTextColor.GRAY)));
            lore.add(Component.empty());
            lore.add(Lang.getComponent("gui-list-item-click-left")
                    .append(Lang.getComponent("gui-list-item-edit").color(NamedTextColor.GRAY)));
            lore.add(Lang.getComponent("gui-list-item-shift-left")
                    .append(Lang.getComponent("gui-list-item-tp-lobby").color(NamedTextColor.GRAY)));
            lore.add(Lang.getComponent("gui-list-item-click-right")
                    .append(Lang.getComponent("gui-list-item-delete").color(NamedTextColor.GRAY)));

            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // --- Bottom row buttons ---
        int lastRow = size - 9;

        // Create new arena button
        ItemStack create = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.displayName(Lang.getComponent("gui-list-create-name")
                .decoration(TextDecoration.ITALIC, false));
        createMeta.lore(List.of(
                Lang.getComponent("gui-list-create-lore-1").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,
                        false),
                Lang.getComponent("gui-list-create-lore-2").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,
                        false)));
        create.setItemMeta(createMeta);
        inv.setItem(lastRow + BTN_CREATE_OFFSET, create);

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Lang.getComponent("gui-close")
                .decoration(TextDecoration.ITALIC, false));
        close.setItemMeta(closeMeta);
        inv.setItem(lastRow + BTN_CLOSE_OFFSET, close);

        // Arena count info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Lang.getComponent("gui-list-info-name", "%count%", String.valueOf(allConfigs.size()))
                .decoration(TextDecoration.ITALIC, false));
        info.setItemMeta(infoMeta);
        inv.setItem(lastRow + BTN_INFO_OFFSET, info);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Component expectedTitle = Lang.getComponent("gui-list-title");
        if (!expectedTitle.equals(event.getView().title()))
            return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getRawSlot();
        int invSize = event.getInventory().getSize();
        int lastRow = invSize - 9;

        // Close button
        if (clicked.getType() == Material.BARRIER && slot == lastRow + BTN_CLOSE_OFFSET) {
            player.closeInventory();
            return;
        }

        // Create button
        if (clicked.getType() == Material.EMERALD && slot == lastRow + BTN_CREATE_OFFSET) {
            // Generate a unique name
            int counter = 1;
            String name;
            do {
                name = "arena_" + counter++;
            } while (plugin.getConfigManager().getArenaConfig(name) != null);

            plugin.getConfigManager().createArena(name);
            plugin.getArenaManager().reload();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            Lang.send(player, "gui-list-create-success", "%arena%", name);
            plugin.getSettingsGUI().open(player, name);
            return;
        }

        // Arena click — must be in the arena slots (before last row)
        if (slot < lastRow && isConcrete(clicked.getType())) {
            String arenaId = extractArenaId(clicked);
            if (arenaId == null)
                return;

            if (event.getClick() == ClickType.LEFT) {
                // Edit → open ArenaDetailsGUI
                if (plugin.getArenaDetailsGUI() != null) {
                    plugin.getArenaDetailsGUI().open(player, arenaId);
                } else {
                    Lang.send(player, "gui-list-details-unavailable");
                }
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                // TP Lobby
                GameInstance game = plugin.getArenaManager().getArena(arenaId);
                org.bukkit.World world = (game != null) ? game.getLobbyLocation().getWorld()
                        : Bukkit.getWorld(plugin.getConfigManager().getArenaConfig(arenaId).lobbyWorld);
                if (world != null) {
                    player.teleport(world.getSpawnLocation());
                    Lang.send(player, "gui-list-tp-lobby-success", "%arena%", arenaId);
                } else {
                    Lang.send(player, "gui-list-tp-lobby-error");
                }
            } else if (event.getClick() == ClickType.RIGHT) {
                // Delete → open ConfirmationGUI
                player.closeInventory();
                final String finalId = arenaId;
                plugin.getConfirmationGUI().open(player,
                        "Supprimer: " + arenaId,
                        "L'arène et son fichier seront supprimés.",
                        NamedTextColor.RED,
                        () -> {
                            // On confirm
                            boolean success = plugin.getConfigManager().deleteArena(finalId);
                            if (success) {
                                plugin.getArenaManager().reload();
                                Lang.send(player, "cmd-admin-delete-success", "%arena%", finalId);
                            } else {
                                Lang.send(player, "gui-list-delete-error");
                            }
                            open(player); // Re-open list
                        },
                        () -> open(player) // On cancel → re-open list
                );
            }
        }
    }

    private boolean isConcrete(Material mat) {
        return mat == Material.GREEN_CONCRETE
                || mat == Material.YELLOW_CONCRETE
                || mat == Material.CYAN_CONCRETE;
    }

    private String extractArenaId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        Component display = item.getItemMeta().displayName();
        if (display == null)
            return null;

        return PlainTextComponentSerializer
                .plainText().serialize(display).trim();
    }
}
