package dev.dnbdj.prime.server;

import dev.dnbdj.prime.PrimeMod;
import dev.dnbdj.prime.discord.DiscordRoles;
import dev.dnbdj.prime.members.PrimeMembers;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Server-side wiring: the verified tick, the Discord role and {@code /prime}.
 *
 * <p>The tick is a vanilla scoreboard team prefix, so chat, the tab list and the
 * nametag above the head show it to unmodded clients - no client mod, no custom
 * packets. Membership comes only from the server's own file, so a client cannot
 * claim the tick.
 */
public class PrimeServer implements DedicatedServerModInitializer {
	/** Team that carries the tick; created on demand and rebuilt by {@code /prime reload}. */
	private static final String TEAM_NAME = "prime_verified";
	private static final Text TICK = Text.literal("\u2714 ").formatted(Formatting.GREEN);

	private static long startedAt;

	@Override
	public void onInitializeServer() {
		// Members must be known before the first player joins.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> PrimeMembers.load());

		ServerLifecycleEvents.SERVER_STARTED.register(started -> {
			startedAt = System.currentTimeMillis();
			PrimeMod.LOGGER.info("Prime server side ready on Minecraft {}.", started.getVersion());
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> startedAt = 0L);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			PrimeMod.LOGGER.info("{} joined ({} online).",
					player.getName().getString(), server.getCurrentPlayerCount());

			if (!PrimeMembers.isVerified(player.getUuid())) {
				return;
			}

			mark(player, server);
			DiscordRoles.grant(player.getUuid());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PrimeMod.LOGGER.info(
				"{} left.",
				handler.getPlayer().getName().getString()));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				CommandManager.literal("prime")
						.executes(PrimeServer::status)
						.then(CommandManager.literal("reload")
								.requires(source -> source.hasPermissionLevel(2))
								.executes(PrimeServer::reload))));
	}

	/** Puts the player on the tick team, creating the team the first time it is needed. */
	private static void mark(ServerPlayerEntity player, MinecraftServer server) {
		ServerScoreboard scoreboard = server.getScoreboard();
		Team team = scoreboard.getTeam(TEAM_NAME);

		if (team == null) {
			team = scoreboard.addTeam(TEAM_NAME);

			if (team == null) {
				return;
			}

			team.setPrefix(TICK);
		}

		scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), team);
	}

	/** Re-reads the member file and re-applies the tick, so removals take effect at once. */
	private static int reload(ServerCommandSource source) {
		PrimeMembers.load();

		MinecraftServer server = source.getServer();
		// Drop and rebuild: a member removed from the file loses the tick immediately.
		server.getScoreboard().removeTeam(TEAM_NAME);

		for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
			if (PrimeMembers.isVerified(online.getUuid())) {
				mark(online, server);
			}
		}

		int count = PrimeMembers.size();
		source.sendFeedback(() -> Text.literal("Prime: reloaded " + count + " verified member(s)."), true);
		return count;
	}

	/** Everything {@code /prime} reports, read from the live server, never cached to disk. */
	private static int status(ServerCommandSource source) {
		String version = FabricLoader.getInstance()
				.getModContainer(PrimeMod.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
		source.sendFeedback(() -> Text.literal("Prime (server side) " + version), false);

		MinecraftServer server = source.getServer();
		source.sendFeedback(() -> Text.literal("Minecraft " + server.getVersion() + " - "
				+ server.getCurrentPlayerCount() + "/" + server.getMaxPlayers() + " players"), false);
		source.sendFeedback(() -> Text.literal("Verified members: " + PrimeMembers.size()
				+ (PrimeMembers.discord().configured() ? ", Discord role on" : ", Discord role off")), false);
		source.sendFeedback(() -> Text.literal("Uptime " + uptime()), false);
		return 1;
	}

	private static String uptime() {
		if (startedAt == 0L) {
			return "just started";
		}

		long seconds = (System.currentTimeMillis() - startedAt) / 1000L;
		long days = seconds / 86400L;
		long hours = (seconds % 86400L) / 3600L;
		long minutes = (seconds % 3600L) / 60L;

		if (days > 0) {
			return days + "d " + hours + "h";
		}

		if (hours > 0) {
			return hours + "h " + minutes + "m";
		}

		return minutes + "m " + (seconds % 60L) + "s";
	}
}
