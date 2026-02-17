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
            default -> sendHelp(sender);
        }
        return true;
    }

    // --- Subcommands ---

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Seul un joueur peut rejoindre.", NamedTextColor.RED));
            return true;
        }

        String arenaId = "default";
        if (args.length > 1) {
            arenaId = args[1];
        }

        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        if (game == null) {
            player.sendMessage(Component.text("Arène introuvable : " + arenaId, NamedTextColor.RED));
            return true;
        }

        game.joinLobby(player);
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Seul un joueur peut quitter.", NamedTextColor.RED));
            return true;
        }

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null) {
            player.sendMessage(Component.text("Tu n'es dans aucune partie.", NamedTextColor.RED));
            return true;
        }

        game.sendToHub(player);
        player.sendMessage(Component.text("Tu as quitté la partie.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathswap.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
            return true;
        }

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null) {
            sender.sendMessage(Component.text("Tu dois être dans une arène pour la démarrer.", NamedTextColor.RED));
            return true;
        }

        boolean debug = args.length > 1 && args[1].equalsIgnoreCase("debug");
        game.startGame(debug);
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathswap.admin")) {
            sender.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED));
            return true;
        }

        String arenaId = "default";
        if (args.length > 1) {
            arenaId = args[1];
        }

        GameInstance game = plugin.getArenaManager().getArena(arenaId);
        if (game == null) {
            sender.sendMessage(Component.text("Arène introuvable.", NamedTextColor.RED));
            return true;
        }

        game.stopGame();
        sender.sendMessage(Component.text("Arène " + arenaId + " arrêtée.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSwapNow(CommandSender sender) {
        if (!sender.hasPermission("deathswap.admin")) {
            sender.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
            return true;
        }

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null) {
            sender.sendMessage(Component.text("Tu n'es pas dans une partie.", NamedTextColor.RED));
            return true;
        }

        game.performSwap();
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("--- Arènes ---", NamedTextColor.GOLD));
        for (GameInstance game : plugin.getArenaManager().getAllArenas()) {
            sender.sendMessage(Component.text(
                    "- " + game.getArenaId() + " [" + game.getState().name() + "] "
                            + game.getAlivePlayers().size() + " joueurs"));
        }
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game == null || !game.getSpectators().contains(player)) {
            player.sendMessage(Component.text("Tu dois être spectateur.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /ds tp <joueur>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !game.getAlivePlayers().contains(target)) {
            player.sendMessage(Component.text("Joueur introuvable ou mort.", NamedTextColor.RED));
            return true;
        }

        player.teleport(target);
        player.sendMessage(Component.text("Téléportation vers " + target.getName(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSettings(CommandSender sender) {
        if (!sender.hasPermission("deathswap.admin")) {
            sender.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Commande joueur uniquement.", NamedTextColor.RED));
            return true;
        }

        // Open settings for the arena the player is in, or default
        GameInstance game = plugin.getArenaManager().getPlayerArena(player);
        if (game != null && (game.getState() == be.dualsfwshield.deathswap.GameState.RUNNING
                || game.getState() == be.dualsfwshield.deathswap.GameState.STARTING)) {
            player.sendMessage(
                    Component.text("Impossible de modifier les paramètres pendant une partie.", NamedTextColor.RED));
            return true;
        }
        String arenaId = (game != null) ? game.getArenaId() : "default";
        plugin.getSettingsGUI().open(player, arenaId);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("deathswap.admin")) {
            sender.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED));
            return true;
        }

        plugin.getConfigManager().load();
        plugin.getArenaManager().reload(); // This handles stopping games too
        if (plugin.getStatsManager() != null)
            plugin.getStatsManager().load();
        if (plugin.getChallengeManager() != null)
            plugin.getChallengeManager().loadChallenges();

        sender.sendMessage(Component.text("Configuration rechargée.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (plugin.getStatsManager() == null) {
            sender.sendMessage(Component.text("Les stats sont désactivées.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Usage console: /ds stats <joueur>", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Les stats sont désactivées.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Commande joueur.", NamedTextColor.RED));
            return true;
        }

        if (plugin.getVoteManager() == null || !plugin.getConfigManager().isVotingEnabled()) {
            player.sendMessage(Component.text("Le vote est désactivé.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            // Usually triggered by click, so manual usage is rare
            player.sendMessage(Component.text("Usage: /ds vote <arena> <choix>", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Aucun vote en cours.", NamedTextColor.RED));
            return true;
        }

        boolean success = plugin.getVoteManager().castVote(arenaId, player.getUniqueId(), choice);
        if (success) {
            player.sendMessage(Component.text("✔ Vote enregistré !", NamedTextColor.GREEN));
            if (plugin.getSoundManager() != null) {
                plugin.getSoundManager().playSound("vote-cast", player);
            }
        } else {
            player.sendMessage(Component.text("Vote invalide.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cette commande est réservée aux joueurs.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("deathswap.admin")) {
            player.sendMessage(Component.text("Vous n'avez pas la permission.", NamedTextColor.RED));
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
                    player.sendMessage(Component.text("Usage: /ds admin create <nom>", NamedTextColor.RED));
                    return true;
                }
                String name = args[2].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(name) != null) {
                    player.sendMessage(Component.text("L'arène '" + name + "' existe déjà !", NamedTextColor.RED));
                    return true;
                }
                plugin.getConfigManager().createArena(name);
                plugin.getArenaManager().reload();
                player.sendMessage(Component.text("✔ Arène '" + name + "' créée ! Ouverture des settings...",
                        NamedTextColor.GREEN));
                plugin.getSettingsGUI().open(player, name);
            }
            case "edit" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /ds admin edit <nom>", NamedTextColor.RED));
                    return true;
                }
                String editName = args[2].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(editName) == null) {
                    player.sendMessage(
                            Component.text("Arène introuvable : " + editName, NamedTextColor.RED));
                    return true;
                }
                plugin.getSettingsGUI().open(player, editName);
            }
            case "save" -> {
                plugin.getConfigManager().save();
                player.sendMessage(Component.text("✔ Toutes les configurations sauvegardées.", NamedTextColor.GREEN));
            }
            case "delete" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /ds admin delete <nom>", NamedTextColor.RED));
                    return true;
                }
                String delName = args[2].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(delName) == null) {
                    player.sendMessage(Component.text("Arène introuvable : " + delName, NamedTextColor.RED));
                    return true;
                }
                // Open confirmation GUI
                plugin.getConfirmationGUI().open(player,
                        "Supprimer: " + delName,
                        "L'arène et son fichier seront supprimés.",
                        NamedTextColor.RED,
                        () -> {
                            plugin.getConfigManager().deleteArena(delName);
                            plugin.getArenaManager().reload();
                            player.sendMessage(Component.text("✔ Arène '" + delName + "' supprimée.",
                                    NamedTextColor.GREEN));
                        },
                        () -> player.sendMessage(Component.text("Suppression annulée.", NamedTextColor.YELLOW)));
            }
            case "clone" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /ds admin clone <source> <destination>",
                            NamedTextColor.RED));
                    return true;
                }
                String srcName = args[2].toLowerCase();
                String dstName = args[3].toLowerCase();
                if (plugin.getConfigManager().getArenaConfig(srcName) == null) {
                    player.sendMessage(Component.text("Arène source introuvable : " + srcName, NamedTextColor.RED));
                    return true;
                }
                if (plugin.getConfigManager().getArenaConfig(dstName) != null) {
                    player.sendMessage(Component.text("L'arène '" + dstName + "' existe déjà !", NamedTextColor.RED));
                    return true;
                }
                boolean success = plugin.getConfigManager().cloneArena(srcName, dstName);
                if (success) {
                    plugin.getArenaManager().reload();
                    player.sendMessage(Component.text("✔ Arène '" + srcName + "' clonée vers '" + dstName + "' !",
                            NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Erreur lors du clonage.", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Usage: /ds admin set <arena> <property> <value>", NamedTextColor.RED));
            return;
        }
        String arenaId = args[2].toLowerCase();
        String property = args[3].toLowerCase();
        String value = args[4];

        be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            player.sendMessage(Component.text("Arène introuvable : " + arenaId, NamedTextColor.RED));
            return;
        }

        try {
            switch (property) {
                case "lobby" -> config.lobbyWorld = value;
                case "game" -> config.gameWorld = value;
                case "gametype" -> config.gameType = be.dualsfwshield.deathswap.GameType.valueOf(value.toUpperCase());
                case "resilience" -> {
                    boolean b = Boolean.parseBoolean(value);
                    config.startIfMinPlayersMet = b;
                    config.preventCancelAfterCountdown = b;
                }
                default -> {
                    player.sendMessage(Component.text("Propriété inconnue: " + property, NamedTextColor.RED));
                    return;
                }
            }
            plugin.getConfigManager().saveArena(config);
            player.sendMessage(Component.text("Propriété '" + property + "' mise à jour.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Valeur invalide.", NamedTextColor.RED));
        }
    }

    private void handleAdminGamerule(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(Component.text("Usage: /ds admin gamerule <arena> <set|remove> <rule> [value]",
                    NamedTextColor.RED));
            return;
        }
        String arenaId = args[2].toLowerCase();
        String action = args[3].toLowerCase();
        String rule = args[4];

        be.dualsfwshield.deathswap.ConfigManager.ArenaConfig config = plugin.getConfigManager().getArenaConfig(arenaId);
        if (config == null) {
            player.sendMessage(Component.text("Arène introuvable : " + arenaId, NamedTextColor.RED));
            return;
        }

        if (action.equals("remove")) {
            config.gamerules.remove(rule);
            player.sendMessage(Component.text("Gamerule '" + rule + "' supprimée.", NamedTextColor.GREEN));
        } else if (action.equals("set")) {
            if (args.length < 6) {
                player.sendMessage(
                        Component.text("Usage: /ds admin gamerule <arena> set <rule> <value>", NamedTextColor.RED));
                return;
            }
            String val = args[5];
            config.gamerules.put(rule, val);
            player.sendMessage(Component.text("Gamerule '" + rule + "' définie à " + val, NamedTextColor.GREEN));
        }
        plugin.getConfigManager().saveArena(config);
    }

    private void handleAdminCommand(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(
                    Component.text("Usage: /ds admin command <arena> <tp|reset> <value...>", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Arène introuvable : " + arenaId, NamedTextColor.RED));
            return;
        }

        if (type.equals("tp")) {
            if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("default")) {
                config.teleportCommand = null;
                player.sendMessage(Component.text("Commande TP réinitialisée (Défaut).", NamedTextColor.GREEN));
            } else {
                config.teleportCommand = value;
                player.sendMessage(Component.text("Commande TP définie.", NamedTextColor.GREEN));
            }
        } else if (type.equals("reset")) {
            if (value.equalsIgnoreCase("none")) {
                config.worldResetCommands = new ArrayList<>();
                player.sendMessage(Component.text("Reset désactivé.", NamedTextColor.GREEN));
            } else if (value.toLowerCase().startsWith("mv")) {
                config.worldResetCommands = new ArrayList<>();
                config.worldResetCommands.add("mv regen %world% -s %seed%");
                config.worldResetCommands.add("mv confirm");
                player.sendMessage(Component.text("Reset défini sur Multiverse.", NamedTextColor.GREEN));
            } else if (value.toLowerCase().startsWith("cwr")) {
                config.worldResetCommands = new ArrayList<>();
                config.worldResetCommands.add("cwr edit %world% setSeed %seed%");
                config.worldResetCommands.add("cwr reset %world%");
                player.sendMessage(Component.text("Reset défini sur Custom CWR.", NamedTextColor.GREEN));
            } else {
                // Custom splitted by ';'
                config.worldResetCommands = new ArrayList<>();
                for (String cmd : value.split(";")) {
                    config.worldResetCommands.add(cmd.trim());
                }
                player.sendMessage(Component.text("Reset défini (" + config.worldResetCommands.size() + " cmds).",
                        NamedTextColor.GREEN));
            }
        }
        plugin.getConfigManager().saveArena(config);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- Aide DeathSwap ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/ds join [arena]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/ds leave", NamedTextColor.YELLOW));
        if (sender.hasPermission("deathswap.admin")) {
            sender.sendMessage(Component.text("/ds start [debug]", NamedTextColor.RED));
            sender.sendMessage(Component.text("/ds stop [arena]", NamedTextColor.RED));
            sender.sendMessage(Component.text("/ds reload", NamedTextColor.RED));
            sender.sendMessage(Component.text("/ds swapnow", NamedTextColor.RED));
            sender.sendMessage(Component.text("/ds admin [create|edit|delete|clone|list|save]", NamedTextColor.RED));
            sender.sendMessage(Component.text("/ds settings", NamedTextColor.RED));
        }
        sender.sendMessage(Component.text("/ds stats [joueur]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/ds top [kills|wins|time|games]", NamedTextColor.AQUA));
    }

    // --- Tab Completion ---

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList(
                    "join", "leave", "list", "stats", "top", "vote"));
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
            if (sub.equals("top")) {
                return filter(Arrays.asList("wins", "kills", "deaths", "time", "games"), args[1]);
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
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
