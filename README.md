# MetroViceRP

The custom roleplay framework for **Metro Vice RP** in **Monteri States**.

## Current milestone — Character Core 0.2.0

### Character system
- Multiple independent characters per Minecraft UUID (default maximum: 3).
- Arabic and international character names, including spaces.
- Per-character profile fields: age, job, rank, cash, and bank balance.
- Persistent active-character selection.
- Ownership validation on every character lookup and selection.
- In-game character selection inventory: `/character menu`.
- Character inventory, armor, ender chest, XP, health, food, saturation, and location are saved separately per character.
- Automatic load when the player joins and periodic/quit/shutdown saves.
- Rename and confirmed deletion of characters.
- SQLite schema upgrades for existing data folders.

### Commands
- `/mvrp` — plugin status.
- `/mvrp reload` — reload configuration (operator permission).
- `/character menu` — open the character selector.
- `/character create <name> <age>` — create a character (names may contain spaces).
- `/character list` — list owned characters.
- `/character select <id>` — switch active character.
- `/character info` — show active profile.
- `/character rename <id> <new name>` — rename an owned character.
- `/character delete <id> CONFIRM` — permanently delete an owned character and its saved state.

Example:
```
/character create Yousef Moammer 20
/character menu
/character info
```

## Build

Requires JDK 25 and Maven 3.9+:

```bash
mvn --batch-mode clean package
```

The output JAR is `target/MetroViceRP.jar`. GitHub Actions is configured to build on pushes to `main` and upload the JAR as a workflow artifact.

## Compatibility

The project targets the Paper 26.3 API line and Java 25, matching the server details supplied for development. The build must pass in CI before the JAR is considered ready for installation. Back up the server and its `plugins/MetroViceRP` data folder before testing updates.

## Planned modules

Custom weighted inventory, phone and apps, police MDT and duty roster, EMS, civilian/government jobs, economy and NPC shops, radio integration, vehicles, housing, and audit logs. Each module will be added in a separate milestone and tested against the target server.
