package com.metrovice.rp.resourcepack;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ResourcePackService implements Listener {
    private static final Pattern SHA1 = Pattern.compile("^[a-f0-9]{40}$");
    private final JavaPlugin plugin;

    public ResourcePackService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.auto-send-on-join", true)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> send(event.getPlayer()), 40L);
    }

    public void send(Player player) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) {
            player.sendMessage("§cحزمة Metro Vice RP غير مفعّلة في إعدادات البلجن بعد.");
            return;
        }

        String url = plugin.getConfig().getString("resource-pack.url", "").trim();
        String hash = plugin.getConfig().getString("resource-pack.sha1", "").trim().toLowerCase(Locale.ROOT);
        if (url.isEmpty() || !url.startsWith("https://")) {
            player.sendMessage("§cرابط حزمة الموارد غير مضبوط. يجب إضافة رابط HTTPS مباشر لملف ZIP.");
            return;
        }
        if (!SHA1.matcher(hash).matches()) {
            player.sendMessage("§cبصمة SHA-1 غير صحيحة. يجب أن تتكون من 40 رمزًا سداسيًا صغيرًا.");
            return;
        }

        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        String prompt = plugin.getConfig().getString("resource-pack.prompt", "Metro Vice RP");
        try {
            player.setResourcePack(url, hash, required, Component.text(prompt));
            player.sendMessage("§bتم إرسال طلب تحميل حزمة Metro Vice RP.");
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not send resource pack to " + player.getName() + ": " + exception.getMessage());
            player.sendMessage("§cتعذر إرسال حزمة الموارد. راجع الرابط وبصمة SHA-1 في config.yml.");
        }
    }
}
