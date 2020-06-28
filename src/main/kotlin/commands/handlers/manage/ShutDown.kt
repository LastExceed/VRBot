package commands.handlers.manage

import commands.CommandProcessor
import commands.CommandResult
import commands.handlers.CommandHandler
import org.javacord.api.event.message.*

class ShutDown(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseParameterless(event: MessageCreateEvent): CommandResult {
		return CommandResult.ok {
			//api.disconnect()
			//error("emergency shutdown")
			"WIP"
		}
	}
}