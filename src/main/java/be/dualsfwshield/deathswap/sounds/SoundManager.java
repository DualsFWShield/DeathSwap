package be.dualsfwshield.deathswap.sounds;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Plays configurable sounds for game events.
 * Sound types and parameters are loaded from config.yml.
 */
public class SoundManager {

    private final DeathSwapPlugin plugin;

    public SoundManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Play a configured sound event to a single player.
     *
     * @param eventName config key (e.g. "swap", "death", "win")
     * @param player    target player
     */
    public void playSound(String eventName, Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled())
            return;

        Map<String, ConfigManager.SoundConfig> sounds = plugin.getConfigManager().getSounds();
        ConfigManager.SoundConfig sc = sounds.get(eventName);
        if (sc == null)
            return;

        try {
            Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(sc.type().toLowerCase()));
            if (sound == null)
                throw new IllegalArgumentException("Sound not found");
            player.playSound(player.getLocation(), sound, sc.volume(), sc.pitch());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type: " + sc.type() + " for event: " + eventName);
        }
    }

    /**
     * Play a configured sound event to all players in a collection.
     *
     * @param eventName config key
     * @param players   target players
     */
    public void playSoundAll(String eventName, Collection<Player> players) {
        if (!plugin.getConfigManager().isSoundsEnabled())
            return;

        Map<String, ConfigManager.SoundConfig> sounds = plugin.getConfigManager().getSounds();
        ConfigManager.SoundConfig sc = sounds.get(eventName);
        if (sc == null)
            return;

        try {
            Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(sc.type().toLowerCase()));
            if (sound == null)
                throw new IllegalArgumentException("Sound not found");
            for (Player player : players) {
                player.playSound(player.getLocation(), sound, sc.volume(), sc.pitch());
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type: " + sc.type() + " for event: " + eventName);
        }
    }
}
