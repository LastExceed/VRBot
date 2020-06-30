package commands.handlers

import commands.*
import org.javacord.api.event.message.*
import java.util.*

abstract class CommandHandler(
	internal val processor: CommandProcessor,
	private val subCommands: Map<String, CommandHandler> = mapOf(),
	private val requiresElevation: Boolean = false
) {
	open fun parse(parameters: List<String>, event: MessageCreateEvent): CommandResult =
		when {
			requiresElevation && !event.messageAuthor.isBotOwner && !event.messageAuthor.isServerAdmin -> {
				CommandResult.error("only the bot owner and server admins can use this")
			}
			parameters.isEmpty() -> {
				parseParameterless(event)
			}
			else -> {
				subCommands[parameters.first()]?.parse(parameters.drop(1), event)
					?: parseWithParameters(parameters, event)
			}
		}

	internal open fun parseParameterless(event: MessageCreateEvent) = CommandResult.error("too few arguments")
	internal open fun parseWithParameters(parameters: List<String>, event: MessageCreateEvent) =
		CommandResult.error("superflous argument `${parameters.first()}`")
}