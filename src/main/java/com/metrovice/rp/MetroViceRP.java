package com.metrovice.rp;

import com.metrovice.rp.character.CharacterCommand;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.storage.Database;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class MetroViceRP extends JavaPlugin {
    private Database database;
    private CharacterService characterService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            database = new Database(this);
            database.open();
            characterService = new CharacterService(this, database);
        } catch (Exception exception) {
            getLogger().severe("Could not initialize MetroViceRP database: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("character")).setExecutor(new CharacterCommand(this, characterService));
        Objects.requireNonNull(getCommand("mvrp")).setExecutor(this);
        getLogger().info("MetroViceRP core enabled. Character storage is ready.");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mvrp")) return false;
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(color("&8&m--------------------------------"));
            sender.sendMessage(color("&b&lMETRO &d&lVICE &fRP &7- Core 0.1.0"));
            sender.sendMessage(color("&7Character framework: &aonline"));
            sender.sendMessage(color("&7Storage: &aSQLite"));
            sender.sendMessage(color("&8&m--------------------------------"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("metrovice.admin")) {
                sender.sendMessage(color("&cليس لديك صلاحية."));
                return true;
            }
            reloadConfig();
            sender.sendMessage(color("&aتم تحديث إعدادات MetroViceRP."));
            return true;
        }
        sender.sendMessage(color("&cالاستخدام: /mvrp [info|reload]"));
        return true;
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}