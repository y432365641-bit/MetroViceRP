# Resource Pack Integration

## Java Edition
The Java pack is a resource pack foundation. Paper's ResourcePackService sends a hosted ZIP using its HTTPS URL and SHA-1. Configure `resource-pack.enabled`, `resource-pack.url`, and `resource-pack.sha1` in the plugin config, then use `/mvrp reload` and `/mvrp pack`.

The pack alone cannot create interactive phone/radio/bank screens. The plugin must implement the menus and draw matching glyphs/assets. Those menu handlers are still pending.

## Bedrock / mobile
Bedrock uses a separate `.mcpack` layout. A Java pack cannot be used as a Bedrock pack. Native Bedrock UI requires a Bedrock-side UI/add-on and a compatible Geyser/Floodgate integration; the current Bedrock files are only the resource foundation.

## Release criteria
Do not mark the pack final until both archives are built, their hashes are recorded, delivery is tested on Java and Bedrock clients, and each corresponding plugin interface is tested in-game.
