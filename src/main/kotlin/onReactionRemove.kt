import enums.Reaction
import org.javacord.api.event.message.reaction.ReactionRemoveEvent

fun onReactionRemove(event: ReactionRemoveEvent) {
	val message = event.message.toNullable()
	if (event.emoji.asUnicodeEmoji().toNullable() != Reaction.join.emoji ||
		event.user.isYourself ||
		message == null
	) return
	val squad = squads[message] ?: return

	val wasMember = squad.guests.remove(event.user)
	if (wasMember) message.edit(squad.createAdvertisement())
}