package com.metrovice.rp.character;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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

    @EventHandler
    public void onCharacterMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof CharacterMenuHolder holder)) return;
        event.setCancelled(true);
        if (!holder.owner().equals(player.getUniqueId())) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        Integer id = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "character_id"), PersistentDataType.INTEGER);
        if (id == null) return;

        try {
            CharacterProfile target = characters.find(player.getUniqueId(), id)
                    .orElseThrow(() -> new IllegalArgumentException("هذه الشخصية ليست ملكك."));
            CharacterProfile previous = characters.active(player.getUniqueId()).orElse(null);
            if (previous != null && previous.id() == target.id()) {
                player.sendMessage(MetroViceRP.color("&eهذه الشخصية مفعلة بالفعل."));
                return;
            }
            if (previous != null) states.save(player, previous.id());
            characters.select(player.getUniqueId(), target.id());
            player.closeInventory();
            states.load(player, target.id());
            player.sendMessage(MetroViceRP.color("&aتم التبديل إلى &f" + target.name()));
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
        } catch (SQLException ex) {
            plugin.getLogger().severe("Character menu error: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر تبديل الشخصية بسبب خطأ في قاعدة البيانات."));
        }
    }

    @EventHandler
    public void onCharacterMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CharacterMenuHolder) event.setCancelled(true);
    }
}