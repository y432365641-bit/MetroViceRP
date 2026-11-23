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
import java.util.UUID;

/**
 * MetroViceRP-owned Bedrock resource-pack bridge.
 *
 * Geyser remains responsible for the Bedrock protocol. This class registers
 * the pack for each new Bedrock session while avoiding duplicate UUIDs that
 * may already have been added by Geyser or another compatible plugin.
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
     * Wait for Geyser's API to become available. On some hosts Geyser finishes
     * enabling after this plugin's onEnable callback.
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
                    plugin.getLogger().info("Pack UUID: " + resourcePack.uuid());
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

        UUID targetUuid = resourcePack.uuid();
        boolean alreadyQueued = event.resourcePacks().stream()
                .anyMatch(existing -> targetUuid.equals(existing.uuid()));

        if (alreadyQueued) {
            plugin.getLogger().fine("MetroViceRP Bedrock pack is already queued for this session (UUID "
                    + targetUuid + "); skipping duplicate registration.");
            return;
        }

        try {
            event.register(resourcePack, new ResourcePackOption<?>[0]);
            plugin.getLogger().info("Added MetroViceRP Bedrock pack to a connecting Bedrock session.");
        } catch (Exception exception) {
            boolean registeredByAnotherListener = event.resourcePacks().stream()
                    .anyMatch(existing -> targetUuid.equals(existing.uuid()));

            if (registeredByAnotherListener) {
                plugin.getLogger().fine("MetroViceRP Bedrock pack was already registered by another listener "
                        + "for this session; continuing without a duplicate.");
                return;
            }

            plugin.getLogger().severe("Could not add MetroViceRP pack to a Bedrock session: "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }
}
