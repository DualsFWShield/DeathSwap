package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
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
import java.util.Map;

/**
 * Config-level arena browser GUI.
 * Lists ALL arena configs (not just active game instances).
 * Left click: edit settings, Right click: delete with confirmation.
 */
public class ArenaListGUI implements Listener {

    private final DeathSwapPlugin plugin;
    private static final Component TITLE = Component.text("📋 Arènes", NamedTextColor.DARK_AQUA, TextDecoration.BOLD);

    public ArenaListGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Map<String, ConfigManager.ArenaConfig> allConfigs = plugin.getConfigManager().getAllArenaConfigs();
        int size = Math.min(54, ((allConfigs.size() / 9) + 2) * 9); // Dynamic size, min 2 rows
        size = Math.max(27, size);

        Inventory inv = Bukkit.createInventory(null, size, TITLE);

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
                statusText = "▶ En cours";
                statusColor = NamedTextColor.GREEN;
            } else if (isWaiting) {
                mat = Material.YELLOW_CONCRETE;
                statusText = "⏳ En attente";
                statusColor = NamedTextColor.YELLOW;
            } else {
                mat = Material.CYAN_CONCRETE;
                statusText = "⏸ Inactive";
                statusColor = NamedTextColor.GRAY;
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(id, NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Statut: ", NamedTextColor.GRAY)
                    .append(Component.text(statusText, statusColor)));
            lore.add(Component.text("Type: ", NamedTextColor.GRAY)
                    .append(Component.text(config.gameType.name(), NamedTextColor.AQUA)));
            lore.add(Component.text("Joueurs: ", NamedTextColor.GRAY)
                    .append(Component.text(config.minPlayers + "-" + config.maxPlayers, NamedTextColor.WHITE)));

            if (game != null) {
                lore.add(Component.text("Connectés: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(game.getAllPlayers().size()), NamedTextColor.WHITE)));
            }

            lore.add(Component.empty());
            lore.add(Component.text("Lobby: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(config.lobbyWorld, NamedTextColor.GRAY)));
            lore.add(Component.text("Game: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(config.gameWorld, NamedTextColor.GRAY)));
            lore.add(Component.empty());
            lore.add(Component.text("Clic G: ", NamedTextColor.GREEN)
                    .append(Component.text("Éditer", NamedTextColor.GRAY)));
            lore.add(Component.text("Shift-Clic G: ", NamedTextColor.AQUA)
                    .append(Component.text("TP Lobby", NamedTextColor.GRAY)));
            lore.add(Component.text("Clic D: ", NamedTextColor.RED)
                    .append(Component.text("Supprimer", NamedTextColor.GRAY)));
            lore.add(Component.text("Clic D: ", NamedTextColor.RED)
                    .append(Component.text("Supprimer", NamedTextColor.GRAY)));

            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // --- Bottom row buttons ---
        int lastRow = size - 9;

        // Create new arena button
        ItemStack create = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.displayName(Component.text("✚ Créer une arène", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        createMeta.lore(List.of(
                Component.text("Crée une nouvelle arène", NamedTextColor.GRAY),
                Component.text("avec des paramètres par défaut.", NamedTextColor.GRAY)));
        create.setItemMeta(createMeta);
        inv.setItem(lastRow + 4, create);

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Fermer", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        close.setItemMeta(closeMeta);
        inv.setItem(lastRow + 8, close);

        // Arena count info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("ℹ " + allConfigs.size() + " arène(s)", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        info.setItemMeta(infoMeta);
        inv.setItem(lastRow, info);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!TITLE.equals(event.getView().title()))
            return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getRawSlot();
        int invSize = event.getInventory().getSize();
        int lastRow = invSize - 9;

        // Close button
        if (clicked.getType() == Material.BARRIER && slot == lastRow + 8) {
            player.closeInventory();
            return;
        }

        // Create button
        if (clicked.getType() == Material.EMERALD && slot == lastRow + 4) {
            // Generate a unique name
            int counter = 1;
            String name;
            do {
                name = "arena_" + counter++;
            } while (plugin.getConfigManager().getArenaConfig(name) != null);

            plugin.getConfigManager().createArena(name);
            plugin.getArenaManager().reload();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            player.sendMessage(Component.text("✔ Arène '" + name + "' créée !", NamedTextColor.GREEN));
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
                    player.sendMessage(Component.text("Menu détails indisponible.", NamedTextColor.RED));
                }
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                // TP Lobby
                GameInstance game = plugin.getArenaManager().getArena(arenaId);
                org.bukkit.World world = (game != null) ? game.getLobbyLocation().getWorld()
                        : Bukkit.getWorld(plugin.getConfigManager().getArenaConfig(arenaId).lobbyWorld);
                if (world != null) {
                    player.teleport(world.getSpawnLocation());
                    player.sendMessage(Component.text("Téléportation au lobby de " + arenaId, NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Monde lobby introuvable.", NamedTextColor.RED));
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
                                player.sendMessage(Component.text("✔ Arène '" + finalId + "' supprimée.",
                                        NamedTextColor.GREEN));
                            } else {
                                player.sendMessage(Component.text("Erreur: arène introuvable.", NamedTextColor.RED));
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

        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(display).trim();
    }
}
