# Metro Vice RP Resource Pack Integration

## Java Edition
The Java pack contains item models/textures for the phone, radio, EMS medical bag, police MDT laptop, and rank-management laptop. Paper's resource-pack service sends the Java ZIP using a direct HTTPS URL and its SHA-1. Configure `resource-pack.enabled`, `resource-pack.url`, and `resource-pack.sha1` in the plugin config.

## Bedrock / Geyser
Install `MetroViceRP-Bedrock.mcpack` in `plugins/Geyser-Spigot/packs/` and `MetroViceRP.json` in `plugins/Geyser-Spigot/custom_mappings/`. Enable Geyser custom content in its configuration and restart the server. Geyser does not automatically convert Java packs or generate item mappings, so both the Bedrock pack and mappings are required.

## Plugin-connected systems
The phone opens character identity, bank balance, messaging/social screens, gallery/location snapshot, settings, custom inventory, police MDT shortcut, radio, and EMS emergency screens. The police rank manager is a separate laptop, restricted to on-duty officers at rank Lieutenant (ملازم) or above. The EMS medical bag can revive a nearby downed player only when its holder is an on-duty EMS member.

## Release criteria
A successful build confirms compilation and packaging only. Mark the pack final after testing the item appearances and plugin interactions on both Java and Bedrock clients. Preserve `plugins/MetroViceRP/metrovice.db` when updating.
