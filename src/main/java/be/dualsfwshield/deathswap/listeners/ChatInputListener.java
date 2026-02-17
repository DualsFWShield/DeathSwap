package be.dualsfwshield.deathswap.listeners;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Listens for chat input from players for configuration purposes.
 */
public class ChatInputListener implements Listener {

    private final DeathSwapPlugin plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new HashMap<>();

    public ChatInputListener(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Request input from a player.
     * 
     * @param player   The player to request input from.
     * @param prompt   The prompt message to send.
     * @param callback The callback to execute when input is received.
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
        player.closeInventory();
        player.sendMessage(Component.text("--------------------------------------------------", NamedTextColor.GRAY));
        player.sendMessage(Component.text(prompt, NamedTextColor.GREEN));
        player.sendMessage(Component.text("Tapez 'cancel' pour annuler.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("--------------------------------------------------", NamedTextColor.GRAY));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!pendingInputs.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Opération annulée.", NamedTextColor.RED));
            return;
        }

        // Run callback appropriately (sync if needed, though most config ops are thread
        // safe or simple)
        // Better to run sync just in case
        new BukkitRunnable() {
            @Override
            public void run() {
                callback.accept(input);
            }
        }.runTask(plugin);
    }
}
