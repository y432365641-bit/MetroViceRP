# Metro Vice RP Voice — Plugin Integration Contract (Draft v1)

This document defines the proposed contract between the Paper plugin and the Metro Vice RP Voice backend. The plugin remains the source of truth for all game state. The website must not create or edit Minecraft characters, jobs, ranks, positions, radio permissions, or phone numbers.

## Responsibilities

- **Paper plugin:** player identity, active character, world and coordinates, job/rank/duty, held radio item, radio channel authorization, phone number ownership, online state, and in-game call/radio actions.
- **Voice website/backend:** browser UI, microphone permission and capture, WebRTC/audio transport, call/radio room orchestration, and user-facing connection state.
- **Audio is not carried by this HTTP API.** A separate WebRTC/SFU or other supported voice transport must be configured and tested.

## Security requirements

- Backend-to-plugin requests use HTTPS and a dedicated server secret configured only on the server. Never expose that secret in browser JavaScript.
- Bind the plugin HTTP listener to localhost by default. If the backend is remote, expose it only through a TLS reverse proxy/firewall allowlist.
- Every mutating request must be authenticated, timestamped, and replay-protected; use a request signature/HMAC in the production implementation.
- Never accept a character name, job, rank, phone number, or radio permission supplied by the browser as authoritative. Resolve them from the online Bukkit player and active character.
- Do not add a website-side character registration flow.

## Proposed endpoints

All paths are under `/api/v1`. These are proposed names; finalize them against the generated Lovable backend before implementation.

### Plugin → backend events

The plugin should send signed events when:
- a player joins/leaves or changes active character;
- a player moves between worlds or changes position (throttled);
- a player changes job/rank/duty;
- the radio item enters/leaves the off-hand;
- a phone number is assigned or a call is started/accepted/rejected/ended.

### Backend → plugin requests

- `GET /players/{uuid}/voice-state` — current online state, active character, world, position, job/rank/duty, radio availability, and phone number.
- `POST /radio/transmit` — request PTT start/stop. Plugin must re-check that the player is online, has an active character, is holding the configured radio item in the off-hand, and is authorized for the requested channel. The website cannot grant access.
- `POST /phone/call` — request a call by target phone number. Plugin resolves the target number and both active characters.
- `POST /phone/call/{id}/answer`, `/reject`, and `/end` — call lifecycle requests. Both participants must be online and the call ID must belong to the caller/recipient.
- `GET /health` — integration health only; no private player data.

## Radio rules

- Radio is an actual Minecraft item and is used from the off-hand.
- No `/radio` command is required to open or use it.
- Police and EMS channels are permission-gated from plugin department membership, rank, and duty state.
- The website's push-to-talk control requests transmit start while pressed and transmit stop on release. The plugin re-validates the player and item for both transitions.
- Removing the radio, going offline, leaving duty, or losing authorization must stop transmission.

## Phone rules

- Each in-game character owns its phone number; the plugin/database is authoritative.
- Calls are private, independent of Minecraft distance, and require both participants to be online.
- The website may display call state but cannot invent phone numbers or mark a call accepted without plugin/backend confirmation.

## Proximity rules

- Proximity eligibility is calculated from live Minecraft positions and configured distance rules.
- Players in different worlds must not hear each other unless an explicit server rule says otherwise.
- The voice transport must enforce room membership server-side; client-side distance filtering alone is not sufficient.

## Compatibility and acceptance checklist

1. No duplicate website character/account database is used for game identity.
2. A player without an active Minecraft character cannot join an in-game voice session.
3. Radio transmission is rejected if the radio is not in the off-hand.
4. Police/EMS channel access is re-checked by the plugin, not trusted from the browser.
5. Phone calls are resolved from plugin-owned character phone numbers.
6. Proximity membership updates when players move, change world, join, or leave.
7. Audio transport is separately configured and tested on Java and Bedrock/mobile clients.
8. Microphone access requires the user's browser/device permission; the plugin cannot silently enable it.
