package be.dualsfwshield.deathswap.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared utility methods for all GUI classes.
 * Centralizes item creation, text colorization, time formatting,
 * and arena ID extraction to avoid code duplication across GUIs.
 */
public final class GuiUtils {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private GuiUtils() {
        // Utility class — no instantiation
    }

    /**
     * Create a GUI item with a colorized name and optional lore lines.
     * Automatically disables italic on both name and lore.
     *
     * @param material the item material
     * @param name     the display name (supports {@code &} color codes)
     * @param lore     optional lore lines (supports {@code &} color codes)
     * @return the configured ItemStack
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
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

    /**
     * Convert a string with legacy {@code &} color codes to an Adventure Component.
     *
     * @param text the text with color codes
     * @return the deserialized Component
     */
    public static Component colorize(String text) {
        return LEGACY.deserialize(text);
    }

    /**
     * Format a duration in seconds to a human-readable string (e.g. "5min 30s").
     *
     * @param seconds the duration in seconds
     * @return formatted time string
     */
    public static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return m + "min" + (s > 0 ? " " + s + "s" : "");
    }

    /**
     * Extract the arena ID from a name tag item stored at a specific slot.
     * Parses the plain text display name looking for ": " separator.
     *
     * @param inv  the inventory to read from
     * @param slot the slot index containing the name tag
     * @return the extracted arena ID, or "default" if not found
     */
    public static String getArenaIdFromInventory(Inventory inv, int slot) {
        ItemStack nameTag = inv.getItem(slot);
        if (nameTag == null || !nameTag.hasItemMeta())
            return "default";

        Component display = nameTag.getItemMeta().displayName();
        if (display == null)
            return "default";

        String plain = PlainTextComponentSerializer.plainText().serialize(display);
        if (plain.contains(": ")) {
            return plain.substring(plain.indexOf(": ") + 2).trim();
        }
        return "default";
    }
}
