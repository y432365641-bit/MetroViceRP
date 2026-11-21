# Metro Vice RP Resource Pack Integration

## Java Edition
The Java pack contains item models/textures for Metro Vice RP items. Paper's resource-pack service sends the Java ZIP using a direct HTTPS URL and its SHA-1. Configure `resource-pack.enabled`, `resource-pack.url`, and `resource-pack.sha1` in the plugin config.

## Bedrock / Geyser
MetroViceRP integrates directly with the Geyser API (2.12.1). The plugin stores its Bedrock `.mcpack` in its own data folder and registers it through Geyser's `SessionLoadResourcePacksEvent` for each connecting Bedrock client. Geyser then sends the pack through the Bedrock resource-pack handshake. No manual copy into `Geyser-Spigot/packs/` is required.

The Geyser custom item mappings are still installed in `plugins/Geyser-Spigot/custom_mappings/` during plugin loading so Geyser can load the custom item definitions on startup. Keep Geyser-Spigot installed; it is a required dependency.

## Testing
Join with a Bedrock client after a full server restart. The console should show that MetroViceRP registered its Geyser resource-pack listener, followed by a per-session registration message when a Bedrock player connects. Verify the Bedrock pack prompt and custom item textures.

## Plugin-connected systems
The phone, police MDT, rank-management laptop, and EMS medical bag use the shared Metro Vice RP item mappings and resource pack.
