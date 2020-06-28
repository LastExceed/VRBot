package commands.handlers

import commands.CommandProcessor
import commands.CommandResult
import org.javacord.api.event.message.*

class CloseAll(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseParameterless(event: MessageCreateEvent): CommandResult {
		val ownedSquads = processor.bot.squads.filter { it.value.host == event.messageAuthor.asUser().get() }

		return CommandResult.ok {
			ownedSquads.forEach { processor.bot.tryCloseSquad(it.key) }
			if (ownedSquads.isEmpty()) "you're not hosting anything" else null
		}
	}


}