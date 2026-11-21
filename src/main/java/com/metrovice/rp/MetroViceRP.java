package com.metrovice.rp;

import com.metrovice.rp.character.CharacterCommand;
import com.metrovice.rp.character.CharacterListener;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.character.CharacterStateService;
import com.metrovice.rp.character.OutfitCommand;
import com.metrovice.rp.character.OutfitService;
import com.metrovice.rp.economy.BankCommand;
import com.metrovice.rp.economy.BankService;
import com.metrovice.rp.economy.BankNpcListener;
import com.metrovice.rp.department.DepartmentService;
import com.metrovice.rp.department.DutyCommand;
import com.metrovice.rp.police.MdtCommand;
import com.metrovice.rp.police.PoliceLaptopListener;
import com.metrovice.rp.police.PoliceRankLaptopService;
import com.metrovice.rp.police.MdtMenuService;
import com.metrovice.rp.police.MdtMenuListener;
import com.metrovice.rp.storage.Database;
import com.metrovice.rp.resourcepack.ResourcePackService;
import com.metrovice.rp.resourcepack.BedrockResourcePackManager;
import com.metrovice.rp.device.DeviceService;
import com.metrovice.rp.ems.EmergencyService;
import org.bukkit.inventory.ItemStack;
import com.metrovice.rp.inventory.InventoryCommand;
import com.metrovice.rp.inventory.InventoryListener;
import com.metrovice.rp.inventory.InventoryService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
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
    private ResourcePackService resourcePackService;
    private InventoryService inventoryService;
    private DeviceService deviceService;
    private EmergencyService emergencyService;
    private PoliceRankLaptopService policeRankLaptopService;
    private Path bedrockPackPath;

    @Override
    public void onLoad() {
        super.onLoad();
        // Geyser is a declared dependency, so its plugin is loaded first.
        // Install custom item mappings before Geyser enables, but keep the pack
        // under MetroViceRP's own data folder for API-based registration.
        bedrockPackPath = com.metrovice.rp.resourcepack.EmbeddedGeyserPack.install(this);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            BedrockResourcePackManager bedrockPacks = new BedrockResourcePackManager(this, bedrockPackPath);
            bedrockPacks.register();
        } catch (Exception exception) {
            getLogger().severe("Could not register MetroViceRP Bedrock pack with Geyser: " + exception.getMessage());
            getLogger().severe("Bedrock pack delivery is unavailable until the Geyser API registration issue is fixed.");
        }

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
        OutfitService outfitService = new OutfitService(database, characterService);
        Objects.requireNonNull(getCommand("outfit")).setExecutor(new OutfitCommand(this, outfitService));
        inventoryService = new InventoryService(this, database);
        Objects.requireNonNull(getCommand("inventory")).setExecutor(new InventoryCommand(this, characterService, inventoryService));
        getServer().getPluginManager().registerEvents(new InventoryListener(this, inventoryService, characterService), this);
        Objects.requireNonNull(getCommand("mvrp")).setExecutor(this);
        Objects.requireNonNull(getCommand("bank")).setExecutor(new BankCommand(this, bankService));
        bankNpcListener = new BankNpcListener(this, bankService);
        getServer().getPluginManager().registerEvents(bankNpcListener, this);
        bankNpcListener.restoreEmployee();
        Objects.requireNonNull(getCommand("duty")).setExecutor(new DutyCommand(this, departmentService));
        MdtMenuService mdtMenus = new MdtMenuService(this, characterService, departmentService);
        Objects.requireNonNull(getCommand("mdt")).setExecutor(new MdtCommand(this, characterService, departmentService, mdtMenus));
        getServer().getPluginManager().registerEvents(new MdtMenuListener(this, mdtMenus, departmentService), this);
        deviceService = new DeviceService(this, characterService, departmentService);
        getServer().getPluginManager().registerEvents(deviceService, this);
        emergencyService = new EmergencyService(this, departmentService, characterService, inventoryService);
        getServer().getPluginManager().registerEvents(emergencyService, this);
        Objects.requireNonNull(getCommand("emsbaggive")).setExecutor(emergencyService);
        Objects.requireNonNull(getCommand("radio")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) { sender.sendMessage("This command can only be used in-game."); return true; }
            return deviceService.sendRadioMessage(player, String.join(" ", args));
        });
        resourcePackService = new ResourcePackService(this);
        getServer().getPluginManager().registerEvents(resourcePackService, this);
        policeLaptop = new PoliceLaptopListener(this);
        getServer().getPluginManager().registerEvents(policeLaptop, this);
        policeRankLaptopService = new PoliceRankLaptopService(this, departmentService);
        getServer().getPluginManager().registerEvents(policeRankLaptopService, this);
        Objects.requireNonNull(getCommand("ranklaptopgive")).setExecutor(policeRankLaptopService);
        getServer().getPluginManager().registerEvents(new CharacterListener(this, characterService, characterStates, departmentService), this);
        getServer().getPluginManager().registerEvents(new PlayerNameVisibilityListener(this), this);

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
            sender.sendMessage(color("&b&lMETRO &d&lVICE &fRP &7- Core 0.9.2"));
            sender.sendMessage(color("&7Character profiles: &aonline"));
            sender.sendMessage(color("&7Isolated inventory state: &aonline"));
            sender.sendMessage(color("&7Custom bag: &ephase 1"));
            sender.sendMessage(color("&7Banking: &aonline"));
            sender.sendMessage(color("&7Police / EMS duty: &aonline"));
            sender.sendMessage(color("&7Phone / Emergency system: &aonline"));
            sender.sendMessage(color("&7Police rank laptop: &aLieutenant+"));
            sender.sendMessage(color("&7Bedrock resource pack: &aGeyser API"));
            sender.sendMessage(color("&7Storage: &aSQLite"));
            sender.sendMessage(color("&8&m--------------------------------"));
            return true;
        }
        if (args[0].equalsIgnoreCase("pack")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command must be used in-game.");
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("send") && args.length == 3) {
                if (!sender.hasPermission("metrovice.admin")) {
                    sender.sendMessage(color("&cليس لديك صلاحية."));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(color("&cاللاعب غير متصل."));
                    return true;
                }
                resourcePackService.send(target);
                sender.sendMessage(color("&aتم إرسال طلب الحزمة إلى " + target.getName()));
                return true;
            }
            resourcePackService.send(player);
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
                sender.sendMessage(color("&aتم تحديث بيانات شخصية " + target.getName() + "."));
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
        if (args[0].equalsIgnoreCase("givephone") || args[0].equalsIgnoreCase("giveradio")) {
            if (!sender.hasPermission("metrovice.admin")) { sender.sendMessage(color("&cليس لديك صلاحية.")); return true; }
            if (args.length != 2) { sender.sendMessage(color("&c/mvrp givephone <player> أو /mvrp giveradio <player>")); return true; }
            Player targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) { sender.sendMessage(color("&cاللاعب يجب أن يكون متصلًا.")); return true; }
            ItemStack device = args[0].equalsIgnoreCase("givephone") ? deviceService.createPhone() : deviceService.createRadio();
            var leftovers = targetPlayer.getInventory().addItem(device);
            if (!leftovers.isEmpty()) { sender.sendMessage(color("&cمخزون اللاعب ممتلئ.")); return true; }
            sender.sendMessage(color("&aتم تسليم الجهاز إلى " + targetPlayer.getName()));
            targetPlayer.sendMessage(color("&bاستلمت جهاز Metro Vice RP جديدًا."));
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
                sender.sendMessage(color("&cاللاعب غير متصل."));
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
        sender.sendMessage(color("&cالاستخدام: /mvrp [info|pack|pack send <player>|reload|setjob|setrank|hirepolice|hireems|firepolice|fireems|dutylist|givelaptop|givephone|giveradio|banknpc|mdt|radio]"));
        return true;
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
