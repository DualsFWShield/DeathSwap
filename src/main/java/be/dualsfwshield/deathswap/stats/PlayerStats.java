package be.dualsfwshield.deathswap.stats;

import java.util.UUID;

/**
 * Holds all stats for a single player.
 */
public class PlayerStats {

    private final UUID uuid;
    private String lastKnownName;
    private int kills;
    private int deaths;
    private int wins;
    private int gamesPlayed;
    private long totalSurvivalTime; // in seconds
    private long lastPlayed; // epoch millis

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String name) {
        this.lastKnownName = name;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public int getWins() {
        return wins;
    }

    public void addWin() {
        wins++;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void addGamePlayed() {
        gamesPlayed++;
    }

    public long getTotalSurvivalTime() {
        return totalSurvivalTime;
    }

    public void addSurvivalTime(long seconds) {
        totalSurvivalTime += seconds;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long epoch) {
        lastPlayed = epoch;
    }

    /**
     * Get a stat value by name.
     */
    public long getStat(String statName) {
        return switch (statName.toLowerCase()) {
            case "kills" -> kills;
            case "deaths" -> deaths;
            case "wins" -> wins;
            case "games", "games_played" -> gamesPlayed;
            case "time", "survival_time" -> totalSurvivalTime;
            default -> 0;
        };
    }
}
