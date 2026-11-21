package com.metrovice.rp.character;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public final class CharacterCommand implements CommandExecutor {
    private final MetroViceRP plugin;
    private final CharacterService service;
    private final CharacterStateService states;

    public CharacterCommand(MetroViceRP plugin, CharacterService service, CharacterStateService states) {
        this.plugin = plugin; this.service = service; this.states = states;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (args.length == 0) { help(player); return true; }
        try {
            switch (args[0].toLowerCase()) {
                case "create" -> create(player, args);
                case "menu" -> CharacterMenu.open(plugin, service, player);
                case "list" -> list(player);
                case "select" -> select(player, args);
                case "info" -> info(player);
                case "rename" -> rename(player, args);
                case "delete" -> delete(player, args);
                default -> help(player);
            }
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
        } catch (SQLException ex) {
            plugin.getLogger().severe("Character database error: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cصار خطأ في حفظ بيانات الشخصية. راجع الإدارة."));
        }
        return true;
    }

    private void create(Player player, String[] args) throws SQLException {
        if (args.length < 3) { player.sendMessage(MetroViceRP.color("&c/character create <name> <age>")); return; }
        int age;
        try { age = Integer.parseInt(args[args.length - 1]); }
        catch (NumberFormatException ex) { player.sendMessage(MetroViceRP.color("&cاكتب العمر كرقم في آخر الأمر.")); return; }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 1));
        CharacterProfile profile = service.create(player.getUniqueId(), name, age);
        player.sendMessage(MetroViceRP.color("&aتم إنشاء الشخصية &f" + profile.name() + " &7(ID: " + profile.id() + ")"));
        player.sendMessage(MetroViceRP.color("&7فعّلها باستخدام &b/character select " + profile.id()));
    }

    private void list(Player player) throws SQLException {
        List<CharacterProfile> profiles = service.list(player.getUniqueId());
        if (profiles.isEmpty()) {
            player.sendMessage(MetroViceRP.color("&eما عندك شخصيات. أنشئ واحدة عبر /character create <name> <age>"));
            return;
        }
        int activeId = service.active(player.getUniqueId()).map(CharacterProfile::id).orElse(-1);
        player.sendMessage(MetroViceRP.color("&8&m-------- &bشخصياتك &8&m--------"));
        for (CharacterProfile profile : profiles) {
            boolean active = activeId == profile.id();
            player.sendMessage(MetroViceRP.color((active ? "&a● " : "&7○ ") + "&f" + profile.name()
                    + " &7| العمر: " + profile.age() + " | الوظيفة: " + profile.job()
                    + " | ID: " + profile.id()));
        }
    }

    private void select(Player player, String[] args) throws SQLException {
        if (args.length != 2) { player.sendMessage(MetroViceRP.color("&c/character select <id>")); return; }
        int id = parseId(args[1]);
        CharacterProfile target = service.find(player.getUniqueId(), id)
                .orElseThrow(() -> new IllegalArgumentException("هذه الشخصية غير موجودة في حسابك."));
        CharacterProfile previous = service.active(player.getUniqueId()).orElse(null);
        if (previous != null && previous.id() == target.id()) {
            player.sendMessage(MetroViceRP.color("&eهذه الشخصية مفعلة بالفعل."));
            return;
        }
        if (previous != null) states.save(player, previous.id());
        service.select(player.getUniqueId(), target.id());
        states.load(player, target.id());
        player.sendMessage(MetroViceRP.color("&aتم التبديل إلى شخصية &f" + target.name()));
        player.sendMessage(MetroViceRP.color("&7تم تحميل مخزونها ودرعها وخبرتها وحالتها المحفوظة."));
    }

    private void info(Player player) throws SQLException {
        CharacterProfile p = service.active(player.getUniqueId()).orElse(null);
        if (p == null) {
            player.sendMessage(MetroViceRP.color("&cما في شخصية مفعلة. استخدم /character list."));
            return;
        }
        player.sendMessage(MetroViceRP.color("&8&m-------- &bملف الشخصية &8&m--------"));
        player.sendMessage(MetroViceRP.color("&fالاسم: &b" + p.name() + " &7| ID: " + p.id()));
        player.sendMessage(MetroViceRP.color("&fالعمر: &b" + p.age() + " &7| الوظيفة: &b" + p.job()));
        player.sendMessage(MetroViceRP.color("&fالرتبة: &b" + p.rank() + " &7| النقد: &a$" + p.cash()));
        player.sendMessage(MetroViceRP.color("&fالبنك: &a$" + p.bank()));
    }

    private void rename(Player player, String[] args) throws SQLException {
        if (args.length < 3) { player.sendMessage(MetroViceRP.color("&c/character rename <id> <new name>")); return; }
        int id = parseId(args[1]);
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        service.rename(player.getUniqueId(), id, name);
        player.sendMessage(MetroViceRP.color("&aتم تعديل اسم الشخصية."));
    }

    private void delete(Player player, String[] args) throws SQLException {
        if (args.length != 3 || !args[2].equalsIgnoreCase("CONFIRM")) {
            player.sendMessage(MetroViceRP.color("&cللحذف النهائي اكتب: /character delete <id> CONFIRM"));
            return;
        }
        int id = parseId(args[1]);
        boolean wasActive = service.active(player.getUniqueId()).map(p -> p.id() == id).orElse(false);
        if (wasActive) states.clearPlayerState(player);
        service.delete(player.getUniqueId(), id);
        player.sendMessage(MetroViceRP.color("&aتم حذف الشخصية وبياناتها المحفوظة."));
    }

    private int parseId(String raw) {
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("معرّف الشخصية يجب أن يكون رقمًا."); }
    }

    private void help(Player player) {
        player.sendMessage(MetroViceRP.color("&8&m------ &bMetro Vice RP &8&m------"));
        player.sendMessage(MetroViceRP.color("&b/character create <name> <age> &7- إنشاء شخصية"));
        player.sendMessage(MetroViceRP.color("&b/character list &7- عرض شخصياتك"));
        player.sendMessage(MetroViceRP.color("&b/character menu &7- فتح قائمة الشخصيات"));
        player.sendMessage(MetroViceRP.color("&b/character select <id> &7- تبديل الشخصية"));
        player.sendMessage(MetroViceRP.color("&b/character info &7- عرض ملف الشخصية"));
        player.sendMessage(MetroViceRP.color("&b/character rename <id> <name> &7- تغيير الاسم"));
        player.sendMessage(MetroViceRP.color("&b/character delete <id> CONFIRM &7- حذف الشخصية"));
    }
}