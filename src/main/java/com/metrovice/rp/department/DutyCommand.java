package com.metrovice.rp.department;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public final class DutyCommand implements CommandExecutor {
    private final MetroViceRP plugin;
    private final DepartmentService departments;

    public DutyCommand(MetroViceRP plugin, DepartmentService departments) {
        this.plugin = plugin; this.departments = departments;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        try {
            List<DepartmentService.Membership> memberships = departments.memberships(player.getUniqueId());
            if (memberships.isEmpty()) {
                player.sendMessage(MetroViceRP.color("&cشخصيتك ليست موظفة في الشرطة أو الإسعاف."));
                return true;
            }
            String department;
            if (args.length > 0) {
                department = departments.normalizeDepartment(args[0]);
            } else if (memberships.size() == 1) {
                department = memberships.get(0).department();
            } else {
                player.sendMessage(MetroViceRP.color("&eأنت مسجل في أكثر من قسم. استخدم /duty police أو /duty ems."));
                return true;
            }
            DepartmentService.Membership result = departments.toggleDuty(player.getUniqueId(), department);
            if (result.onDuty()) {
                player.sendMessage(MetroViceRP.color("&aبدأت مناوبتك في &f" + department + " &7- الرتبة: " + result.rank()));
            } else {
                player.sendMessage(MetroViceRP.color("&eأنهيت مناوبتك في &f" + department + "&e."));
            }
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
        } catch (SQLException ex) {
            plugin.getLogger().severe("Duty database error: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر تحديث حالة المناوبة."));
        }
        return true;
    }
}