package com.metrovice.rp;

import com.metrovice.rp.character.CharacterCommand;
import com.metrovice.rp.character.CharacterListener;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.character.CharacterStateService;
import com.metrovice.rp.economy.BankCommand;
import com.metrovice.rp.economy.BankService;
import com.metrovice.rp.storage.Database;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Objects;

public final class MetroViceRP extends JavaPlugin {
    private Database database;
    private CharacterService characterService;
    private CharacterStateService characterStates;
    private BankService bankService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            database = new Database(this);
            database.open();
            characterService = new CharacterService(this, database);
            characterStates = new CharacterStateService(database);
            bankService = new BankService(database, characterService);
        } catch (Exception exception) {
            getLogger().severe("Could not initialize MetroViceRP database: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("character")).setExecutor(new CharacterCommand(this, characterService, characterStates));
        Objects.requireNonNull(getCommand("mvrp")).setExecutor(this);
        Objects.requireNonNull(getCommand("bank")).setExecutor(new BankCommand(this, bankService));
        getServer().getPluginManager().registerEvents(new CharacterListener(this, characterService, characterStates), this);

        // Periodic persistence protects character inventories if the server stops unexpectedly.
        getServer().getScheduler().runTaskTimer(this, this::saveOnlineCharacters, 1200L, 1200L);
        getLogger().info("MetroViceRP core enabled. Character profiles and isolated player states are ready.");
    }

    @Override
    public void onDisable() {
        saveOnlineCharacters();
        if (database != null) database.close();
    }

    private void saveOnlineCharacters() {
        if (characterService == null || characterStates == null || database == null) return;
        for (Player player : getServer().getOnlinePlayers()) {
            try {
                characterService.active(player.getUniqueId()).ifPresent(profile -> {
                    try { characterStates.save(player, profile.id()); }
                    catch (SQLException ex) { getLogger().severe("Could not save " + profile.name() + ": " + ex.getMessage()); }
                });
            } catch (SQLException ex) {
                getLogger().severe("Could not find active character for " + player.getName() + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mvrp")) return false;
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(color("&8&m--------------------------------"));
            sender.sendMessage(color("&b&lMETRO &d&lVICE &fRP &7- Core 0.2.0"));
            sender.sendMessage(color("&7Character profiles: &aonline"));
            sender.sendMessage(color("&7Isolated inventory state: &aonline"));
            sender.sendMessage(color("&7Banking: &aonline"));
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