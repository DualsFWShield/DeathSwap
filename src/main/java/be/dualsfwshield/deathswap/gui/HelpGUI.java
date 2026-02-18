package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

public class HelpGUI implements Listener {

    // ── Inventory layout ──────────────────────────────────────────────
    private static final int INV_SIZE = 27;
    // Row 1 – Player commands
    private static final int SLOT_JOIN = 10;
    private static final int SLOT_LEAVE = 11;
    private static final int SLOT_STATS = 12;
    private static final int SLOT_TOP = 13;
    private static final int SLOT_VOTE = 14;
    // Row 2 – Admin commands
    private static final int SLOT_ADMIN_DASH = 19;
    private static final int SLOT_ADMIN_SET = 20;
    private static final int SLOT_ADMIN_START = 21;
    private static final int SLOT_ADMIN_STOP = 22;
    private static final int SLOT_ADMIN_SWAP = 23;
    private static final int SLOT_ADMIN_RLD = 24;
    private static final int SLOT_ADMIN_CMDS = 25;
    // Footer
    private static final int SLOT_CLOSE = 26;

    private final DeathSwapPlugin plugin;

    public HelpGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Component title = Lang.getComponent("gui-help-title");
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);

        // Row 1: Player Commands
        inv.setItem(SLOT_JOIN, createItem(Material.EMERALD, Lang.get("gui-help-join-name"), "/ds join [arena]",
                Lang.get("gui-help-join-desc")));
        inv.setItem(SLOT_LEAVE, createItem(Material.RED_BED, Lang.get("gui-help-leave-name"), "/ds leave",
                Lang.get("gui-help-leave-desc")));
        inv.setItem(SLOT_STATS, createItem(Material.PAPER, Lang.get("gui-help-stats-name"), "/ds stats [joueur]",
                Lang.get("gui-help-stats-desc")));
        inv.setItem(SLOT_TOP, createItem(Material.GOLD_INGOT, Lang.get("gui-help-top-name"), "/ds top [stat]",
                Lang.get("gui-help-top-desc")));
        inv.setItem(SLOT_VOTE, createItem(Material.HOPPER, Lang.get("gui-help-vote-name"), "/ds vote [num]",
                Lang.get("gui-help-vote-desc")));

        // Row 2: Admin Commands (Only if permission)
        if (player.hasPermission("deathswap.admin")) {
            inv.setItem(SLOT_ADMIN_DASH, createItem(Material.COMMAND_BLOCK, Lang.get("gui-help-admin-dashboard-name"),
                    "/ds admin list", Lang.get("gui-help-admin-dashboard-desc")));
            inv.setItem(SLOT_ADMIN_SET,
                    createItem(Material.COMPARATOR, Lang.get("gui-help-settings-name"), "/ds settings",
                            Lang.get("gui-help-settings-desc")));
            inv.setItem(SLOT_ADMIN_START,
                    createItem(Material.LIME_WOOL, Lang.get("gui-help-start-name"), "/ds start [debug]",
                            Lang.get("gui-help-start-desc")));
            inv.setItem(SLOT_ADMIN_STOP,
                    createItem(Material.RED_WOOL, Lang.get("gui-help-stop-name"), "/ds stop [arena]",
                            Lang.get("gui-help-stop-desc")));
            inv.setItem(SLOT_ADMIN_SWAP, createItem(Material.ENDER_PEARL, Lang.get("gui-help-swap-name"), "/ds swapnow",
                    Lang.get("gui-help-swap-desc")));
            inv.setItem(SLOT_ADMIN_RLD, createItem(Material.REPEATER, Lang.get("gui-help-reload-name"), "/ds reload",
                    Lang.get("gui-help-reload-desc")));
            inv.setItem(SLOT_ADMIN_CMDS,
                    createItem(Material.BOOK, Lang.get("gui-help-commands-name"), "/ds help commands",
                            Lang.get("gui-help-commands-desc")));
        }

        // Close Button
        inv.setItem(SLOT_CLOSE,
                createItem(Material.BARRIER, Lang.get("gui-help-close-name"), "", Lang.get("gui-help-close-desc")));

        // Fill empty slots with glass pane
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", "", "");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String usage, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            if (!usage.isEmpty()) {
                lore.add(
                        Lang.getComponent("gui-help-usage", "%usage%", usage).decoration(TextDecoration.ITALIC, false));
            }
            if (!description.isEmpty()) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(description)
                        .colorIfAbsent(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Lang.getComponent("gui-help-click-suggest").decoration(TextDecoration.ITALIC, true));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Suggest helper
    private void suggestCommand(Player player, String command) {
        player.closeInventory();
        // Use Bukkit dispatch if it's a direct command without args needed, or suggest
        // if args needed.
        // The requirement was "suggests the command in the chat".
        // Adventure/Spigot doesn't have a direct "open chat with text" method for
        // server->client easily without sending a clickable message.
        // BUT, we can just send a clickable message in chat that puts it in the input.
        // OR better: Since this is a GUI, clicking usually executes. But the user asked
        // "suggests the command".
        // Let's print a clickable message in chat.

        player.sendMessage(Lang.getComponent("gui-help-click-chat")
                .append(Component.text(command, NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(command))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                                .showText(Lang.getComponent("gui-help-click-insert")))));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component expectedTitle = Lang.getComponent("gui-help-title");
        if (!expectedTitle.equals(event.getView().title()))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE || item.getType() == Material.AIR)
            return;

        if (item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Logic to extract command from lore or known slots.
        // Since we constructed it, we know slots.
        String command = "";
        int slot = event.getSlot();

        switch (slot) {
            // Player
            case SLOT_JOIN -> command = "/ds join";
            case SLOT_LEAVE -> command = "/ds leave";
            case SLOT_STATS -> command = "/ds stats";
            case SLOT_TOP -> command = "/ds top";
            case SLOT_VOTE -> command = "/ds vote";

            // Admin
            case SLOT_ADMIN_DASH -> command = "/ds admin list";
            case SLOT_ADMIN_SET -> command = "/ds settings";
            case SLOT_ADMIN_START -> command = "/ds start";
            case SLOT_ADMIN_STOP -> command = "/ds stop";
            case SLOT_ADMIN_SWAP -> command = "/ds swapnow";
            case SLOT_ADMIN_RLD -> command = "/ds reload";
            case SLOT_ADMIN_CMDS -> command = "/ds help commands";
        }

        if (!command.isEmpty()) {
            // Basic "Quick Execute" for simple commands?
            // User said "suggests the command".
            suggestCommand(player, command);
        }
    }
}
