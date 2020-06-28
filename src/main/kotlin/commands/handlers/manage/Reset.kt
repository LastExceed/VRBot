package commands.handlers.manage

import commands.CommandProcessor
import commands.CommandResult
import commands.handlers.CommandHandler
import org.javacord.api.event.message.*
import kotlin.streams.*

class Reset(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseParameterless(event: MessageCreateEvent): CommandResult {
		with(processor.bot) {
			val countSquads = squads.size
			squads.forEach { tryCloseSquad(it.key) }

			val countMessages = channels.overview.messagesAsStream.map {
				val isGettingDeleted = it.author.isYourself
				if (isGettingDeleted) it.delete()
				isGettingDeleted
			}.toList().count { it }

			val countRoles = server.roles.map {
				val isGettingDeleted = it.name.matches(Regex("""^(Lith|Meso|Neo|Axi) ([A-Z]\d+)(\+[A-Z]\d+)*$"""))
				if (isGettingDeleted) it.delete()
				isGettingDeleted
			}.count { it }

			return CommandResult.ok {
				"""
				closed $countSquads tracked squads
				deleted $countMessages orphan messages
				deleted $countRoles orphan roles
				""".trimIndent()
			}
		}
	}
}