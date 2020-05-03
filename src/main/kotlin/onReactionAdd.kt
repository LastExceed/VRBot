import enums.Reaction
import org.javacord.api.event.message.reaction.ReactionAddEvent

fun onReactionAdd(event: ReactionAddEvent) {
	if (event.user.isYourself) return

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
				closeSquad(message)
				Channels.formed.sendMessage(squad.createFillNotification())
			} else message.edit(squad.createAdvertisement())
		}
		Reaction.delete.emoji -> {
			if (event.user != squad.host && !event.user.isBotOwner && !server.isAdmin(event.user)) {
				event.removeReaction()
				return
			}

			closeSquad(message)
		}
		else -> return
	}
}