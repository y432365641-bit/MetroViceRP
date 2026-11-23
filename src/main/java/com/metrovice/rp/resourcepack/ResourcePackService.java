package com.metrovice.rp.resourcepack;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.api.GeyserApi;

public final class ResourcePackService implements Listener {
    private final JavaPlugin plugin;
    private final EmbeddedJavaPackServer javaPackServer;

    public ResourcePackService(JavaPlugin plugin, EmbeddedJavaPackServer javaPackServer) {
        this.plugin = plugin;
        this.javaPackServer = javaPackServer;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.auto-send-on-join", true)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) send(event.getPlayer());
        }, 40L);
    }

    public void send(Player player) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) {
            player.sendMessage("§cحزمة Metro Vice RP غير مفعّلة في إعدادات البلجن.");
            return;
        }

        // Bedrock clients receive the separate .mcpack through Geyser's pack
        // handshake. Never send the Java ZIP to a Bedrock client.
        GeyserApi geyser = GeyserApi.api();
        if (geyser == null) {
            plugin.getLogger().warning("Geyser API is not ready; resource pack was not sent to " + player.getName());
            player.sendMessage("§cنظام حزمة الموارد غير جاهز حالياً. حاول لاحقاً.");
            return;
        }
        if (geyser.isBedrockPlayer(player.getUniqueId())) {
            player.sendMessage("§bحزمة Bedrock تُدار تلقائياً عبر Geyser عند دخولك.");
            return;
        }

        if (javaPackServer == null || !javaPackServer.isReady()) {
            player.sendMessage("§cحزمة Java غير جاهزة. راجع إعداد public-url ومنفذ الحزمة في config.yml.");
            return;
        }

        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        String prompt = plugin.getConfig().getString("resource-pack.prompt", "Metro Vice RP");
        try {
            player.setResourcePack(javaPackServer.downloadUrl(), javaPackServer.sha1(),
                    required, Component.text(prompt));
            player.sendMessage("§bتم إرسال طلب تحميل حزمة Metro Vice RP.");
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not send resource pack to " + player.getName() + ": "
                    + exception.getMessage());
            player.sendMessage("§cتعذر إرسال طلب الحزمة. تحقق من رابط التنزيل العام.");
        }
    }
}
