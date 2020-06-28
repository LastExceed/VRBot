package commands.handlers

import commands.*
import org.javacord.api.event.message.*
import java.util.*

abstract class CommandHandler(
	internal val processor: CommandProcessor,
	private val subCommands: Map<String, CommandHandler> = mapOf()
) {
	open fun parse(parameters: List<String>, event: MessageCreateEvent): CommandResult =
		if (parameters.isEmpty())
			parseParameterless(event)
		else
			subCommands[parameters.first()]?.parse(parameters.drop(1), event)
				?: parseWithParameters(parameters, event)

	internal open fun parseParameterless(event: MessageCreateEvent) = CommandResult.error("too few arguments")
	internal open fun parseWithParameters(parameters: List<String>, event: MessageCreateEvent) =
		CommandResult.error("superflous argument `${parameters.first()}`")
	//TODO: ensure all parameters were parsed
}