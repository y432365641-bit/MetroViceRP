package com.metrovice.rp.police;

import com.metrovice.rp.MetroViceRP;
import com.metrovice.rp.character.CharacterProfile;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.department.DepartmentService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public final class MdtCommand implements CommandExecutor {
    private final MetroViceRP plugin;
    private final CharacterService characters;
    private final DepartmentService departments;

    public MdtCommand(MetroViceRP plugin, CharacterService characters, DepartmentService departments) {
        this.plugin = plugin; this.characters = characters; this.departments = departments;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        try {
            if (!departments.isOnDuty(player.getUniqueId(), "POLICE")) {
                player.sendMessage(MetroViceRP.color("&cنظام MDT متاح لأفراد الشرطة أثناء المناوبة فقط."));
                return true;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("roster")) {
                List<DepartmentService.DutyMember> roster = departments.onDuty("POLICE");
                player.sendMessage(MetroViceRP.color("&8&m------ &bPolice MDT | المناوبون &8&m------"));
                if (roster.isEmpty()) player.sendMessage(MetroViceRP.color("&7لا يوجد أفراد مناوبون."));
                for (DepartmentService.DutyMember member : roster) {
                    player.sendMessage(MetroViceRP.color("&f" + member.characterName() + " &7- " + member.rank()));
                }
                player.sendMessage(MetroViceRP.color("&7للبحث عن هوية: &b/mdt search <name>"));
                return true;
            }
            if (args[0].equalsIgnoreCase("search")) {
                if (args.length < 2) {
                    player.sendMessage(MetroViceRP.color("&c/mdt search <character name>"));
                    return true;
                }
                String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                List<CharacterProfile> matches = characters.searchByName(query);
                player.sendMessage(MetroViceRP.color("&8&m------ &bنتائج البحث &8&m------"));
                if (matches.isEmpty()) player.sendMessage(MetroViceRP.color("&7لا توجد نتائج."));
                for (CharacterProfile profile : matches) {
                    player.sendMessage(MetroViceRP.color("&b" + profile.name() + " &7| العمر: &f" + profile.age()
                            + " &7| الوظيفة: &f" + profile.job() + " &7| الرتبة: &f" + profile.rank()
                            + " &8| ID: " + profile.id()));
                }
                return true;
            }
            player.sendMessage(MetroViceRP.color("&c/mdt [roster|search <name>]"));
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
        } catch (SQLException ex) {
            plugin.getLogger().severe("Police MDT database error: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر فتح سجلات MDT."));
        }
        return true;
    }
}