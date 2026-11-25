package dev.dnbdj.prime.discord;

import dev.dnbdj.prime.PrimeMod;
import dev.dnbdj.prime.members.PrimeMembers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Grants the configured Discord role to the account linked to a verified player.
 *
 * <p>One plain REST call over {@link HttpClient} - no bot library, and the server
 * thread never waits on Discord. Discord answers 204 when the member already has
 * the role, so calling this on every join is safe.
 */
public final class DiscordRoles {
	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private DiscordRoles() {
	}

	/** Fire-and-forget; a failure is logged and the join carries on. */
	public static void grant(UUID uuid) {
		PrimeMembers.Discord discord = PrimeMembers.discord();
		PrimeMembers.Entry entry = PrimeMembers.entry(uuid);

		if (discord == null || !discord.configured() || entry == null) {
			return;
		}

		String discordId = entry.discordId();

		if (discordId == null || discordId.isBlank()) {
			PrimeMod.LOGGER.info("-- {} is verified but has no Discord id on file.", label(entry, uuid));
			return;
		}

		URI uri = URI.create("https://discord.com/api/v10/guilds/%s/members/%s/roles/%s"
				.formatted(discord.guildId().trim(), discordId.trim(), discord.roleId().trim()));

		HttpRequest request = HttpRequest.newBuilder(uri)
				.header("Authorization", "Bot " + discord.token().trim())
				.header("Content-Type", "application/json")
				.header("User-Agent", "Prime-Server-Side (https://github.com/dnbdj/Prime-Server-Side)")
				.timeout(Duration.ofSeconds(15))
				.PUT(HttpRequest.BodyPublishers.noBody())
				.build();

		CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenAccept(response -> {
					if (response.statusCode() / 100 == 2) {
						PrimeMod.LOGGER.info("Discord role granted to {}.", label(entry, uuid));
					} else {
						// The body never contains the token; status plus message is enough to fix the setup.
						PrimeMod.LOGGER.warn("Discord role grant failed ({}): {}",
								response.statusCode(), shorten(response.body()));
					}
				})
				.exceptionally(error -> {
					PrimeMod.LOGGER.warn("Could not reach Discord: {}", error.getMessage());
					return null;
				});
	}

	private static String label(PrimeMembers.Entry entry, UUID uuid) {
		return entry.name() == null || entry.name().isBlank() ? uuid.toString() : entry.name();
	}

	private static String shorten(String body) {
		if (body == null) {
			return "";
		}

		String single = body.replace('\n', ' ').trim();
		return single.length() <= 200 ? single : single.substring(0, 200) + "...";
	}
}
