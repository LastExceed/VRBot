package commands.handlers

import commands.*
import commands.handlers.manage.*
import org.javacord.api.event.message.*
import java.util.*

class Manage(processor: CommandProcessor) : CommandHandler(
	processor,
	mapOf(
		"prefix" to Prefix(processor),
		"relics" to Inventory(processor, false),
		"reset" to Reset(processor),
		"set-channel" to SetChannel(processor),
		"shutdown" to ShutDown(processor)
	),
	requiresElevation = true
)

