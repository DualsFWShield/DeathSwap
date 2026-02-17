package be.dualsfwshield.deathswap;

import be.dualsfwshield.deathswap.challenges.ChallengeListener;
import be.dualsfwshield.deathswap.challenges.ChallengeManager;
import be.dualsfwshield.deathswap.commands.DeathSwapCommand;
import be.dualsfwshield.deathswap.modes.BlockShuffleListener;
import be.dualsfwshield.deathswap.modes.DeathShuffleListener;
import be.dualsfwshield.deathswap.listeners.GameListener;
import be.dualsfwshield.deathswap.listeners.LobbyListener;
import be.dualsfwshield.deathswap.listeners.ReadyListener;
import be.dualsfwshield.deathswap.listeners.SpectatorListener;
import be.dualsfwshield.deathswap.sounds.SoundManager;
import be.dualsfwshield.deathswap.stats.LeaderboardManager;
import be.dualsfwshield.deathswap.stats.StatsManager;
import be.dualsfwshield.deathswap.vote.VoteManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathSwapPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private StatsManager statsManager;
    private SoundManager soundManager;
    private ChallengeManager challengeManager;
    private VoteManager voteManager;
    private LeaderboardManager leaderboardManager;
    private be.dualsfwshield.deathswap.gui.SettingsGUI settingsGUI;
    private be.dualsfwshield.deathswap.gui.SwapTimerGUI swapTimerGUI;
    private be.dualsfwshield.deathswap.gui.GamerulesGUI gamerulesGUI;
    private be.dualsfwshield.deathswap.gui.AdminGUI adminGUI;
    private be.dualsfwshield.deathswap.gui.ArenaDetailsGUI arenaDetailsGUI;
    private be.dualsfwshield.deathswap.gui.PlayerListGUI playerListGUI;
    private be.dualsfwshield.deathswap.gui.PlayerActionGUI playerActionGUI;
    private be.dualsfwshield.deathswap.gui.ConfirmationGUI confirmationGUI;
    private be.dualsfwshield.deathswap.gui.ArenaListGUI arenaListGUI;
    private be.dualsfwshield.deathswap.listeners.ChatInputListener chatInputListener;

    @Override
    public void onEnable() {
        // 1. Config Manager
        this.configManager = new ConfigManager(this);

        // 2. Feature Managers
        if (configManager.isStatsEnabled()) {
            this.statsManager = new StatsManager(this);
            this.leaderboardManager = new LeaderboardManager(this);
        }

        if (configManager.isSoundsEnabled()) {
            this.soundManager = new SoundManager(this);
        }

        if (configManager.isChallengesEnabled()) {
            this.challengeManager = new ChallengeManager(this);
            getServer().getPluginManager().registerEvents(new ChallengeListener(this), this);
        }

        if (configManager.isVotingEnabled()) {
            this.voteManager = new VoteManager(this);
        }

        // GUI Managers
        this.settingsGUI = new be.dualsfwshield.deathswap.gui.SettingsGUI(this);
        this.swapTimerGUI = new be.dualsfwshield.deathswap.gui.SwapTimerGUI(this);
        this.gamerulesGUI = new be.dualsfwshield.deathswap.gui.GamerulesGUI(this);
        this.adminGUI = new be.dualsfwshield.deathswap.gui.AdminGUI(this);
        this.arenaDetailsGUI = new be.dualsfwshield.deathswap.gui.ArenaDetailsGUI(this);
        this.playerListGUI = new be.dualsfwshield.deathswap.gui.PlayerListGUI(this);
        this.playerActionGUI = new be.dualsfwshield.deathswap.gui.PlayerActionGUI(this);
        this.confirmationGUI = new be.dualsfwshield.deathswap.gui.ConfirmationGUI(this);
        this.arenaListGUI = new be.dualsfwshield.deathswap.gui.ArenaListGUI(this);
        getServer().getPluginManager().registerEvents(settingsGUI, this);
        getServer().getPluginManager().registerEvents(swapTimerGUI, this);
        getServer().getPluginManager().registerEvents(gamerulesGUI, this);
        getServer().getPluginManager().registerEvents(adminGUI, this);
        getServer().getPluginManager().registerEvents(arenaDetailsGUI, this);
        getServer().getPluginManager().registerEvents(playerListGUI, this);
        getServer().getPluginManager().registerEvents(playerActionGUI, this);
        getServer().getPluginManager().registerEvents(playerActionGUI, this);
        getServer().getPluginManager().registerEvents(confirmationGUI, this);
        getServer().getPluginManager().registerEvents(arenaListGUI, this);

        // Chat Input Listener
        this.chatInputListener = new be.dualsfwshield.deathswap.listeners.ChatInputListener(this);
        getServer().getPluginManager().registerEvents(chatInputListener, this);

        // 3. Register Mode Listeners
        getServer().getPluginManager().registerEvents(new DeathShuffleListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockShuffleListener(this), this);

        // 4. Arena Manager (after config loaded)
        this.arenaManager = new ArenaManager(this);

        // 4b. Register gameplay listeners (need ArenaManager)
        getServer().getPluginManager().registerEvents(new ReadyListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);

        // 5. Commands
        DeathSwapCommand dsCommand = new DeathSwapCommand(this);
        getCommand("ds").setExecutor(dsCommand);
        getCommand("ds").setTabCompleter(dsCommand);

        getLogger().info("DeathSwap v" + getDescription().getVersion() + " est activé !");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.reload(); // Stops all games
        }
        if (statsManager != null) {
            statsManager.shutdown();
        }
        getLogger().info("DeathSwap est désactivé.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public ChallengeManager getChallengeManager() {
        return challengeManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public be.dualsfwshield.deathswap.gui.SettingsGUI getSettingsGUI() {
        return settingsGUI;
    }

    public be.dualsfwshield.deathswap.gui.SwapTimerGUI getSwapTimerGUI() {
        return swapTimerGUI;
    }

    public be.dualsfwshield.deathswap.gui.GamerulesGUI getGamerulesGUI() {
        return gamerulesGUI;
    }

    public be.dualsfwshield.deathswap.gui.AdminGUI getAdminGUI() {
        return adminGUI;
    }

    public be.dualsfwshield.deathswap.gui.ArenaDetailsGUI getArenaDetailsGUI() {
        return arenaDetailsGUI;
    }

    public be.dualsfwshield.deathswap.gui.PlayerListGUI getPlayerListGUI() {
        return playerListGUI;
    }

    public be.dualsfwshield.deathswap.gui.PlayerActionGUI getPlayerActionGUI() {
        return playerActionGUI;
    }

    public be.dualsfwshield.deathswap.gui.ConfirmationGUI getConfirmationGUI() {
        return confirmationGUI;
    }

    public be.dualsfwshield.deathswap.gui.ArenaListGUI getArenaListGUI() {
        return arenaListGUI;
    }

    public be.dualsfwshield.deathswap.listeners.ChatInputListener getChatInputListener() {
        return chatInputListener;
    }
}
