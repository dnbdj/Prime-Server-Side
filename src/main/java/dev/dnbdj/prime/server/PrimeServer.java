package dev.dnbdj.prime.server;

import dev.dnbdj.prime.PrimeMod;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Server-side wiring: session logging and the {@code /prime} status command.
 *
 * <p>Uses the dedicated-server entrypoint, so this only runs on a real server and
 * never on a client. Everything here is server-authoritative; nothing is trusted
 * from the connecting client and no custom packets are needed.
 */
public class PrimeServer implements DedicatedServerModInitializer {
	private static long startedAt;

	@Override
	public void onInitializeServer() {
		ServerLifecycleEvents.SERVER_STARTED.register(started -> {
			startedAt = System.currentTimeMillis();
			PrimeMod.LOGGER.info("Prime server side ready on Minecraft {}.", started.getVersion());
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> startedAt = 0L);

		// The callback hands over a ServerPlayNetworkHandler, not the player directly.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PrimeMod.LOGGER.info(
				"{} joined ({} online).",
				handler.getPlayer().getName().getString(),
				server.getCurrentPlayerCount()));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PrimeMod.LOGGER.info(
				"{} left.",
				handler.getPlayer().getName().getString()));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("prime").executes(PrimeServer::status)));
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
