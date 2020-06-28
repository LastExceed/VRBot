package commands.handlers

import commands.*
import enums.*
import org.javacord.api.event.message.*
import toNullable
import java.util.*

class Kick(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseWithParameters(parameters: List<String>, event: MessageCreateEvent): CommandResult {
		val idInput = parameters.first()
		val id = idInput
			.removePrefix("<@!")
			.removeSuffix(">")
			.toLongOrNull() ?: return CommandResult.error("invalid user `$idInput`")
		val target = processor.bot.server.getMemberById(id).toNullable() ?: return CommandResult.error("user `$id` not found")
		val ownedSquads = processor.bot.squads.filter { it.value.host == event.messageAuthor }
		if (ownedSquads.isEmpty()) return CommandResult.error("you're not hosting anything")
		val sharedSquads = ownedSquads.filter { it.value.guests.contains(target) }
		if (sharedSquads.isEmpty()) return CommandResult.error("that user hasn't joined any of your squads")

		return CommandResult.ok {
			sharedSquads.forEach {
				if (it.value.guests.remove(target)) {
					it.key.edit(it.value.createAdvertisement())
					it.key.reactions.find {
						it.emoji.asUnicodeEmoji().toNullable() == Reaction.join.emoji
					}!!.removeUser(target)
				}
			}
			null
		}
	}
}