package com.metrovice.rp.resourcepack;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MetroViceRP-owned Bedrock resource-pack bridge.
 *
 * Geyser remains the Bedrock protocol implementation. This class owns the
 * MetroViceRP pack file and registers it with every new Bedrock session.
 */
public final class BedrockResourcePackManager implements EventRegistrar {
    private static final int MAX_ATTEMPTS = 60;

    private final JavaPlugin plugin;
    private final Path packPath;
    private ResourcePack resourcePack;
    private boolean registered;

    public BedrockResourcePackManager(JavaPlugin plugin, Path packPath) {
        this.plugin = plugin;
        this.packPath = packPath;
    }

    /**
     * Waits for Geyser's API to become available. Some server hosts enable
     * plugins in an order where Geyser finishes its bootstrap after this
     * plugin's onEnable callback.
     */
    public void register() {
        if (packPath == null || !Files.isRegularFile(packPath)) {
            plugin.getLogger().severe("Bedrock pack file is missing: " + packPath);
            return;
        }

        new BukkitRunnable() {
            private int attempts;

            @Override
            public void run() {
                if (!plugin.isEnabled()) {
                    cancel();
                    return;
                }

                attempts++;
                try {
                    GeyserApi geyser = GeyserApi.api();
                    if (geyser == null) {
                        retryOrStop();
                        return;
                    }

                    resourcePack = ResourcePack.create(PackCodec.path(packPath));
                    geyser.eventBus().register(BedrockResourcePackManager.this,
                            BedrockResourcePackManager.this);
                    geyser.eventBus().subscribe(BedrockResourcePackManager.this,
                            SessionLoadResourcePacksEvent.class,
                            BedrockResourcePackManager.this::onSessionLoadResourcePacks);

                    registered = true;
                    plugin.getLogger().info("Bedrock pack bridge is ready. Waiting for Bedrock sessions.");
                    plugin.getLogger().info("Pack file: " + packPath.toAbsolutePath());
                    cancel();
                } catch (Throwable exception) {
                    if (attempts >= MAX_ATTEMPTS) {
                        plugin.getLogger().severe("Could not connect the Bedrock pack bridge to Geyser after "
                                + attempts + " attempts: " + exception.getClass().getSimpleName()
                                + ": " + exception.getMessage());
                        cancel();
                    } else {
                        plugin.getLogger().warning("Waiting for Geyser API (" + attempts + "/"
                                + MAX_ATTEMPTS + "): " + exception.getMessage());
                    }
                }
            }

            private void retryOrStop() {
                if (attempts >= MAX_ATTEMPTS) {
                    plugin.getLogger().severe("Geyser API did not become available. "
                            + "Check that Geyser-Spigot is installed and enabled.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 20L);
    }

    private void onSessionLoadResourcePacks(SessionLoadResourcePacksEvent event) {
        if (!registered || resourcePack == null) {
            return;
        }

        try {
            // Pass an empty options array to select the non-deprecated API overload.
            event.register(resourcePack, new ResourcePackOption<?>[0]);
            plugin.getLogger().info("Added MetroViceRP Bedrock pack to a connecting Bedrock session.");
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not add MetroViceRP pack to a Bedrock session: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }
}
