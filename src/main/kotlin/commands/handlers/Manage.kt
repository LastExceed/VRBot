package commands.handlers

import commands.*
import commands.handlers.manage.*
import org.javacord.api.event.message.*
import java.util.*

class Manage(processor: CommandProcessor) : CommandHandler(
	processor,
	mapOf(
		"prefix" to Prefix(processor),
		"relics" to Relics(processor),
		"reset" to Reset(processor),
		"set-channel" to SetChannel(processor),
		"shutdown" to ShutDown(processor)
	)
) {
	//TODO: implement `requiresElevation` in base class instead
	override fun parse(parameters: List<String>, event: MessageCreateEvent): CommandResult {
		if (!event.messageAuthor.isBotOwner && !event.messageAuthor.isServerAdmin) return CommandResult.error("only the bot owner and server admins can use this")
		return super.parse(parameters, event)
	}
}

