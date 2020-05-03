import org.javacord.api.event.message.MessageDeleteEvent

fun onMessageDelete(event: MessageDeleteEvent) {
	squads.remove(event.message.toNullable() ?: return)?.role?.delete()
}