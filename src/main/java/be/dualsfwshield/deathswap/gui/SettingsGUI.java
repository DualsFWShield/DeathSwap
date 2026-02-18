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
    private static final Component TITLE = Component.text("⚙ Admin Config", NamedTextColor.GOLD, TextDecoration.BOLD);

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

        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        boolean isActive = game != null
                && (game.getState() == GameState.RUNNING || game.getState() == GameState.STARTING);

        // Row 1 (0-8): Core Settings
        inv.setItem(0,
                createItem(Material.matchMaterial("PAINTING") != null ? Material.PAINTING : Material.ITEM_FRAME,
                        "&6Game Type",
                        "&7Actuel: &e" + config.gameType.name(),
                        "",
                        "&aCliquez pour changer"));

        inv.setItem(2, createItem(Material.COMPASS, "&6Lobby World",
                "&7Actuel: &e" + config.lobbyWorld,
                "",
                "&aClic G: &7Changer (Chat)"));

        inv.setItem(4, createItem(Material.GRASS_BLOCK, "&6Game World",
                "&7Actuel: &e" + config.gameWorld,
                "",
                "&aClic G: &7Changer (Chat)"));

        inv.setItem(6, createItem(Material.COMMAND_BLOCK, "&6Gamerules",
                "&7Configurer les règles de jeu",
                "&7(" + config.gamerules.size() + " règles définies)",
                "",
                "&aCliquez pour ouvrir"));

        inv.setItem(8, createItem(Material.ITEM_FRAME, "&6Mode UI",
                "&7Actuel: &e" + config.uiMode.name(),
                "",
                "&eRICH: &7BossBar + Actionbar",
                "&eCLEAN: &7Chat uniquement",
                "",
                "&aCliquez pour changer"));

        // Row 2 (9-17): Timers & Limits
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
                config.pvpEnabled ? "&aPvP Activé" : "&ePvP Désactivé",
                "",
                "&7Cliquez pour basculer"));

        // Row 3 (18-26): Players & Limits
        inv.setItem(20, createItem(Material.PLAYER_HEAD, "&6Joueurs Min",
                "&7Actuel: &e" + config.minPlayers,
                "",
                "&aClic G: &7+1",
                "&cClic D: &7-1"));

        inv.setItem(22, createItem(Material.PLAYER_HEAD, "&6Joueurs Max",
                "&7Actuel: &e" + config.maxPlayers,
                "",
                "&aClic G: &7+1",
                "&cClic D: &7-1"));

        inv.setItem(24, createItem(Material.GOLDEN_APPLE, "&6Protection Spawn",
                "&7Actuel: &e" + config.spawnProtection + "s",
                "",
                "&aClic G: &7+5s",
                "&cClic D: &7-5s"));

        // Row 4 (27-35): Dimensions & Misc
        inv.setItem(28, createItem(
                config.netherEnabled ? Material.NETHERRACK : Material.BARRIER,
                "&6Nether",
                config.netherEnabled ? "&aActivé" : "&cDésactivé",
                "",
                "&7Cliquez pour basculer"));

        inv.setItem(30, createItem(
                config.endEnabled ? Material.END_STONE : Material.BARRIER,
                "&6End",
                config.endEnabled ? "&aActivé" : "&cDésactivé",
                "",
                "&7Cliquez pour basculer"));

        inv.setItem(32, createItem(Material.WHEAT_SEEDS, "&6Seeds",
                "&7Total: &e" + config.seeds.size() + " seeds",
                "",
                "\u00a77Les seeds sont configurables",
                "\u00a77dans arenas/" + arenaId + ".yml"));

        // Row 5 (36-44): Commands & Resilience
        String tpCmdPreview = config.teleportCommand != null && !config.teleportCommand.isEmpty()
                ? (config.teleportCommand.length() > 30 ? config.teleportCommand.substring(0, 27) + "..."
                        : config.teleportCommand)
                : "&7Défaut (Global)";

        inv.setItem(37, createItem(Material.ENDER_PEARL, "&6Commande Teleport",
                "&7Actuel: &e" + tpCmdPreview,
                "",
                "&aClic G: &7Définir (Chat)",
                "&cClic D: &7Reset (Global)"));

        String resetCmd = (config.worldResetCommands != null && !config.worldResetCommands.isEmpty())
                ? "&eActivé (" + config.worldResetCommands.size() + " cmds)"
                : "&7Désactivé (None)";

        inv.setItem(39, createItem(Material.TNT, "&6Commande Reset",
                "&7Actuel: &e" + resetCmd,
                "",
                "&aClic G: &7Cycle (None -> CWR -> MV)",
                "&cClic D: &7Custom (Chat)"));

        inv.setItem(41, createItem(Material.TOTEM_OF_UNDYING, "&6Resilience",
                "&7Start si Min Players: &e" + config.startIfMinPlayersMet,
                "&7Bloquer Cancel Unready: &e" + config.preventCancelAfterCountdown,
                "",
                "&aCliquez pour basculer (ON/OFF)"));

        // Row 6 (45-53): Footer
        inv.setItem(45, createItem(Material.NAME_TAG, "&eArène: &6" + arenaId));

        if (isActive) {
            inv.setItem(49, createItem(Material.BARRIER, "&cArrêter l'Arène",
                    "&7Force Stop la partie en cours.",
                    "",
                    "&Click G pour arrêter !"));
        }

        inv.setItem(53, createItem(Material.ARROW, "&eRetour", "&7Vers le menu de l'arène"));

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
                plugin.getChatInputListener().requestInput(player, "Entrez le nom du monde Lobby (ou 'cancel'):",
                        (input) -> {
                            config.lobbyWorld = input;
                            plugin.getConfigManager().saveArena(config);
                            Bukkit.getScheduler().runTask(plugin, () -> open(player, arenaId));
                        });
            }
            case 4 -> { // Game World
                plugin.getChatInputListener().requestInput(player, "Entrez le nom du monde de Jeu (ou 'cancel'):",
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
                            "Entrez la commande de TP (Placeholders: %player%, %world%, %x%, %y%, %z%, %yaw%, %pitch%):",
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
                        player.sendMessage(Component.text("Reset défini sur CyberWorldReset.", NamedTextColor.GREEN));
                    } else if (nextType == 2) { // MV
                        config.worldResetCommands.add("mv regen %world% -s %seed%");
                        config.worldResetCommands.add("mv confirm"); // just in case
                        player.sendMessage(
                                Component.text("Reset défini sur Multiverse (Regen).", NamedTextColor.GREEN));
                    } else { // None
                        player.sendMessage(Component.text("Reset désactivé (None).", NamedTextColor.YELLOW));
                    }
                    plugin.getConfigManager().saveArena(config);
                    open(player, arenaId);
                } else if (isRightClick) {
                    // Custom Input
                    plugin.getChatInputListener().requestInput(player,
                            "Entrez les commandes de reset séparées par ';' (Placeholders: %world%, %seed%):",
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
                    player.sendMessage(Component.text("Arène arrêtée.", NamedTextColor.RED));
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
