package commands

import VRBot
import commands.handlers.*
import org.javacord.api.event.message.MessageCreateEvent
import java.util.*

class CommandProcessor(val bot: VRBot) {
	var commandPrefix = "::"
	val commandHandlers: Map<String, CommandHandler>

	init {
		val host = Host(this)
		val kick = Kick(this)
		val closeAll = CloseAll(this)

		commandHandlers = mapOf(
			"closeall" to closeAll,
			"close-all" to closeAll,
			"close_all" to closeAll,
			"help" to Help(this),
			"h" to host,
			"host" to host,
			"inventory" to Inventory(this),
			"kick" to kick,
			"manage" to Manage(this)
		)
	}

	fun handle(event: MessageCreateEvent) {
		val commands = event.message.content
			.toLowerCase()
			.lines()
			.map { it.trim() }
			.filter { it.startsWith(commandPrefix) }
			.map { line ->
				line
					.removePrefix(commandPrefix)
					.trim() //trim a second time in case of ' :: host'
					.replace(Regex("""\s+"""), " ")
					.split(' ')
			}.filter { it.isNotEmpty() }
		if (commands.isEmpty()) return

		val results = commands.map { command ->
			commandHandlers[command.first()]?.parse(command.drop(1), event)
				?: CommandResult.error("unknown command `${command.first()}`")
		}
		val resultsFailed = results.filter { !it.parsingSucceeded }
		val allCommandsSucceeded = resultsFailed.isEmpty()
		val toExecute = if (allCommandsSucceeded) results else resultsFailed

		val response = toExecute
			.mapNotNull { it.execute() }
			.joinToString("\n")
		val reaction = if (allCommandsSucceeded) "✅" else "❌" //"⚠️"

		if (event.channel == bot.channels.commands) {
			event.message.addReaction(reaction)
			if (response.isNotBlank()) bot.channels.commands.sendMessage(response)
		} else {
			event.message.delete()
			if (allCommandsSucceeded && response.isBlank()) return
			val ping = event.messageAuthor.asUser().get().mentionTag
			val quote = event.message.content
			bot.channels.commands.sendMessage("$ping```$quote```$reaction\n$response")
		}
	}
}