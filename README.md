# MetroViceRP

The custom roleplay framework for **Metro Vice RP** in **Monteri States**.

## Current milestone — Resource Pack Delivery Integration 0.7.0

### Resource pack delivery (integration wired; pack assets still being built)
- Configurable HTTPS direct ZIP URL and SHA-1 validation.
- Optional automatic delivery when a player joins.
- `/mvrp pack` sends the configured pack to yourself.
- `/mvrp pack send <player>` sends it to an online player (admin permission).
- Configure `resource-pack.enabled`, `url`, `sha1`, `required`, `auto-send-on-join`, and `prompt` in `config.yml`.
- Java and Bedrock pack assets are separate deliverables; this integration alone does not mean the final packs or their phone/radio/bank interfaces are finished.

## Existing core milestone — Character, Bank Employee NPC, Departments & Police MDT 0.6.0

### Character system
- Multiple independent characters per Minecraft UUID (default maximum: 3).
- Arabic and international character names, including spaces.
- Per-character profile fields: age, job, rank, cash, and bank balance.
- Configurable starting cash and bank balance.
- Character-linked bank with balance, deposit, withdrawal, and player-to-player bank transfer.
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
- `/mvrp setjob <player> <job>` — admin: change the online player's active character job.
- `/mvrp setrank <player> <rank>` — admin: change the online player's active character rank.
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

### Police MDT
- Admin gives the in-game laptop with `/mvrp givelaptop <player>`.
- The laptop opens `/mdt` when used; access is restricted to police characters while on duty.
- `/mdt roster` — view the current police duty roster.
- `/mdt search <name>` — search character profiles by name.

### Police and EMS commands
- `/duty [police|ems]` — start/end duty for a department assigned to your active character.
- `/mvrp hirepolice <player> <rank>` — admin: hire an online player's active character into police.
- `/mvrp firepolice <player>` — admin: remove the active character from police.
- `/mvrp hireems <player> <rank>` — admin: hire an online player's active character into EMS.
- `/mvrp fireems <player>` — admin: remove the active character from EMS.
- `/mvrp dutylist <police|ems>` — show current on-duty roster and session duration.

Police ranks are configured in the exact hierarchy supplied for Metro Vice RP. EMS ranks remain configurable.

### Bank employee NPC
- Admin creates the in-world bank employee at their current location with `/mvrp banknpc`.
- Right-click the employee to open the bank account menu and see the active character's cash and bank balance.
- Deposit and withdrawal are performed through the employee GUI using the available amount buttons.
- `/bank send <player> <amount>` remains available for player-to-player transfers.

### Character entry and switching
- On first join, the character menu opens automatically.
- Create the first character with `/character create <name> <age>`; it is activated automatically.
- `/character` or `/character menu` opens the character selector.
- `/character switch <id>` switches to another saved character and loads its isolated state.

### Bank commands
- `/bank balance`
- `/bank deposit <amount>`
- `/bank withdraw <amount>`
- `/bank send <player> <amount>` (both players must be online and have an active character)

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
