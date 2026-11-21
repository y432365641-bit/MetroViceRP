package com.metrovice.rp.character;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;

public final class CharacterListener implements Listener {
    private final MetroViceRP plugin;
    private final CharacterService characters;
    private final CharacterStateService states;

    public CharacterListener(MetroViceRP plugin, CharacterService characters, CharacterStateService states) {
        this.plugin = plugin; this.characters = characters; this.states = states;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                characters.active(player.getUniqueId()).ifPresent(profile -> {
                    try { states.load(player, profile.id()); }
                    catch (SQLException ex) { plugin.getLogger().severe("Could not load character state: " + ex.getMessage()); }
                });
            } catch (SQLException ex) {
                plugin.getLogger().severe("Could not find active character: " + ex.getMessage());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        try {
            characters.active(player.getUniqueId()).ifPresent(profile -> {
                try { states.save(player, profile.id()); }
                catch (SQLException ex) { plugin.getLogger().severe("Could not save character state: " + ex.getMessage()); }
            });
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not resolve active character on quit: " + ex.getMessage());
        }
    }
}