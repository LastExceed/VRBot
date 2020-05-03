import enums.Reaction
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.DiscordClient
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.user.User
import org.javacord.api.entity.user.UserStatus
import java.io.File
import java.util.*

val token = File("bot_secret").readText()
val api = DiscordApiBuilder().setToken(token).login().join()

val server = api.servers.find { it.id == 697398509682425897 }!!

val squads = mutableMapOf<Message, Squad>()

fun main() {

	//println(api.createBotInvite())
	server.addReactionAddListener(::onReactionAdd)
	server.addReactionRemoveListener(::onReactionRemove)
	server.addMessageCreateListener(::onMessageCreate)
	server.addMessageDeleteListener(::onMessageDelete)
	println("ready")

	loop@ while (true) {
		when (readLine()) {
			"exit" -> {
				api.disconnect()
				return
			}
		}
	}
}

//TODO: move these somewhere else
fun <T> Optional<T>.toNullable() = orElse(null)

fun openSquad(author: User, newSquad: SquadSoonToBe) {
	server.createRoleBuilder()
		.setName(newSquad.era.name + " " + newSquad.relicIDs.joinToString("+"))
		.setMentionable(true)
		.create()
		.whenComplete { role, ex ->
			newSquad.relicIDs.forEach { relicID ->
				val relicDb = Database[newSquad.era][relicID]!!
				for (userID in relicDb.getAllUsers()) {

					val user = server.getMemberById(userID).toNullable() ?: continue//when someone leaves the server
					if (user.status != UserStatus.ONLINE) continue
					if (user.getStatusOnClient(DiscordClient.DESKTOP) != UserStatus.ONLINE &&
						user.getStatusOnClient(DiscordClient.WEB) != UserStatus.ONLINE
					) continue
					if (user.getRoles(server).contains(role)) continue //when they match multiple relics in the query

					user.addRole(role)
				}
			}

			Thread.sleep(500) //@mentions don't work if done too quickly after role creation/assignment. GG discord

			val squad = Squad(author, role, newSquad.refinement, newSquad.stagger, newSquad.anons, newSquad.comment)
			Channels.overview.sendMessage(squad.createAdvertisement()).whenCompleteAsync { message, ex ->
				squads[message] = squad
				message.addReaction(Reaction.join.emoji)
				message.addReaction(Reaction.delete.emoji)

				Thread.sleep(15 * 60 * 1000)

				if (squads.containsKey(message)) {
					closeSquad(message)
				}
			}
		}
}

fun closeSquad(message: Message): Boolean {
	message.delete()
	squads.remove(message)?.role?.delete() ?: return false
	return true
}