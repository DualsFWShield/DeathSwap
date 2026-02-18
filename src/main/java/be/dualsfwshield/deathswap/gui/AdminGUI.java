package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.Collection;
import java.util.Arrays;

public class AdminGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private final DeathSwapPlugin plugin;
    // Removed static TITLE, using key instead

    public AdminGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Component title = be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-title");
        Inventory inv = Bukkit.createInventory(null, 54, title);

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
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-status", "%status%", "").append(Component.text(state.name(), getColorForState(state))));
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-players", "%current%", String.valueOf(connected), "%max%", String.valueOf(max)));
            lore.add(Component.empty());
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-lobby", "%world%", arena.getConfig().lobbyWorld).color(NamedTextColor.DARK_GRAY));
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-game", "%world%", arena.getConfig().gameWorld).color(NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-click-details"));
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-click-tp"));
            lore.add(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-click-config"));

            meta.lore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        // Global Actions (Bottom Row)
        // Slot 49: Reload
        ItemStack reload = new ItemStack(Material.NETHER_STAR);
        ItemMeta reloadMeta = reload.getItemMeta();
        reloadMeta.displayName(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-reload-name").decoration(TextDecoration.ITALIC, false));
        reload.setItemMeta(reloadMeta);
        inv.setItem(49, reload);

        // Slot 53: Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-close-name").decoration(TextDecoration.ITALIC, false));
        close.setItemMeta(closeMeta);
        inv.setItem(53, close);

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

    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(name));
        if (lore.length > 0) {
            meta.lore(Arrays.stream(lore)
                    .map(l -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(l))
                    .toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        Component expectedTitle = be.dualsfwshield.deathswap.util.Lang.getComponent("gui-admin-title");
        if (!expectedTitle.equals(event.getView().title()))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (clicked.getType() == Material.BARRIER && event.getSlot() == 53) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.NETHER_STAR && event.getSlot() == 49) {
            plugin.reloadConfig();
            plugin.getConfigManager().load(); // Reload arenas? This might be dangerous if games running.
            // ArenaManager.reload() handles active games safely (stops them).
            plugin.getArenaManager().reload();
            be.dualsfwshield.deathswap.util.Lang.send(player, "gui-admin-reloaded");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            open(player); // Refresh
            return;
        }

        // Arena Click?
        if (event.getSlot() < plugin.getArenaManager().getAllArenas().size()) {
            // Re-fetch logic or assume slot maps to iteration order?
            // Safer to use Item Name as ID.
            // Using logic:
            String arenaId = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(clicked.getItemMeta().displayName());
            // Need to strip decorators/colors? PlainText serializer usually does good job.
            // But title was BOLD GOLD. Plain text might just be ID.

            GameInstance arena = plugin.getArenaManager().getArena(arenaId);
            if (arena == null) {
                // Try strip?
                // Or just loop and find matching name
                for (String id : plugin.getArenaManager().getArenaIds()) {
                    if (clicked.getItemMeta().getDisplayName().contains(id)) { // Legacy fallback check
                        arena = plugin.getArenaManager().getArena(id);
                        break;
                    }
                }
            }

            if (arena != null) {
                if (event.getClick() == ClickType.LEFT) {
                    // Open Details GUI (To be implemented)
                    if (plugin.getArenaDetailsGUI() != null) {
                        plugin.getArenaDetailsGUI().open(player, arena.getArenaId());
                    } else {
                        be.dualsfwshield.deathswap.util.Lang.send(player, "gui-admin-details-soon");
                    }
                } else if (event.getClick() == ClickType.RIGHT) {
                    // TP to lobby
                    player.teleport(arena.getLobbyLocation());
                    be.dualsfwshield.deathswap.util.Lang.send(player, "gui-admin-tp-success", "%arena%", arena.getArenaId());
                } else if (event.getClick() == ClickType.MIDDLE) {
                    // Config
                    plugin.getSettingsGUI().open(player, arena.getArenaId());
                }
            }
        }
    }
}
