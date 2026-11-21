package com.metrovice.rp.resourcepack;

import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MetroViceRP-owned Bedrock pack registration.
 * Geyser remains responsible for the Bedrock protocol and delivering the pack.
 */
public final class BedrockResourcePackManager implements EventRegistrar {
    private final JavaPlugin plugin;
    private final Path packPath;
    private ResourcePack resourcePack;

    public BedrockResourcePackManager(JavaPlugin plugin, Path packPath) {
        this.plugin = plugin;
        this.packPath = packPath;
    }

    public void register() {
        if (packPath == null || !Files.isRegularFile(packPath)) {
            throw new IllegalStateException("Embedded Bedrock .mcpack is missing: " + packPath);
        }

        GeyserApi geyser = GeyserApi.api();
        if (geyser == null) {
            throw new IllegalStateException("Geyser API is not available. Check that Geyser-Spigot loads before MetroViceRP.");
        }

        resourcePack = ResourcePack.create(PackCodec.path(packPath));
        geyser.eventBus().register(this, this);
        geyser.eventBus().subscribe(this, SessionLoadResourcePacksEvent.class, this::onSessionLoadResourcePacks);
        plugin.getLogger().info("Registered MetroViceRP Bedrock pack with the Geyser API. Pack: " + packPath);
    }

    private void onSessionLoadResourcePacks(SessionLoadResourcePacksEvent event) {
        try {
            event.register(resourcePack);
            plugin.getLogger().info("Registered MetroViceRP Bedrock resource pack for a connecting Bedrock player.");
        } catch (Exception exception) {
            plugin.getLogger().severe("Geyser could not register the MetroViceRP Bedrock pack for a session: "
                    + exception.getMessage());
        }
    }
}
