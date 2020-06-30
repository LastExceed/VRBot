package commands.handlers.manage

import commands.CommandProcessor
import commands.CommandResult
import commands.handlers.CommandHandler
import org.javacord.api.event.message.*

class SetChannel(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseParameterless(event: MessageCreateEvent): CommandResult {
		return CommandResult.ok { "WIP" }//TODO: allow configuring channels
	}
}