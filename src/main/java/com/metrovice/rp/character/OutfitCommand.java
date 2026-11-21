package com.metrovice.rp.character;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public final class OutfitCommand implements CommandExecutor {
    private final MetroViceRP plugin;
    private final OutfitService outfits;

    public OutfitCommand(MetroViceRP plugin, OutfitService outfits) {
        this.plugin = plugin;
        this.outfits = outfits;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                List<String> names = outfits.list(player);
                player.sendMessage(MetroViceRP.color("&bأطقم شخصيتك: &f" + (names.isEmpty() ? "لا يوجد" : String.join("، ", names))));
                player.sendMessage(MetroViceRP.color("&7/outfit save <name> &8- حفظ الطقم الحالي"));
                player.sendMessage(MetroViceRP.color("&7/outfit load <name> &8- ارتداء طقم محفوظ"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(MetroViceRP.color("&c/outfit [list|save <name>|load <name>]"));
                return true;
            }
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            if (args[0].equalsIgnoreCase("save")) {
                outfits.save(player, name);
                player.sendMessage(MetroViceRP.color("&aتم حفظ الطقم لهذه الشخصية فقط."));
            } else if (args[0].equalsIgnoreCase("load")) {
                if (outfits.load(player, name)) player.sendMessage(MetroViceRP.color("&aتم ارتداء الطقم المحفوظ."));
                else player.sendMessage(MetroViceRP.color("&cهذا الطقم غير موجود لهذه الشخصية."));
            } else {
                player.sendMessage(MetroViceRP.color("&c/outfit [list|save <name>|load <name>]"));
            }
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
        } catch (SQLException | IOException | ClassNotFoundException ex) {
            plugin.getLogger().severe("Outfit operation failed: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر تنفيذ عملية الملابس."));
        }
        return true;
    }
}
