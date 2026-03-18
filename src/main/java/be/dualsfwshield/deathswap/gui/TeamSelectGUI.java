package be.dualsfwshield.deathswap.gui;

import be.dualsfwshield.deathswap.ConfigManager;
import be.dualsfwshield.deathswap.DeathSwapPlugin;
import be.dualsfwshield.deathswap.GameInstance;
import be.dualsfwshield.deathswap.TeamManager;
import be.dualsfwshield.deathswap.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for selecting a team in the lobby.
 * Displays wool blocks representing each team color.
 * Players click a wool to join that team.
 */
public class TeamSelectGUI implements Listener {

    private static final int INV_SIZE = 27; // 3 rows
    private static final int SLOT_LEAVE_TEAM = 22;
    private static final int SLOT_BACK = 26;

    private final DeathSwapPlugin plugin;

    public TeamSelectGUI(DeathSwapPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the team selection GUI for a player.
     */
    public void open(Player player, GameInstance arena) {
        if (arena == null || arena.getTeamManager() == null) return;

        TeamManager tm = arena.getTeamManager();
        ConfigManager.ArenaConfig config = arena.getConfig();

        Component title = Lang.getComponent("gui-team-select-title");
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);

        int playerCount = arena.getLobbyPlayers().size();
        List<TeamManager.Team> activeTeams = tm.getActiveTeams(playerCount);
        TeamManager.Team playerTeam = tm.getPlayerTeam(player);

        // Place wool blocks for each active team
        for (int i = 0; i < activeTeams.size() && i < 12; i++) {
            TeamManager.Team team = activeTeams.get(i);
            boolean isInThisTeam = team.equals(playerTeam);

            ItemStack wool = new ItemStack(team.getWoolMaterial());
            ItemMeta meta = wool.getItemMeta();

            // Team name with color
            String teamName = "&" + getColorCode(team.getTextColor()) + "&l" + team.getDisplayName();
            meta.displayName(GuiUtils.colorize(teamName).decoration(TextDecoration.ITALIC, false));

            // Lore with member count and list
            List<Component> lore = new ArrayList<>();
            lore.add(GuiUtils.colorize(Lang.get("gui-team-select-members", "%count%",
                    String.valueOf(team.size()))).decoration(TextDecoration.ITALIC, false));

            // List current members
            for (java.util.UUID uuid : team.getMembers()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    lore.add(Component.text("  • " + member.getName(), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }

            lore.add(Component.empty());
            if (isInThisTeam) {
                lore.add(GuiUtils.colorize(Lang.get("gui-team-select-current"))
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(GuiUtils.colorize(Lang.get("gui-team-select-click-join"))
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);

            // Enchant if player is in this team (glowing effect)
            if (isInThisTeam) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            wool.setItemMeta(meta);
            inv.setItem(i, wool);
        }

        // Leave team button
        inv.setItem(SLOT_LEAVE_TEAM, GuiUtils.createItem(Material.BARRIER,
                Lang.get("gui-team-select-leave"),
                Lang.get("gui-team-select-leave-lore")));

        // Back button
        inv.setItem(SLOT_BACK, GuiUtils.createItem(Material.ARROW,
                Lang.get("gui-team-select-back")));

        // Fill empty slots
        ItemStack filler = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INV_SIZE; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title() == null) return;

        Component expectedTitle = Lang.getComponent("gui-team-select-title");
        if (!expectedTitle.equals(event.getView().title())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= INV_SIZE) return;

        GameInstance arena = plugin.getArenaManager().findByLobbyWorld(player.getWorld().getName());
        if (arena == null || arena.getTeamManager() == null) return;

        TeamManager tm = arena.getTeamManager();

        if (slot == SLOT_BACK) {
            player.closeInventory();
            return;
        }

        if (slot == SLOT_LEAVE_TEAM) {
            tm.removeFromTeam(player);
            Lang.send(player, "gui-team-select-left");
            open(player, arena); // Refresh
            return;
        }

        // Check if clicked a team wool (slots 0-11)
        if (slot >= 0 && slot < 12) {
            int playerCount = arena.getLobbyPlayers().size();
            List<TeamManager.Team> activeTeams = tm.getActiveTeams(playerCount);
            if (slot < activeTeams.size()) {
                TeamManager.Team team = activeTeams.get(slot);
                boolean success = tm.assignPlayerToTeam(player, team);
                if (success) {
                    Lang.send(player, "gui-team-select-joined", "%team%", team.getDisplayName());
                    // Broadcast to lobby
                    arena.broadcastLobby(Lang.get("gui-team-select-joined-broadcast",
                            "%player%", player.getName(), "%team%", team.getDisplayName()));
                } else {
                    Lang.send(player, "gui-team-select-full");
                }
                open(player, arena); // Refresh
            }
        }
    }

    /**
     * Map NamedTextColor to legacy color code character.
     */
    private String getColorCode(NamedTextColor color) {
        if (color == NamedTextColor.RED) return "c";
        if (color == NamedTextColor.BLUE) return "9";
        if (color == NamedTextColor.GREEN) return "a";
        if (color == NamedTextColor.YELLOW) return "e";
        if (color == NamedTextColor.GOLD) return "6";
        if (color == NamedTextColor.DARK_PURPLE) return "5";
        if (color == NamedTextColor.AQUA) return "b";
        if (color == NamedTextColor.LIGHT_PURPLE) return "d";
        if (color == NamedTextColor.WHITE) return "f";
        return "7";
    }
}
