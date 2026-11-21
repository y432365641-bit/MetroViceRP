package com.metrovice.rp;

import com.metrovice.rp.character.CharacterCommand;
import com.metrovice.rp.character.CharacterListener;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.character.CharacterStateService;
import com.metrovice.rp.economy.BankCommand;
import com.metrovice.rp.economy.BankService;
import com.metrovice.rp.economy.BankNpcListener;
import com.metrovice.rp.department.DepartmentService;
import com.metrovice.rp.department.DutyCommand;
import com.metrovice.rp.police.MdtCommand;
import com.metrovice.rp.police.PoliceLaptopListener;
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
    private BankNpcListener bankNpcListener;
    private DepartmentService departmentService;
    private PoliceLaptopListener policeLaptop;

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
        bankNpcListener = new BankNpcListener(this, bankService);
        getServer().getPluginManager().registerEvents(bankNpcListener, this);
        Objects.requireNonNull(getCommand("duty")).setExecutor(new DutyCommand(this, departmentService));
        Objects.requireNonNull(getCommand("mdt")).setExecutor(new MdtCommand(this, characterService, departmentService));
        policeLaptop = new PoliceLaptopListener(this);
        getServer().getPluginManager().registerEvents(policeLaptop, this);
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
            sender.sendMessage(color("&b&lMETRO &d&lVICE &fRP &7- Core 0.5.0"));
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
        if (args[0].equalsIgnoreCase("hirepolice") || args[0].equalsIgnoreCase("hireems")
                || args[0].equalsIgnoreCase("firepolice") || args[0].equalsIgnoreCase("fireems")) {
            if (!sender.hasPermission("metrovice.admin")) {
                sender.sendMessage(color("&cليس لديك صلاحية."));
                return true;
            }
            boolean hiring = args[0].toLowerCase().startsWith("hire");
            if (args.length < (hiring ? 3 : 2)) {
                sender.sendMessage(color("&c/mvrp hirepolice <player> <rank>"));
                sender.sendMessage(color("&c/mvrp hireems <player> <rank>"));
                sender.sendMessage(color("&c/mvrp firepolice <player>"));
                sender.sendMessage(color("&c/mvrp fireems <player>"));
                return true;
            }
            Player targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(color("&cاللاعب يجب أن يكون متصلًا."));
                return true;
            }
            String department = args[0].toLowerCase().contains("police") ? "POLICE" : "EMS";
            try {
                if (hiring) {
                    String rank = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    if (department.equals("POLICE") && !getConfig().getStringList("departments.police-ranks").contains(rank)) {
                        sender.sendMessage(color("&cالرتبة غير موجودة في قائمة رتب الشرطة بالإعدادات."));
                        return true;
                    }
                    departmentService.hire(targetPlayer.getUniqueId(), department, rank);
                    sender.sendMessage(color("&aتم تسجيل " + targetPlayer.getName() + " في قسم " + department + " برتبة " + rank));
                    targetPlayer.sendMessage(color("&aتم تعيين شخصيتك في قسم " + department + " برتبة " + rank));
                } else {
                    departmentService.fire(targetPlayer.getUniqueId(), department);
                    sender.sendMessage(color("&eتم إنهاء خدمة شخصية " + targetPlayer.getName() + " من قسم " + department));
                    targetPlayer.sendMessage(color("&eتم إنهاء خدمتك من قسم " + department));
                }
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(color("&c" + ex.getMessage()));
            } catch (SQLException ex) {
                getLogger().severe("Department administration error: " + ex.getMessage());
                sender.sendMessage(color("&cتعذر تحديث القسم."));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("banknpc")) {
            if (!sender.hasPermission("metrovice.admin")) { sender.sendMessage(color("&cليس لديك صلاحية.")); return true; }
            if (!(sender instanceof Player player)) { sender.sendMessage("Only an in-game player can place the bank employee."); return true; }
            bankNpcListener.spawnEmployee(player.getLocation());
            player.sendMessage(color("&aتم إنشاء موظف البنك في موقعك. اضغط عليه لفتح حسابك."));
            return true;
        }
        if (args[0].equalsIgnoreCase("givelaptop")) {
            if (!sender.hasPermission("metrovice.admin")) {
                sender.sendMessage(color("&cليس لديك صلاحية."));
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(color("&c/mvrp givelaptop <player>"));
                return true;
            }
            Player targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(color("&cاللاعب يجب أن يكون متصلًا."));
                return true;
            }
            var leftovers = targetPlayer.getInventory().addItem(policeLaptop.createLaptop());
            if (!leftovers.isEmpty()) {
                sender.sendMessage(color("&cمخزون اللاعب ممتلئ."));
                return true;
            }
            sender.sendMessage(color("&aتم تسليم جهاز MDT إلى " + targetPlayer.getName()));
            targetPlayer.sendMessage(color("&9استلمت جهاز MDT الخاص بالشرطة."));
            return true;
        }
        if (args[0].equalsIgnoreCase("dutylist")) {
            if (args.length != 2) {
                sender.sendMessage(color("&c/mvrp dutylist <police|ems>"));
                return true;
            }
            try {
                String department = departmentService.normalizeDepartment(args[1]);
                var members = departmentService.onDuty(department);
                if (department.equals("POLICE")) {
                    var rankOrder = getConfig().getStringList("departments.police-ranks");
                    members.sort(java.util.Comparator.comparingInt(member -> {
                        int index = rankOrder.indexOf(member.rank());
                        return index < 0 ? Integer.MAX_VALUE : index;
                    }));
                }
                sender.sendMessage(color("&8&m------ &b" + department + " - المناوبون &8&m------"));
                if (members.isEmpty()) sender.sendMessage(color("&7لا يوجد أفراد على رأس عملهم حاليًا."));
                for (var member : members) {
                    long hours = member.dutySeconds() / 3600;
                    long minutes = (member.dutySeconds() % 3600) / 60;
                    sender.sendMessage(color("&f" + member.characterName() + " &7- " + member.rank()
                            + " &8| &b" + hours + "h " + minutes + "m"));
                }
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(color("&c" + ex.getMessage()));
            } catch (SQLException ex) {
                getLogger().severe("Duty roster error: " + ex.getMessage());
                sender.sendMessage(color("&cتعذر جلب قائمة المناوبين."));
            }
            return true;
        }
        sender.sendMessage(color("&cالاستخدام: /mvrp [info|reload|setjob|setrank|hirepolice|hireems|firepolice|fireems|dutylist|givelaptop|banknpc|mdt]"));
        return true;
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}