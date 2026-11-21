package com.metrovice.rp.character;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public final class CharacterCommand implements CommandExecutor {
    private final MetroViceRP plugin;
    private final CharacterService service;

    public CharacterCommand(MetroViceRP plugin, CharacterService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (args.length == 0) {
            help(player); return true;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "create" -> {
                    if (args.length != 3) { player.sendMessage(MetroViceRP.color("&c/character create <name> <age>")); return true; }
                    int age;
                    try { age = Integer.parseInt(args[2]); }
                    catch (NumberFormatException ex) { player.sendMessage(MetroViceRP.color("&cالعمر لازم يكون رقم.")); return true; }
                    CharacterProfile profile = service.create(player.getUniqueId(), args[1], age);
                    player.sendMessage(MetroViceRP.color("&aتم إنشاء الشخصية &f" + profile.name() + " &7(ID: " + profile.id() + ")"));
                    player.sendMessage(MetroViceRP.color("&7استخدم &b/character select " + profile.id() + " &7لتفعيلها."));
                }
                case "list" -> {
                    List<CharacterProfile> profiles = service.list(player.getUniqueId());
                    if (profiles.isEmpty()) { player.sendMessage(MetroViceRP.color("&eما عندك شخصيات. أنشئ واحدة عبر /character create <name> <age>")); return true; }
                    player.sendMessage(MetroViceRP.color("&8&m-------- &bشخصياتك &8&m--------"));
                    for (CharacterProfile profile : profiles) {
                        boolean active = service.active(player.getUniqueId()).map(p -> p.id() == profile.id()).orElse(false);
                        player.sendMessage(MetroViceRP.color((active ? "&a● " : "&7○ ") + "&f" + profile.name() + " &7- العمر: " + profile.age() + " - ID: " + profile.id()));
                    }
                }
                case "select" -> {
                    if (args.length != 2) { player.sendMessage(MetroViceRP.color("&c/character select <id>")); return true; }
                    int id;
                    try { id = Integer.parseInt(args[1]); }
                    catch (NumberFormatException ex) { player.sendMessage(MetroViceRP.color("&cمعرّف الشخصية لازم يكون رقم.")); return true; }
                    service.select(player.getUniqueId(), id);
                    CharacterProfile profile = service.active(player.getUniqueId()).orElseThrow();
                    player.sendMessage(MetroViceRP.color("&aتم تفعيل شخصية &f" + profile.name() + "&a."));
                }
                case "info" -> {
                    CharacterProfile profile = service.active(player.getUniqueId()).orElse(null);
                    if (profile == null) player.sendMessage(MetroViceRP.color("&cما في شخصية مفعلة. استخدم /character list."));
                    else player.sendMessage(MetroViceRP.color("&bشخصيتك الحالية: &f" + profile.name() + " &7| العمر: " + profile.age() + " | ID: " + profile.id()));
                }
                default -> help(player);
            }
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
        } catch (SQLException ex) {
            plugin.getLogger().severe("Character database error: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cصار خطأ في قاعدة البيانات. راجع الإدارة."));
        }
        return true;
    }

    private void help(Player player) {
        player.sendMessage(MetroViceRP.color("&8&m------ &bMetro Vice RP &8&m------"));
        player.sendMessage(MetroViceRP.color("&b/character create <name> <age> &7- إنشاء شخصية"));
        player.sendMessage(MetroViceRP.color("&b/character list &7- عرض شخصياتك"));
        player.sendMessage(MetroViceRP.color("&b/character select <id> &7- اختيار شخصية"));
        player.sendMessage(MetroViceRP.color("&b/character info &7- الشخصية المفعلة"));
    }
}