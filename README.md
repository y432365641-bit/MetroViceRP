# MetroViceRP

Minecraft roleplay plugin framework for **Metro Vice RP** and **Monteri States**.

## Current milestone: Core + Character Profiles

Implemented:
- Paper plugin bootstrap and configuration.
- SQLite database created automatically in the plugin data folder.
- Multiple character profiles per Minecraft account (configurable limit).
- Character creation, listing, selection, and active-character information.
- Per-player ownership checks for character IDs.
- GitHub Actions Maven build for Java 25.

Commands:
- `/mvrp` — show core status.
- `/mvrp reload` — reload configuration (operator permission).
- `/character create <name> <age>`
- `/character list`
- `/character select <id>`
- `/character info`

## Build

Requires JDK 25 and Maven 3.9+:

```bash
mvn clean package
```

Output: `target/MetroViceRP.jar`

## Compatibility

The Maven build targets the Paper API snapshot configured in `pom.xml`. The first CI run is the compatibility check. Do not install a JAR unless the build succeeds and the target server version is confirmed.

## Planned modules

Phone, custom inventory and weight, police MDT and duty, EMS, jobs, economy, NPC shops, radio integration, vehicles, housing, and audit logs will be added in separate tested milestones.
