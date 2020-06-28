package commands.handlers.help

import commands.CommandProcessor
import commands.CommandResult
import commands.handlers.CommandHandler
import org.javacord.api.event.message.*

class Inventory(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseParameterless(event: MessageCreateEvent) = CommandResult.ok {
		"""
		- **you will only get pinged if your discord status is set to online** (mobile doesn't count)
		- `${processor.commandPrefix}inventory add meso a1 a2 b1 b2 b3`
		- `${processor.commandPrefix}inventory remove lith f1 f2 g1 k3`
		""".trimIndent()
	}
}