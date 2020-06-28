package commands.handlers.manage

import commands.CommandProcessor
import commands.CommandResult
import commands.handlers.CommandHandler
import org.javacord.api.event.message.*
import java.util.*

class Prefix(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseWithParameters(parameters: List<String>, event: MessageCreateEvent): CommandResult {
		processor.commandPrefix = parameters.first()
		return CommandResult.ok { null }
	}
}