package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reusable confirmation GUI for destructive admin actions.
 * Shows a "Are you sure?" screen with Confirm / Cancel buttons.
 */
public class ConfirmationGUI implements Listener {

    // ── Inventory layout ──────────────────────────────────────────────
    private static final int INV_SIZE = 27;
    private static final int SLOT_CANCEL = 11;
    private static final int SLOT_WARNING = 13;
    private static final int SLOT_CONFIRM = 15;

    private final DeathSwapPlugin plugin;

    // Stores pending confirmations: player UUID -> action to run on confirm
    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();

    public ConfirmationGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the confirmation screen for a player.
     *
     * @param player       The admin player
     * @param actionName   Display name of the action (e.g. "Bannir Steve",
     *                     "Régénérer monde")
     * @param description  Short description shown in the GUI
     * @param warningColor Color for the warning icon
     * @param onConfirm    Runnable to execute when confirmed
     * @param onCancel     Runnable to execute when cancelled (e.g. re-open previous
     *                     GUI)
     */
    public void open(Player player, String actionName, String description,
            NamedTextColor warningColor, Runnable onConfirm, Runnable onCancel) {

        // Store the pending action
        pendingActions.put(player.getUniqueId(), new PendingAction(onConfirm, onCancel));

        Component title = Lang.getComponent("gui-confirmation-title");
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);

        // Slot 11: CANCEL (green wool - safe option)
        ItemStack cancel = new ItemStack(Material.GREEN_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(
                Lang.getComponent("gui-confirmation-cancel-name").decoration(TextDecoration.ITALIC, false));
        cancelMeta.lore(List.of(
                Lang.getComponent("gui-confirmation-cancel-lore").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(SLOT_CANCEL, cancel);

        // Slot 13: Warning icon (description of action)
        ItemStack warning = new ItemStack(Material.TNT);
        ItemMeta warningMeta = warning.getItemMeta();
        warningMeta.displayName(
                Component.text(actionName, warningColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        warningMeta.lore(List.of(
                Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Lang.getComponent("gui-confirmation-warning-irreversible").decoration(TextDecoration.ITALIC, false)));
        warning.setItemMeta(warningMeta);
        inv.setItem(SLOT_WARNING, warning);

        // Slot 15: CONFIRM (red wool - danger option)
        ItemStack confirm = new ItemStack(Material.RED_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(
                Lang.getComponent("gui-confirmation-confirm-name").decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(List.of(
                Lang.getComponent("gui-confirmation-confirm-lore-1").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Lang.getComponent("gui-confirmation-confirm-lore-2").decoration(TextDecoration.ITALIC, false)));
        confirm.setItemMeta(confirmMeta);
        inv.setItem(SLOT_CONFIRM, confirm);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        Component expectedTitle = Lang.getComponent("gui-confirmation-title");
        if (!expectedTitle.equals(event.getView().title()))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        PendingAction action = pendingActions.remove(player.getUniqueId());
        if (action == null) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.RED_WOOL) {
            // CONFIRMED
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 0.5f);
            action.onConfirm.run();
        } else if (clicked.getType() == Material.GREEN_WOOL) {
            // CANCELLED
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            action.onCancel.run();
        }
        // Clicking the warning icon (TNT) does nothing — it's informational
    }

    /**
     * Clean up pending action if GUI is closed without clicking.
     */
    public void removePending(UUID uuid) {
        pendingActions.remove(uuid);
    }

    private record PendingAction(Runnable onConfirm, Runnable onCancel) {
    }
}
