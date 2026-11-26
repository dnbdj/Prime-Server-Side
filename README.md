# Prime (server side)

The server half of [Prime](https://github.com/dnbdj/Prime-Client-Side). A Fabric
server mod for **Minecraft Java 1.21.11** that runs on a dedicated server; it
never loads on a client (`"environment": "server"`).

## What it does

**The verified tick.** Every player listed in the server's member file gets a
green `✔` in front of their name. It is a vanilla scoreboard team prefix, so
chat, the tab list and the nametag above the head show it to unmodded clients -
no client mod, no custom packets, no resource pack.

**The Discord role.** The same players get the configured role in your Discord
server, granted when they join. One REST call, idempotent (Discord answers 204
when they already have it), fired without blocking the server thread.

There is nothing else: no gameplay effects, no cosmetics, no perks.

## Verification

Membership lives in `config/prime-members.json` on the server. Nothing a client
sends is trusted, so a vanilla client cannot claim the tick - you add people
by UUID, then `/prime reload`.

```json
{
  "discord": {
    "token": "your bot token",
    "guildId": "123456789012345678",
    "roleId": "234567890123456789"
  },
  "members": {
    "069a79f4-44e9-4726-a5be-fca90e38aaf5": {
      "name": "Notch",
      "discordId": "345678901234567890"
    }
  }
}
```

`discordId` is optional per member; without it they still get the tick and only
the role is skipped. Leaving `discord` blank disables the bot entirely.

## Discord bot setup

1. Create an application at <https://discord.com/developers/applications>, add a
   bot, and copy its token into `discord.token`.
2. Invite it with the **Manage Roles** permission.
3. In the server's role list, drag the bot's own role **above** the role you
   want to grant - Discord refuses to grant roles above the bot's top role.
4. Put the guild id and the role id in the file, then `/prime reload`.

Keep `config/prime-members.json` readable only by the server account: it holds
the bot token.

## Commands

| Command | Who | Effect |
| --- | --- | --- |
| `/prime` | everyone | Mod version, Minecraft version, player count, member count, uptime |
| `/prime reload` | permission level 2 | Re-reads the member file and re-applies the tick to online players |

## Build

Same toolchain as [gem-mod](https://github.com/dnbdj/gem-mod): Loom 1.18.4,
loader 0.19.5, Yarn 1.21.11+build.6, Fabric API 0.141.6+1.21.11, Java 21.

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
.\gradlew.bat build
.\gradlew.bat runServer   # needs a eula.txt with eula=true in run/
```

Output: `build/libs/prime-1.0.0.jar`. The wrapper is committed (Gradle 9.8.2),
so no local Gradle install is needed; the first run downloads the distribution.

Last verified build: `BUILD SUCCESSFUL` on Gradle 9.8.2 and JDK 25 (targeting Java
21), producing `prime-1.0.0.jar`.

### If the network blocks the Fabric maven

`maven.fabricmc.net` and Mojang's metadata hosts are TLS-blocked on some
machines. Copy gem-mod's `local-mirror/` directory here; `build.gradle` picks it
up automatically and resolves the intermediary mappings from disk.

## Deploy

Drop the jar into the server's `mods/` folder alongside Fabric API:

```
mods/prime-1.0.0.jar
mods/fabric-api-0.141.6+1.21.11.jar
```

## Notes

- The tick uses `Scoreboard.addScoreHolderToTeam` with `getNameForScoreboard()`,
  so it follows name changes and stays unique per player.
- `/prime reload` removes and recreates the team rather than editing it, which
  is what makes a removal from the file take effect on players already online.
- `ServerPlayConnectionEvents` lives in Fabric API's **networking** module
  (`api.networking.v1`); its `JOIN` callback hands you a
  `ServerPlayNetworkHandler`, so call `getPlayer()` on it.
- `HttpClient.sendAsync` keeps the Discord call off the server thread; only the
  outcome is logged, never the token.
- 1.21.9+ replaced numeric permission levels: `/prime reload` gates on
  `CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)`
  (the old level 2) instead of `hasPermissionLevel(2)`.
