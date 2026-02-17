package be.dualsfwshield.deathswap.stats;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Formats and displays leaderboard data to players.
 */
public class LeaderboardManager {

    private final DeathSwapPlugin plugin;

    public LeaderboardManager(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Send a formatted top-N leaderboard to the sender.
     *
     * @param sender the command sender
     * @param stat   stat type (kills, wins, deaths, time, games)
     * @param limit  max entries to show
     */
    public void sendLeaderboard(CommandSender sender, String stat, int limit) {
        StatsManager sm = plugin.getStatsManager();
        if (sm == null) {
            sender.sendMessage(Component.text("Les stats sont désactivées.", NamedTextColor.RED));
            return;
        }

        List<PlayerStats> top = sm.getTopPlayers(stat, limit);

        String statDisplay = switch (stat.toLowerCase()) {
            case "kills" -> "Kills";
            case "wins" -> "Victoires";
            case "deaths" -> "Morts";
            case "time", "survival_time" -> "Temps de survie";
            case "games", "games_played" -> "Parties jouées";
            default -> stat;
        };

        sender.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                TextDecoration.STRIKETHROUGH));
        sender.sendMessage(Component.text(" ★ TOP " + limit + " — " + statDisplay + " ★", NamedTextColor.GOLD,
                TextDecoration.BOLD));
        sender.sendMessage(Component.empty());

        if (top.isEmpty()) {
            sender.sendMessage(Component.text("  Aucune donnée disponible.", NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < top.size(); i++) {
                PlayerStats ps = top.get(i);
                String name = ps.getLastKnownName() != null ? ps.getLastKnownName()
                        : ps.getUuid().toString().substring(0, 8);
                long value = ps.getStat(stat);

                NamedTextColor rankColor = switch (i) {
                    case 0 -> NamedTextColor.GOLD;
                    case 1 -> NamedTextColor.GRAY;
                    case 2 -> NamedTextColor.DARK_RED;
                    default -> NamedTextColor.WHITE;
                };

                String valueStr;
                if (stat.equalsIgnoreCase("time") || stat.equalsIgnoreCase("survival_time")) {
                    long hours = value / 3600;
                    long minutes = (value % 3600) / 60;
                    long seconds = value % 60;
                    valueStr = String.format("%dh %02dm %02ds", hours, minutes, seconds);
                } else {
                    valueStr = String.valueOf(value);
                }

                sender.sendMessage(
                        Component.text("  " + (i + 1) + ". ", rankColor, TextDecoration.BOLD)
                                .append(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.BOLD,
                                        false))
                                .append(Component.text(" — ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD,
                                        false))
                                .append(Component.text(valueStr, NamedTextColor.YELLOW).decoration(TextDecoration.BOLD,
                                        false)));
            }
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                TextDecoration.STRIKETHROUGH));
    }

    /**
     * Send a formatted player stat card.
     */
    public void sendPlayerStats(CommandSender sender, PlayerStats stats) {
        String name = stats.getLastKnownName() != null ? stats.getLastKnownName()
                : stats.getUuid().toString().substring(0, 8);

        sender.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                TextDecoration.STRIKETHROUGH));
        sender.sendMessage(Component.text(" ★ Stats de " + name + " ★", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.empty());

        sendStatLine(sender, "Kills", stats.getKills());
        sendStatLine(sender, "Morts", stats.getDeaths());
        sendStatLine(sender, "Victoires", stats.getWins());
        sendStatLine(sender, "Parties", stats.getGamesPlayed());

        long time = stats.getTotalSurvivalTime();
        long hours = time / 3600;
        long minutes = (time % 3600) / 60;
        long seconds = time % 60;
        sender.sendMessage(
                Component.text("  Survie : ", NamedTextColor.GRAY)
                        .append(Component.text(String.format("%dh %02dm %02ds", hours, minutes, seconds),
                                NamedTextColor.WHITE)));

        if (stats.getDeaths() > 0) {
            double kd = (double) stats.getKills() / stats.getDeaths();
            sender.sendMessage(
                    Component.text("  K/D : ", NamedTextColor.GRAY)
                            .append(Component.text(String.format("%.2f", kd), NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("------------------------------", NamedTextColor.DARK_GRAY,
                TextDecoration.STRIKETHROUGH));
    }

    private void sendStatLine(CommandSender sender, String label, int value) {
        sender.sendMessage(
                Component.text("  " + label + " : ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(value), NamedTextColor.WHITE)));
    }
}
