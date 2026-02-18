package be.dualsfwshield.deathswap.gui;

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
import java.util.Collection;
import java.util.List;

public class AdminGUI implements Listener {

    /*
     * ── Inventory layout (54 slots, 6 rows) ─────────────────────────
     *
     * Rows 1-5 │ [arena items fill dynamically]
     * Row 6 │ · · · · RELOAD · · · CLOSE
     */
    private static final int INV_SIZE = 54;
    private static final int SLOT_RELOAD = 49;
    private static final int SLOT_CLOSE = 53;

    private final DeathSwapPlugin plugin;

    public AdminGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, Lang.getComponent("gui-admin-title"));

        int slot = 0;
        Collection<GameInstance> arenas = plugin.getArenaManager().getAllArenas();

        for (GameInstance arena : arenas) {
            String id = arena.getArenaId();
            GameState state = arena.getState();
            int connected = arena.getAllPlayers().size();
            int max = arena.getConfig().maxPlayers;

            Material mat = switch (state) {
                case WAITING -> Material.YELLOW_CONCRETE;
                case STARTING -> Material.LIME_CONCRETE;
                case RUNNING -> Material.GREEN_CONCRETE;
                case ENDED -> Material.RED_CONCRETE;
                case DISABLED -> Material.BARRIER;
            };

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(id, NamedTextColor.GOLD, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Lang.getComponent("gui-admin-status", "%status%", "")
                    .append(Component.text(state.name(), getColorForState(state))));
            lore.add(Lang.getComponent("gui-admin-players", "%current%",
                    String.valueOf(connected), "%max%", String.valueOf(max)));
            lore.add(Component.empty());
            lore.add(Lang.getComponent("gui-admin-lobby", "%world%", arena.getConfig().lobbyWorld)
                    .color(NamedTextColor.DARK_GRAY));
            lore.add(Lang.getComponent("gui-admin-game", "%world%", arena.getConfig().gameWorld)
                    .color(NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(Lang.getComponent("gui-admin-click-details"));
            lore.add(Lang.getComponent("gui-admin-click-tp"));
            lore.add(Lang.getComponent("gui-admin-click-config"));

            meta.lore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        // Global Actions (Bottom Row)
        // Slot 49: Reload
        ItemStack reload = new ItemStack(Material.NETHER_STAR);
        ItemMeta reloadMeta = reload.getItemMeta();
        reloadMeta.displayName(Lang.getComponent("gui-admin-reload-name")
                .decoration(TextDecoration.ITALIC, false));
        reload.setItemMeta(reloadMeta);
        inv.setItem(SLOT_RELOAD, reload);

        // Slot 53: Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Lang.getComponent("gui-admin-close-name")
                .decoration(TextDecoration.ITALIC, false));
        close.setItemMeta(closeMeta);
        inv.setItem(SLOT_CLOSE, close);

        player.openInventory(inv);
    }

    private NamedTextColor getColorForState(GameState state) {
        return switch (state) {
            case WAITING -> NamedTextColor.YELLOW;
            case STARTING -> NamedTextColor.GREEN;
            case RUNNING -> NamedTextColor.DARK_GREEN;
            case ENDED -> NamedTextColor.RED;
            case DISABLED -> NamedTextColor.DARK_RED;
        };
    }

    /**
     * @deprecated Use {@link GuiUtils#createItem(Material, String, String...)}
     *             instead.
     */
    @Deprecated
    public static ItemStack createItem(Material material, String name, String... lore) {
        return GuiUtils.createItem(material, name, lore);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        Component expectedTitle = Lang.getComponent("gui-admin-title");
        if (!expectedTitle.equals(event.getView().title()))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (clicked.getType() == Material.BARRIER && event.getSlot() == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.NETHER_STAR && event.getSlot() == SLOT_RELOAD) {
            plugin.reloadConfig();
            plugin.getConfigManager().load(); // Reload arenas? This might be dangerous if games running.
            // ArenaManager.reload() handles active games safely (stops them).
            plugin.getArenaManager().reload();
            Lang.send(player, "gui-admin-reloaded");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            open(player); // Refresh
            return;
        }

        if (event.getSlot() < plugin.getArenaManager().getAllArenas().size()) {
            String arenaId = PlainTextComponentSerializer.plainText()
                    .serialize(clicked.getItemMeta().displayName());

            GameInstance arena = plugin.getArenaManager().getArena(arenaId);
            if (arena == null) {
                for (String id : plugin.getArenaManager().getArenaIds()) {
                    if (clicked.getItemMeta().getDisplayName().contains(id)) {
                        arena = plugin.getArenaManager().getArena(id);
                        break;
                    }
                }
            }

            if (arena != null) {
                if (event.getClick() == ClickType.LEFT) {
                    if (plugin.getArenaDetailsGUI() != null) {
                        plugin.getArenaDetailsGUI().open(player, arena.getArenaId());
                    } else {
                        Lang.send(player, "gui-admin-details-soon");
                    }
                } else if (event.getClick() == ClickType.RIGHT) {
                    player.teleport(arena.getLobbyLocation());
                    Lang.send(player, "gui-admin-tp-success", "%arena%",
                            arena.getArenaId());
                } else if (event.getClick() == ClickType.MIDDLE) {
                    // Config
                    plugin.getSettingsGUI().open(player, arena.getArenaId());
                }
            }
        }
    }
}
