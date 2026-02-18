package be.dualsfwshield.deathswap.commands;

import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeathSwapCommand implements CommandExecutor, TabCompleter {

    private final DeathSwapPlugin plugin;

    public DeathSwapCommand(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "join" -> {
                return handleJoin(sender, args);
            }
            case "leave" -> {
                return handleLeave(sender);
            }
            case "start" -> {
                return handleStart(sender, args);
            }
            case "stop" -> {
                return handleStop(sender, args);
            }
            case "swapnow" -> {
                return handleSwapNow(sender);
            }
            case "list" -> {
                return handleList(sender);
            }
            case "tp" -> {
                return handleTp(sender, args);
            }
            case "settings" -> {
                return handleSettings(sender);
            }
            case "reload" -> {
                return handleReload(sender);
            }
            case "stats" -> {
                return handleStats(sender, args);
            }
            case "top" -> {
                return handleTop(sender, args);
            }
            case "vote" -> {
                return handleVote(sender, args);
            }
            case "admin" -> {
                return handleAdmin(sender, args);
            }
            case "help" -> {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("commands") && sender.hasPermission("deathswap.admin")) {
                         sendAdminHelp(sender);
                         return true;
                    }
                    if (args[1].equalsIgnoreCase("gui") && sender instanceof Player player) {
                        plugin.getHelpGUI().open(player);
                        return true;
                    }
                }
                sendHelp(sender);
                return true;
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    // --- Subcommands ---

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "command-player-only");
            return true;
        }

        String arenaId = "default";
        if (args.length > 1) {
            arenaId = args[1];
        }

        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        if (game == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-join-not-found", "%arena%", arenaId);
            return true;
        }

        game.joinLobby(player);
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "command-player-only");
            return true;
        }

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-leave-not-in-game");
            return true;
        }

        game.sendToHub(player);
        be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-leave-success");
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathswap.admin")) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "no-permission");
            return true;
        }

        if (!(sender instanceof Player player)) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "command-player-only");
            return true;
        }

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-start-not-in-game");
            return true;
        }

        boolean debug = args.length > 1 && args[1].equalsIgnoreCase("debug");
        game.startGame(debug);
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathswap.admin")) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "no-permission");
            return true;
        }

        String arenaId = "default";
        if (args.length > 1) {
            arenaId = args[1];
        }

        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        if (game == null) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-stop-not-found");
            return true;
        }

        game.stopGame();
        be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-stop-success", "%arena%", arenaId);
        return true;
    }

    private boolean handleSwapNow(CommandSender sender) {
        if (!sender.hasPermission("deathswap.admin")) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "no-permission");
            return true;
        }

        if (!(sender instanceof Player player)) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "command-player-only");
            return true;
        }

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-swap-not-in-game");
            return true;
        }

        game.performSwap();
        return true;
    }

    private boolean handleList(CommandSender sender) {
        be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-list-header");
        for (GameInstance game : plugin.getArenaManager().getAllArenas()) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-list-format", 
                "%arena%", game.getArenaId(), 
                "%state%", game.getState().name(), 
                "%count%", String.valueOf(game.getAlivePlayers().size()));
        }
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || !game.getSpectators().contains(player)) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-tp-spectator-only");
            return true;
        }

        if (args.length < 2) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "command-usage", "%usage%", "/ds tp <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !game.getAlivePlayers().contains(target)) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-tp-player-not-found");
            return true;
        }

        player.teleport(target);
        be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-tp-success", "%player%", target.getName());
        return true;
    }

    private boolean handleSettings(CommandSender sender) {
        if (!sender.hasPermission("deathswap.admin")) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "command-player-only");
            return true;
        }

        // Open settings for the arena the player is in, or default
        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game != null && (game.getState() == be.dualsfwshield.deathswap.GameState.RUNNING
                || game.getState() == be.dualsfwshield.deathswap.GameState.STARTING)) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-settings-in-game");
            return true;
        }
        String arenaId = (game != null) ? game.getArenaId() : "default";
        plugin.getSettingsGUI().open(player, arenaId);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("deathswap.admin")) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "no-permission");
            return true;
        }

        plugin.getConfigManager().load();
        plugin.getArenaManager().reload(); // This handles stopping games too
        if (plugin.getStatsManager() != null)
            plugin.getStatsManager().load();
        if (plugin.getChallengeManager() != null)
            plugin.getChallengeManager().loadChallenges();

        be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-admin-reload");
        return true;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (plugin.getStatsManager() == null) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-stats-disabled");
            return true;
        }

        UUID targetUuid;
        String targetName;

        if (args.length > 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else if (sender instanceof Player p) {
            targetUuid = p.getUniqueId();
            targetName = p.getName();
        } else {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-stats-console-usage");
            return true;
        }

        PlayerStats stats = plugin.getStatsManager().getStats(targetUuid);
        if (targetName != null)
            stats.setLastKnownName(targetName); // Update name if known
        plugin.getLeaderboardManager().sendPlayerStats(sender, stats);
        return true;
    }

    private boolean handleTop(CommandSender sender, String[] args) {
        if (plugin.getStatsManager() == null) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "cmd-stats-disabled");
            return true;
        }

        String stat = "wins";
        if (args.length > 1) {
            stat = args[1].toLowerCase();
        }

        int limit = 10;
        plugin.getLeaderboardManager().sendLeaderboard(sender, stat, limit);
        return true;
    }

    private boolean handleVote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "command-player-only");
            return true;
        }

        if (plugin.getVoteManager() == null || !plugin.getConfigManager().isVotingEnabled()) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-vote-disabled");
            return true;
        }

        if (args.length < 3) {
            // Usually triggered by click, so manual usage is rare
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-vote-usage");
            return true;
        }

        String arenaId = args[1];
        int choice;
        try {
            choice = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return true;
        }

        if (!plugin.getVoteManager().isVoteActive(arenaId)) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-vote-no-vote");
            return true;
        }

        boolean success = plugin.getVoteManager().castVote(arenaId, player.getUniqueId(), choice);
        if (success) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-vote-success");
            if (plugin.getSoundManager() != null) {
                plugin.getSoundManager().playSound("vote-cast", player);
            }
        } else {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-vote-invalid");
        }

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            be.dualsfwshield.deathswap.util.Lang.send(sender, "command-player-only");
            return true;
        }

        if (!player.hasPermission("deathswap.admin")) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "no-permission");
            return true;
        }

        // No subcommand → open Arena List GUI (new Dashboard)
        if (args.length < 2) {
            plugin.getArenaListGUI().open(player);
            return true;
        }

        String adminSub = args[1].toLowerCase();

        switch (adminSub) {
            case "create" -> {
                if (args.length < 3) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-usage-create");
                    return true;
                }
                String name = args[2].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(name) != null) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-arena-exists", "%arena%", name);
                    return true;
                }
                plugin.getConfigManager().createArena(name);
                plugin.getArenaManager().reload();
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-create-success", "%arena%", name);
                plugin.getSettingsGUI().open(player, name);
            }
            case "edit" -> {
                if (args.length < 3) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-usage-edit");
                    return true;
                }
                String editName = args[2].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(editName) == null) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-join-not-found", "%arena%", editName);
                    return true;
                }
                plugin.getSettingsGUI().open(player, editName);
            }
            case "save" -> {
                plugin.getConfigManager().save();
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-save-success");
            }
            case "delete" -> {
                if (args.length < 3) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-usage-delete");
                    return true;
                }
                String delName = args[2].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(delName) == null) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-join-not-found", "%arena%", delName);
                    return true;
                }
                // Open confirmation GUI
                plugin.getConfirmationGUI().open(player,
                        be.dualsfwshield.deathswap.util.Lang.get("cmd-admin-delete-confirm-title", "%arena%", delName),
                        be.dualsfwshield.deathswap.util.Lang.get("cmd-admin-delete-confirm-subtitle"),
                        NamedTextColor.RED,
                        () -> {
                            plugin.getConfigManager().deleteArena(delName);
                            plugin.getArenaManager().reload();
                            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-delete-success", "%arena%", delName);
                        },
                        () -> be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-delete-cancel"));
            }
            case "clone" -> {
                if (args.length < 4) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-usage-clone");
                    return true;
                }
                String srcName = args[2].toLowerCase();
                String dstName = args[3].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(srcName) == null) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-clone-src-not-found", "%arena%", srcName);
                    return true;
                }
                if (plugin.getConfigManager().getArenaConfig(dstName) != null) {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-arena-exists", "%arena%", dstName);
                    return true;
                }
                boolean success = plugin.getConfigManager().cloneArena(srcName, dstName);
                if (success) {
                    plugin.getArenaManager().reload();
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-clone-success", "%src%", srcName, "%dst%", dstName);
                } else {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-clone-error");
                }
            }
            case "list" -> {
                plugin.getArenaListGUI().open(player);
            }
            case "set" -> {
                handleAdminSet(player, args);
            }
            case "gamerule" -> {
                handleAdminGamerule(player, args);
            }
            case "command" -> {
                handleAdminCommand(player, args);
            }
            default -> {
                // Unknown subcommand, open Arena List GUI
                plugin.getArenaListGUI().open(player);
            }
        }
        return true;
    }

    private void handleAdminSet(Player player, String[] args) {
        if (args.length < 5) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-usage-set");
            return;
        }
        String arenaId = args[2].toLowerCase();
        String property = args[3].toLowerCase();
        String value = args[4];

        be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-join-not-found", "%arena%", arenaId);
            return;
        }

        try {
            switch (property) {
                // Worlds
                case "lobby" -> config.lobbyWorld = value;
                case "game" -> config.gameWorld = value;
                
                // Game Type
                case "gametype" -> config.gameType = be.dualsfwshield.deathswap.GameType.valueOf(value.toUpperCase());
                
                // Players
                case "minplayers" -> config.minPlayers = Integer.parseInt(value);
                case "maxplayers" -> config.maxPlayers = Integer.parseInt(value);
                
                // UI Mode
                case "uimode" -> config.uiMode = be.dualsfwshield.deathswap.UIMode.valueOf(value.toUpperCase());
                
                // Timers
                case "loadtime" -> config.loadTime = Integer.parseInt(value);
                case "swapmode" -> config.swapMode = be.dualsfwshield.deathswap.SwapMode.valueOf(value.toUpperCase());
                case "swapinterval" -> config.swapInterval = Integer.parseInt(value);
                case "swapmin" -> config.swapMin = Integer.parseInt(value);
                case "swapmax" -> config.swapMax = Integer.parseInt(value);
                case "maxgametime" -> config.maxGameTime = Integer.parseInt(value);
                case "spawnprotection" -> config.spawnProtection = Integer.parseInt(value);
                
                // Round Timers
                case "roundtimeeasy" -> config.roundTimeEasy = Integer.parseInt(value);
                case "roundtimemedium" -> config.roundTimeMedium = Integer.parseInt(value);
                case "roundtimehard" -> config.roundTimeHard = Integer.parseInt(value);
                
                // Game Rules
                case "pvp" -> config.pvpEnabled = Boolean.parseBoolean(value);
                case "nether" -> config.netherEnabled = Boolean.parseBoolean(value);
                case "end" -> config.endEnabled = Boolean.parseBoolean(value);
                
                // Resilience
                case "resilience" -> {
                    boolean b = Boolean.parseBoolean(value);
                    config.startIfMinPlayersMet = b;
                    config.preventCancelAfterCountdown = b;
                }
                
                default -> {
                    be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-set-unknown", "%prop%", property);
                    return;
                }
            }
            plugin.getConfigManager().saveArena(config);
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-set-success", "%prop%", property);
        } catch (IllegalArgumentException e) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-set-invalid");
        }
    }

    private void handleAdminGamerule(Player player, String[] args) {
        if (args.length < 5) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-usage-gamerule");
            return;
        }
        String arenaId = args[2].toLowerCase();
        String action = args[3].toLowerCase();
        String rule = args[4];

        be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-join-not-found", "%arena%", arenaId);
            return;
        }

        if (action.equals("remove")) {
            config.gamerules.remove(rule);
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-gamerule-removed", "%rule%", rule);
        } else if (action.equals("set")) {
            if (args.length < 6) {
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-usage-gamerule");
                return;
            }
            String val = args[5];
            config.gamerules.put(rule, val);
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-gamerule-set-success", "%rule%", rule, "%value%", val);
        }
        plugin.getConfigManager().saveArena(config);
    }

    private void handleAdminCommand(Player player, String[] args) {
        if (args.length < 5) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-command-usage");
            return;
        }
        String arenaId = args[2].toLowerCase();
        String type = args[3].toLowerCase();

        // Join rest of args
        StringBuilder sb = new StringBuilder();
        for (int i = 4; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String value = sb.toString().trim();

        be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-join-not-found", "%arena%", arenaId);
            return;
        }

        if (type.equals("tp")) {
            if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("default")) {
                config.teleportCommand = null;
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-command-tp-reset");
            } else {
                config.teleportCommand = value;
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-command-tp-set");
            }
        } else if (type.equals("reset")) {
            if (value.equalsIgnoreCase("none")) {
                config.worldResetCommands = new ArrayList<>();
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-command-reset-disabled");
            } else if (value.toLowerCase().startsWith("mv")) {
                config.worldResetCommands = new ArrayList<>();
                config.worldResetCommands.add("mv regen %world% -s %seed%");
                config.worldResetCommands.add("mv confirm");
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-command-reset-mv");
            } else if (value.toLowerCase().startsWith("cwr")) {
                config.worldResetCommands = new ArrayList<>();
                config.worldResetCommands.add("cwr edit %world% setSeed %seed%");
                config.worldResetCommands.add("cwr reset %world%");
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-command-reset-cwr");
            } else {
                // Custom splitted by ';'
                config.worldResetCommands = new ArrayList<>();
                for (String cmd : value.split(";")) {
                    config.worldResetCommands.add(cmd.trim());
                }
                be.dualsfwshield.deathswap.util.Lang.send(player, "cmd-admin-command-reset-custom", "%count%", String.valueOf(config.worldResetCommands.size()));
            }
        }
        plugin.getConfigManager().saveArena(config);
    }

    private void sendHelp(CommandSender sender) {
        be.dualsfwshield.deathswap.util.Lang.send(sender, "help-title");
        sender.sendMessage(Component.text("/ds join [arena]", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("help-join")));
        sender.sendMessage(Component.text("/ds leave", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("help-leave")));
        sender.sendMessage(Component.text("/ds stats [joueur]", NamedTextColor.AQUA)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("help-stats")));
        sender.sendMessage(Component.text("/ds top [stat]", NamedTextColor.AQUA)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("help-top")));
        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/ds help gui", NamedTextColor.GREEN)
                .append(be.dualsfwshield.deathswap.util.Lang.getComponent("help-gui")));
        }
            
        if (sender.hasPermission("deathswap.admin")) {
            sender.sendMessage(Component.empty());
            be.dualsfwshield.deathswap.util.Lang.send(sender, "help-admin-title");
            be.dualsfwshield.deathswap.util.Lang.send(sender, "help-admin-see-more");
            sender.sendMessage(Component.text("/ds help commands", NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(be.dualsfwshield.deathswap.util.Lang.getComponent("help-admin-hover")))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/ds help commands")));
        }
    }
    
    private void sendAdminHelp(CommandSender sender) {
        be.dualsfwshield.deathswap.util.Lang.send(sender, "admin-help-title");
        
        sender.sendMessage(Component.text("/ds admin list", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-list")));
            
        sender.sendMessage(Component.text("/ds admin create <nom>", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-create")));
            
        sender.sendMessage(Component.text("/ds admin delete <nom>", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-delete")));
            
        sender.sendMessage(Component.text("/ds admin clone <src> <dst>", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-clone")));
            
        sender.sendMessage(Component.text("/ds admin set <arena> <prop> <val>", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-set")));
            
        sender.sendMessage(Component.text("/ds admin gamerule <arena> set <rule> <val>", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-gamerule")));
            
        sender.sendMessage(Component.text("/ds admin command <arena> <tp|reset> <val>", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-command")));
            
        sender.sendMessage(Component.text("/ds start [debug]", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-start")));
            
        sender.sendMessage(Component.text("/ds stop [arena]", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-stop")));
            
        sender.sendMessage(Component.text("/ds swapnow", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-swapnow")));
            
        sender.sendMessage(Component.text("/ds reload", NamedTextColor.YELLOW)
            .append(be.dualsfwshield.deathswap.util.Lang.getComponent("admin-help-reload")));
    }

    // --- Tab Completion ---

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList(
                    "join", "leave", "list", "stats", "top", "vote", "help"));
            if (sender.hasPermission("deathswap.admin")) {
                subcommands.addAll(Arrays.asList("start", "stop", "swapnow", "reload", "settings", "admin"));
            }
            return filter(subcommands, args[0]);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("join") || sub.equals("stop")) {
                return filter(new ArrayList<>(plugin.getArenaManager().getArenaIds()), args[1]);
            }
            if (sub.equals("stats")) {
                return null; // Player names
            }
            if (sub.equals("help")) {
                return filter(Arrays.asList("commands", "gui"), args[1]);
            }
            if (sub.equals("top")) {
                return filter(Arrays.asList("wins", "kills", "deaths", "time", "games"), args[1]);
            }
            if (sub.equals("start")) {
                return filter(Collections.singletonList("debug"), args[1]);
            }
            if (sub.equals("admin")) {
                return filter(Arrays.asList("create", "edit", "delete", "clone", "list", "save", "set", "gamerule",
                        "command"), args[1]);
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String adminSub = args[1].toLowerCase();
            if (sub.equals("admin")
                    && (adminSub.equals("edit") || adminSub.equals("delete") || adminSub.equals("clone")
                            || adminSub.equals("set") || adminSub.equals("gamerule") || adminSub.equals("command"))) {
                return filter(new ArrayList<>(plugin.getArenaManager().getArenaIds()), args[2]);
            }
        } else if (args.length == 4) {
             String sub = args[0].toLowerCase();
             String adminSub = args[1].toLowerCase();
             if (sub.equals("admin")) {
                 if (adminSub.equals("set")) {
                     return filter(Arrays.asList(
                         "lobby", "game", "gametype", "minplayers", "maxplayers", "uimode",
                         "loadtime", "swapmode", "swapinterval", "swapmin", "swapmax", 
                         "maxgametime", "spawnprotection", "roundtimeeasy", "roundtimemedium", 
                         "roundtimehard", "pvp", "nether", "end", "resilience"
                     ), args[3]);
                 }
                 if (adminSub.equals("gamerule")) {
                     return filter(Arrays.asList("set", "remove"), args[3]);
                 }
                 if (adminSub.equals("command")) {
                     return filter(Arrays.asList("tp", "reset"), args[3]);
                 }
             }
        } else if (args.length == 5) {
             String sub = args[0].toLowerCase();
             String adminSub = args[1].toLowerCase();
             if (sub.equals("admin")) {
                 if (adminSub.equals("set")) {
                     String prop = args[3].toLowerCase();
                     if (prop.equals("gametype")) return filter(Arrays.stream(be.dualsfwshield.deathswap.GameType.values()).map(Enum::name).collect(Collectors.toList()), args[4]);
                     if (prop.equals("uimode")) return filter(Arrays.stream(be.dualsfwshield.deathswap.UIMode.values()).map(Enum::name).collect(Collectors.toList()), args[4]);
                     if (prop.equals("swapmode")) return filter(Arrays.stream(be.dualsfwshield.deathswap.SwapMode.values()).map(Enum::name).collect(Collectors.toList()), args[4]);
                     if (Arrays.asList("pvp", "nether", "end", "resilience").contains(prop)) return filter(Arrays.asList("true", "false"), args[4]);
                 }
                 if (adminSub.equals("command") && args[3].equalsIgnoreCase("reset")) {
                     return filter(Arrays.asList("none", "mv", "cwr"), args[4]);
                 }
             }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
