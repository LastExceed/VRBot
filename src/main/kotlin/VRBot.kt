import commands.CommandProcessor
import enums.Reaction
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.server.Server
import org.javacord.api.event.message.*
import org.javacord.api.event.message.reaction.*

class VRBot(val server: Server) {
	init {
		server.addMessageCreateListener(::onMessageCreate)
		server.addMessageDeleteListener(::onMessageDelete)
		server.addReactionAddListener(::onReactionAdd)
		server.addReactionRemoveListener(::onReactionRemove)
	}

	private val commandProcessor = CommandProcessor(this)
	val squads = mutableMapOf<Message, Squad>()

	val channels = Channels(
		commands = server.textChannels.find { it.name == "bot-spam" }!!,
		overview = server.textChannels.find { it.name == "squads-overview" }!!,
		formed = server.textChannels.find { it.name == "squads-formed" }!!
	)

	private fun onMessageCreate(event: MessageCreateEvent) {
		val author = event.messageAuthor.asUser().toNullable() ?: return
		if (author.isYourself) return //prevent recursions

		commandProcessor.handle(event)
	}

	private fun onMessageDelete(event: MessageDeleteEvent) {
		tryCloseSquad(event.message.toNullable() ?: return)
	}

	private fun onReactionAdd(event: ReactionAddEvent) {
		if (event.user.isYourself) return //prevents any potential recursion
		val message = event.message.toNullable() ?: return
		val squad = squads[message] ?: return

		when (event.emoji.asUnicodeEmoji().toNullable()) {
			Reaction.join.emoji -> {
				if (event.user == squad.host) {
					event.removeReaction()
					return
				}

				squad.guests.add(event.user)

				if (squad.isFull) {
					tryCloseSquad(message)
					channels.formed.sendMessage(squad.createFillNotification())
				} else message.edit(squad.createAdvertisement())
			}
			Reaction.delete.emoji -> {
				if (event.user == squad.host || event.user.isBotOwner || server.isAdmin(event.user)) {
					tryCloseSquad(message)
				} else {
					event.removeReaction()
				}
			}
			else -> event.removeReaction()
		}
	}

	private fun onReactionRemove(event: ReactionRemoveEvent) {
		val message = event.message.toNullable()
		if (event.emoji.asUnicodeEmoji().toNullable() != Reaction.join.emoji ||
			event.user.isYourself ||
			message == null
		) return
		val squad = squads[message] ?: return

		val wasMember = squad.guests.remove(event.user)
		if (wasMember) message.edit(squad.createAdvertisement())
	}

	fun openSquad(squad: Squad) {
		channels.overview.sendMessage(squad.createAdvertisement())
			.whenCompleteAsync { message, ex ->
				squads[message] = squad
				message.addReaction(Reaction.join.emoji)
				message.addReaction(Reaction.delete.emoji)

				Thread.sleep(15 * 60 * 1000)

				if (squads.containsKey(message)) tryCloseSquad(message)
			}
	}

	fun tryCloseSquad(message: Message): Boolean { //TODO: using the message to identify the squad feels wrong
		message.delete()
		squads.remove(message)?.role?.delete() ?: return false
		return true
	}
}