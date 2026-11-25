# Prime (server side)

The server half of [Prime](https://github.com/dnbdj/Prime-Client-Side). A Fabric
server mod for **Minecraft Java 1.21.11** that runs on a dedicated server; it
never loads on a client (`"environment": "server"`).

## What it does

- **`/prime`** - reports the mod version, the server's Minecraft version, the
  online player count and how long the server has been up. No permission
  needed; nothing is persisted.
- **Session logging** - one log line on join (with the online count) and one on
  leave, so a server operator can see who came and went in `logs/latest.log`.

Status is read from the live server every time, never cached, so it cannot
report a stale player count.

## Build

Same toolchain as [gem-mod](https://github.com/dnbdj/gem-mod): Loom 1.18.4,
loader 0.19.5, Yarn 1.21.11+build.6, Fabric API 0.141.6+1.21.11, Java 21.

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.1-hotspot"
.\gradlew.bat build
.\gradlew.bat runServer   # needs a eula.txt with eula=true in run/
```

Output: `build/libs/prime-1.0.0.jar`.

This repo ships no Gradle wrapper binary - copy `gradlew`, `gradlew.bat` and
`gradle/wrapper/` from gem-mod (or run `gradle wrapper`) once.

### If the network blocks the Fabric maven

`maven.fabricmc.net` and Mojang's metadata hosts are TLS-blocked on some
machines. Copy gem-mod's `local-mirror/` directory here; `build.gradle` picks it
up automatically and resolves the intermediary mappings from disk.

## Deploy

Drop the jar into the server's `mods/` folder. Fabric API must be present too:

```
mods/prime-1.0.0.jar
mods/fabric-api-0.141.6+1.21.11.jar
```

The launcher's instance setup offers a **Prime mod** option; the jar it installs
is built from this repository.

## Notes

- `getCurrentPlayerCount()`, `getMaxPlayers()` and `getVersion()` are read from
  the `MinecraftServer` the command source belongs to, so `/prime` is correct on
  any running server instance.
- `ServerPlayConnectionEvents` lives in Fabric API's **networking** module
  (`api.networking.v1`), not the lifecycle module; its `JOIN` callback hands you
  a `ServerPlayNetworkHandler`, so call `getPlayer()` on it.
- `CommandRegistrationCallback` is the `v2` package, matching the command
  registry API of 1.21.11.
