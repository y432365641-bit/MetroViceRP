package com.metrovice.rp;

import com.metrovice.rp.character.CharacterCommand;
import com.metrovice.rp.character.CharacterListener;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.character.CharacterStateService;
import com.metrovice.rp.economy.BankCommand;
import com.metrovice.rp.economy.BankService;
import com.metrovice.rp.department.DepartmentService;
import com.metrovice.rp.department.DutyCommand;
import com.metrovice.rp.storage.Database;
import org.bukkit.Bukkit;
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
    private DepartmentService departmentService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            database = new Database(this);
            database.open();
            characterService = new CharacterService(this, database);
            characterStates = new CharacterStateService(database);
            bankService = new BankService(database, characterService);
            departmentService = new DepartmentService(database, characterService);
        } catch (Exception exception) {
            getLogger().severe("Could not initialize MetroViceRP database: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("character")).setExecutor(new CharacterCommand(this, characterService, characterStates));
        Objects.requireNonNull(getCommand("mvrp")).setExecutor(this);
        Objects.requireNonNull(getCommand("bank")).setExecutor(new BankCommand(this, bankService));
        Objects.requireNonNull(getCommand("duty")).setExecutor(new DutyCommand(this, departmentService));
        getServer().getPluginManager().registerEvents(new CharacterListener(this, characterService, characterStates, departmentService), this);

        // Periodic persistence protects character inventories if the server stops unexpectedly.
        getServer().getScheduler().runTaskTimer(this, this::saveOnlineCharacters, 1200L, 1200L);
        getLogger().info("MetroViceRP core enabled. Character profiles and isolated player states are ready.");
    }

    @Override
    public void onDisable() {
        if (departmentService != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                try { departmentService.endAllDuty(player.getUniqueId()); }
                catch (SQLException ex) { getLogger().severe("Could not close duty session: " + ex.getMessage()); }
            }
        }
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
            sender.sendMessage(color("&b&lMETRO &d&lVICE &fRP &7- Core 0.3.0"));
            sender.sendMessage(color("&7Character profiles: &aonline"));
            sender.sendMessage(color("&7Isolated inventory state: &aonline"));
            sender.sendMessage(color("&7Banking: &aonline"));
            sender.sendMessage(color("&7Police / EMS duty: &aonline"));
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
        if (args[0].equalsIgnoreCase("setjob") || args[0].equalsIgnoreCase("setrank")) {
            if (!sender.hasPermission("metrovice.admin")) {
                sender.sendMessage(color("&cليس لديك صلاحية."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(color("&c/mvrp setjob <player> <job>"));
                sender.sendMessage(color("&c/mvrp setrank <player> <rank>"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cاللاعب يجب أن يكون متصلًا."));
                return true;
            }
            try {
                var profile = characterService.active(target.getUniqueId()).orElse(null);
                if (profile == null) {
                    sender.sendMessage(color("&cاللاعب لا يملك شخصية مفعلة."));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                if (args[0].equalsIgnoreCase("setjob")) characterService.setJob(target.getUniqueId(), profile.id(), value);
                else characterService.setRank(target.getUniqueId(), profile.id(), value);
                sender.sendMessage(color("&aتم تحديث ملف شخصية " + target.getName() + "."));
                target.sendMessage(color("&aتم تحديث بيانات وظيفتك/رتبتك من الإدارة."));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(color("&c" + ex.getMessage()));
            } catch (SQLException ex) {
                getLogger().severe("Could not update character profile: " + ex.getMessage());
                sender.sendMessage(color("&cتعذر تحديث بيانات الشخصية."));
            }
            return true;
        }
        sender.sendMessage(color("&cالاستخدام: /mvrp [info|reload|setjob|setrank]"));
        return true;
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}