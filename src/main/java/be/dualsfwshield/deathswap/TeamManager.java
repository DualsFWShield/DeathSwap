package be.dualsfwshield.deathswap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages team data, assignment, balancing, and team-specific game mechanics
 * for a single arena's game instance.
 */
public class TeamManager {

    /**
     * Represents a single team with a color, display name, and player list.
     */
    public static class Team {
        private final String id;
        private final Material woolMaterial;
        private final String displayName;
        private final NamedTextColor textColor;
        private final Set<UUID> members = new LinkedHashSet<>();

        public Team(String id, Material woolMaterial, String displayName, NamedTextColor textColor) {
            this.id = id;
            this.woolMaterial = woolMaterial;
            this.displayName = displayName;
            this.textColor = textColor;
        }

        public String getId() { return id; }
        public Material getWoolMaterial() { return woolMaterial; }
        public String getDisplayName() { return displayName; }
        public NamedTextColor getTextColor() { return textColor; }
        public Set<UUID> getMembers() { return members; }
        public int size() { return members.size(); }

        public void addMember(UUID uuid) { members.add(uuid); }
        public void removeMember(UUID uuid) { members.remove(uuid); }
        public boolean hasMember(UUID uuid) { return members.contains(uuid); }

        /**
         * Check if this team still has alive players from the given alive set.
         */
        public boolean hasAlivePlayers(Set<Player> alivePlayers) {
            for (Player p : alivePlayers) {
                if (members.contains(p.getUniqueId())) return true;
            }
            return false;
        }

        /**
         * Get alive players in this team.
         */
        public List<Player> getAlivePlayers(Set<Player> alivePlayers) {
            List<Player> result = new ArrayList<>();
            for (Player p : alivePlayers) {
                if (members.contains(p.getUniqueId())) result.add(p);
            }
            return result;
        }
    }

    // All available team definitions (ordered by display preference)
    private static final List<TeamDef> TEAM_DEFS = List.of(
            new TeamDef("red", Material.RED_WOOL, "Rouge", NamedTextColor.RED),
            new TeamDef("blue", Material.BLUE_WOOL, "Bleu", NamedTextColor.BLUE),
            new TeamDef("green", Material.GREEN_WOOL, "Vert", NamedTextColor.GREEN),
            new TeamDef("yellow", Material.YELLOW_WOOL, "Jaune", NamedTextColor.YELLOW),
            new TeamDef("orange", Material.ORANGE_WOOL, "Orange", NamedTextColor.GOLD),
            new TeamDef("purple", Material.PURPLE_WOOL, "Violet", NamedTextColor.DARK_PURPLE),
            new TeamDef("cyan", Material.CYAN_WOOL, "Cyan", NamedTextColor.AQUA),
            new TeamDef("pink", Material.PINK_WOOL, "Rose", NamedTextColor.LIGHT_PURPLE),
            new TeamDef("lime", Material.LIME_WOOL, "Lime", NamedTextColor.GREEN),
            new TeamDef("light_blue", Material.LIGHT_BLUE_WOOL, "Bleu Clair", NamedTextColor.AQUA),
            new TeamDef("magenta", Material.MAGENTA_WOOL, "Magenta", NamedTextColor.LIGHT_PURPLE),
            new TeamDef("white", Material.WHITE_WOOL, "Blanc", NamedTextColor.WHITE)
    );

    private record TeamDef(String id, Material wool, String displayName, NamedTextColor color) {}

    private final List<Team> teams = new ArrayList<>();
    private final Map<UUID, Team> playerTeamMap = new HashMap<>();
    private final ConfigManager.ArenaConfig config;

    // Track original max health for restoration on cleanup
    private final Map<UUID, Double> originalMaxHealth = new HashMap<>();

    public TeamManager(ConfigManager.ArenaConfig config) {
        this.config = config;
        initTeams();
    }

    /**
     * Initialize team slots based on config.
     */
    private void initTeams() {
        teams.clear();
        int count = Math.min(config.maxTeams, TEAM_DEFS.size());
        for (int i = 0; i < count; i++) {
            TeamDef def = TEAM_DEFS.get(i);
            teams.add(new Team(def.id, def.wool, def.displayName, def.color));
        }
    }

    /**
     * Calculate how many teams should be active based on player count.
     * If teamSize is configured (>0), uses that. Otherwise, dynamically calculates.
     */
    public int calculateActiveTeamCount(int playerCount) {
        if (config.teamSize > 0) {
            // Fixed team size: teams = ceil(players / teamSize)
            return Math.min(
                    (int) Math.ceil((double) playerCount / config.teamSize),
                    teams.size()
            );
        }
        // Dynamic: aim for teams of 2 (most common)
        return Math.min(
                Math.max(2, playerCount / 2),
                teams.size()
        );
    }

    /**
     * Get the ideal team size based on config and player count.
     */
    public int getIdealTeamSize(int playerCount) {
        if (config.teamSize > 0) return config.teamSize;
        int teamCount = calculateActiveTeamCount(playerCount);
        return (int) Math.ceil((double) playerCount / teamCount);
    }

    /**
     * Get the list of active teams (limited to what makes sense for the player count).
     */
    public List<Team> getActiveTeams(int playerCount) {
        int count = calculateActiveTeamCount(playerCount);
        return teams.subList(0, Math.min(count, teams.size()));
    }

    /**
     * Assign a player to a specific team.
     * @return true if assignment succeeded
     */
    public boolean assignPlayerToTeam(Player player, Team team) {
        // Remove from current team first
        removeFromTeam(player);

        // Check team capacity
        int idealSize = getIdealTeamSize(getAllPlayerCount());
        if (team.size() >= idealSize + 1) {
            // Team is over-full, reject
            return false;
        }

        team.addMember(player.getUniqueId());
        playerTeamMap.put(player.getUniqueId(), team);
        return true;
    }

    /**
     * Assign a player to a team by team ID.
     */
    public boolean assignPlayerToTeam(Player player, String teamId) {
        Team team = getTeamById(teamId);
        if (team == null) return false;
        return assignPlayerToTeam(player, team);
    }

    /**
     * Remove a player from their current team.
     */
    public void removeFromTeam(Player player) {
        Team current = playerTeamMap.remove(player.getUniqueId());
        if (current != null) {
            current.removeMember(player.getUniqueId());
        }
    }

    /**
     * Get the team a player is in.
     */
    public Team getPlayerTeam(Player player) {
        return playerTeamMap.get(player.getUniqueId());
    }

    /**
     * Get a team by its ID.
     */
    public Team getTeamById(String id) {
        for (Team t : teams) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }

    /**
     * Auto-balance all unassigned players into teams equitably.
     * Players who already chose a team keep their choice.
     */
    public void autoBalanceTeams(Set<Player> allPlayers) {
        int playerCount = allPlayers.size();
        List<Team> activeTeams = getActiveTeams(playerCount);

        // Collect unassigned players
        List<Player> unassigned = new ArrayList<>();
        for (Player p : allPlayers) {
            Team t = playerTeamMap.get(p.getUniqueId());
            if (t == null || !activeTeams.contains(t)) {
                // Remove from non-active team if needed
                if (t != null) {
                    t.removeMember(p.getUniqueId());
                    playerTeamMap.remove(p.getUniqueId());
                }
                unassigned.add(p);
            }
        }

        // Shuffle for randomness
        Collections.shuffle(unassigned);

        // Assign to smallest teams first
        for (Player p : unassigned) {
            Team smallest = activeTeams.stream()
                    .min(Comparator.comparingInt(Team::size))
                    .orElse(activeTeams.get(0));
            smallest.addMember(p.getUniqueId());
            playerTeamMap.put(p.getUniqueId(), smallest);
        }
    }

    /**
     * Apply death penalty to teammates: reduce their max health by 50%.
     */
    public void applyDeathPenalty(Team team, Player deadPlayer, Set<Player> alivePlayers) {
        for (Player p : team.getAlivePlayers(alivePlayers)) {
            if (p.equals(deadPlayer)) continue;

            // Store original max health if not yet tracked
            originalMaxHealth.putIfAbsent(p.getUniqueId(), 20.0);

            AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                double currentMax = attr.getBaseValue();
                double newMax = Math.max(2.0, currentMax / 2.0); // Min 1 heart
                attr.setBaseValue(newMax);

                // Clamp current health to new max
                if (p.getHealth() > newMax) {
                    p.setHealth(newMax);
                }

                p.sendMessage(Component.text("💔 ", NamedTextColor.RED)
                        .append(Component.text(deadPlayer.getName() + " est mort ! ", NamedTextColor.GRAY))
                        .append(Component.text("Vie max réduite à " + (int)(newMax / 2) + " ❤",
                                NamedTextColor.RED, TextDecoration.BOLD)));
            }
        }
    }

    /**
     * Remove 1/N of the team's combined inventory items randomly on player death.
     * N = original team size.
     */
    public void removeItemsOnDeath(Team team, Set<Player> alivePlayers, int originalTeamSize) {
        if (originalTeamSize <= 1) return;

        List<Player> alive = team.getAlivePlayers(alivePlayers);
        if (alive.isEmpty()) return;

        // Collect all items from alive teammates
        List<ItemStack> allItems = new ArrayList<>();
        for (Player p : alive) {
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    allItems.add(item);
                }
            }
        }

        // Calculate how many items to remove (1/N of total)
        int toRemove = Math.max(1, allItems.size() / originalTeamSize);

        // Shuffle and remove randomly
        Collections.shuffle(allItems);
        Set<ItemStack> itemsToRemove = new HashSet<>(allItems.subList(0, Math.min(toRemove, allItems.size())));

        for (Player p : alive) {
            ItemStack[] contents = p.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && itemsToRemove.remove(contents[i])) {
                    p.getInventory().setItem(i, null);
                }
            }
        }

        // Notify
        for (Player p : alive) {
            p.sendMessage(Component.text("📦 Des items ont été perdus suite à la mort d'un coéquipier !",
                    NamedTextColor.RED));
        }
    }

    /**
     * Synchronize inventories: merge all team members' items and redistribute equally.
     * Called after each swap/round.
     */
    public void syncTeamInventories(Team team, Set<Player> alivePlayers) {
        List<Player> alive = team.getAlivePlayers(alivePlayers);
        if (alive.size() <= 1) return;

        // Collect all items (excluding armor and offhand)
        List<ItemStack> allItems = new ArrayList<>();
        for (Player p : alive) {
            for (int i = 0; i < 36; i++) { // Main inventory slots only
                ItemStack item = p.getInventory().getItem(i);
                if (item != null && !item.getType().isAir()) {
                    allItems.add(item.clone());
                }
            }
        }

        // Clear inventories
        for (Player p : alive) {
            for (int i = 0; i < 36; i++) {
                p.getInventory().setItem(i, null);
            }
        }

        // Redistribute round-robin
        int playerIdx = 0;
        for (ItemStack item : allItems) {
            alive.get(playerIdx % alive.size()).getInventory().addItem(item);
            playerIdx++;
        }
    }

    /**
     * Get the number of teams that still have alive players.
     */
    public int getAliveTeamCount(Set<Player> alivePlayers) {
        int count = 0;
        for (Team t : teams) {
            if (t.hasAlivePlayers(alivePlayers)) count++;
        }
        return count;
    }

    /**
     * Get the last team still alive (for win condition).
     */
    public Team getLastAliveTeam(Set<Player> alivePlayers) {
        Team last = null;
        for (Team t : teams) {
            if (t.hasAlivePlayers(alivePlayers)) {
                if (last != null) return null; // More than 1 team alive
                last = t;
            }
        }
        return last;
    }

    /**
     * Restore all players' original max health.
     */
    public void restoreAllMaxHealth() {
        for (Map.Entry<UUID, Double> entry : originalMaxHealth.entrySet()) {
            Player p = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
                if (attr != null) {
                    attr.setBaseValue(entry.getValue());
                }
            }
        }
        originalMaxHealth.clear();
    }

    /**
     * Clear all team data (on game end/cleanup).
     */
    public void cleanup() {
        restoreAllMaxHealth();
        for (Team t : teams) {
            t.getMembers().clear();
        }
        playerTeamMap.clear();
    }

    /**
     * Get all teams (including empty ones up to maxTeams).
     */
    public List<Team> getAllTeams() {
        return teams;
    }

    private int getAllPlayerCount() {
        return (int) playerTeamMap.size() + teams.stream()
                .mapToInt(t -> (int) t.getMembers().stream()
                        .filter(uuid -> !playerTeamMap.containsKey(uuid))
                        .count())
                .sum();
    }
}
