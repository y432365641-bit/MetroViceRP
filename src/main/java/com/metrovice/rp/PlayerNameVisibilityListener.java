package com.metrovice.rp;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hides account usernames from overhead name tags and the player list.
 * Character names remain available to the RP/MDT data services.
 */
public final class PlayerNameVisibilityListener implements Listener {
    private static final String TEAM_NAME = "mvrp_hidden";
    private final JavaPlugin plugin;

    public PlayerNameVisibilityListener(JavaPlugin plugin) {
        this.plugin = plugin;
        applyToOnlinePlayers();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) apply(event.getPlayer());
        });
    }

    private void applyToOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) apply(player);
    }

    private void apply(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(TEAM_NAME);
        if (team == null) team = board.registerNewTeam(TEAM_NAME);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.addEntry(player.getName());

        // Keep the tab entry neutral; Bukkit API does not expose a portable hide-listed toggle.
        player.setPlayerListName(" ");
    }
}
