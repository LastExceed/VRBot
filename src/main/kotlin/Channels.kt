import org.javacord.api.entity.channel.ServerTextChannel

data class Channels(
	var commands: ServerTextChannel,
	var overview: ServerTextChannel,
	var formed: ServerTextChannel
)