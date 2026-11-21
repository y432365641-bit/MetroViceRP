package com.metrovice.rp.economy;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public final class BankCommand implements CommandExecutor {
    private final MetroViceRP plugin;
    private final BankService bank;

    public BankCommand(MetroViceRP plugin, BankService bank) {
        this.plugin = plugin; this.bank = bank;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
                BankService.Balance b = bank.balance(player.getUniqueId());
                player.sendMessage(MetroViceRP.color("&8&m------ &bحسابك البنكي &8&m------"));
                player.sendMessage(MetroViceRP.color("&fالنقد: &a$" + b.cash()));
                player.sendMessage(MetroViceRP.color("&fالبنك: &b$" + b.bank()));
                return true;
            }
            if (args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("withdraw")) {
                if (args.length != 2) { help(player); return true; }
                long amount = parseAmount(args[1]);
                if (args[0].equalsIgnoreCase("deposit")) {
                    bank.deposit(player.getUniqueId(), amount);
                    player.sendMessage(MetroViceRP.color("&aتم إيداع $" + amount + " في البنك."));
                } else {
                    bank.withdraw(player.getUniqueId(), amount);
                    player.sendMessage(MetroViceRP.color("&aتم سحب $" + amount + " من البنك."));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("send")) {
                if (args.length != 3) { help(player); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { player.sendMessage(MetroViceRP.color("&cاللاعب لازم يكون متصلًا بالسيرفر.")); return true; }
                long amount = parseAmount(args[2]);
                bank.transfer(player.getUniqueId(), target.getUniqueId(), amount);
                player.sendMessage(MetroViceRP.color("&aتم تحويل $" + amount + " إلى &f" + target.getName()));
                target.sendMessage(MetroViceRP.color("&aوصلك تحويل بنكي بقيمة $" + amount + " من &f" + player.getName()));
                return true;
            }
            help(player);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
        } catch (SQLException ex) {
            plugin.getLogger().severe("Bank database error: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر تنفيذ العملية البنكية. حاول لاحقًا."));
        }
        return true;
    }

    private long parseAmount(String raw) {
        try { return Long.parseLong(raw); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("اكتب مبلغًا صحيحًا."); }
    }

    private void help(Player player) {
        player.sendMessage(MetroViceRP.color("&8&m------ &bالبنك &8&m------"));
        player.sendMessage(MetroViceRP.color("&b/bank balance &7- عرض الرصيد"));
        player.sendMessage(MetroViceRP.color("&b/bank deposit <amount> &7- إيداع"));
        player.sendMessage(MetroViceRP.color("&b/bank withdraw <amount> &7- سحب"));
        player.sendMessage(MetroViceRP.color("&b/bank send <player> <amount> &7- تحويل للاعب متصل"));
    }
}