package com.metrovice.rp.inventory;

import com.metrovice.rp.character.CharacterService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class InventoryCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final CharacterService characters;
    private final InventoryService inventories;

    public InventoryCommand(JavaPlugin plugin, CharacterService characters, InventoryService inventories) {
        this.plugin = plugin;
        this.characters = characters;
        this.inventories = inventories;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        try {
            var character = characters.active(player.getUniqueId()).orElse(null);
            if (character == null) {
                player.sendMessage(ChatColor.RED + "يجب اختيار شخصية أولاً.");
                return true;
            }
            inventories.open(player, character);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not open custom inventory: " + ex.getMessage());
            player.sendMessage(ChatColor.RED + "تعذر فتح الحقيبة حالياً.");
        }
        return true;
    }
}