package commands.handlers

import Database
import Squad
import commands.*
import enums.*
import org.javacord.api.entity.*
import org.javacord.api.entity.user.*
import org.javacord.api.event.message.*
import relicIDregex
import toNullable
import java.util.*

class Host(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseWithParameters(parameters: List<String>, event: MessageCreateEvent): CommandResult {
		val paramQueue = LinkedList(parameters)

		val eraInput = paramQueue.poll() ?: return CommandResult.error("Era not specified")
		val era = Era.find(eraInput) ?: return CommandResult.error("unknown era `$eraInput`")

		val relicIDsInput = paramQueue.poll() ?: return CommandResult.error("Relic(s) not specified")
		val relicIDs = relicIDsInput.toUpperCase().split('+').toSet() //set removes duplicates
		val relics = relicIDs.map {
			if (!it.matches(relicIDregex)) return CommandResult.error("relic ID `$it` doesn't match regex `${relicIDregex.pattern}`")
			Database[era][it] ?: return CommandResult.error("The relic `$era $it` doesn't exist or isn't vaulted")
		}.toSet()

		var refinement: Refinement? = null
		var stagger: Stagger? = null
		var anons: Int? = null
		val comment = mutableListOf<String>()

		while (!paramQueue.isEmpty()) { //TODO: seems hacky
			val parameter = paramQueue.remove()
			if (refinement == null) {
				refinement = Refinement.find(parameter.toLowerCase())
				if (refinement != null) continue
			}
			if (stagger == null) {
				stagger = Stagger.find(parameter.toLowerCase())
				if (stagger != null) continue
			}
			if (anons == null) {
				val memberCount = parameter.toIntOrNull()
					?: if (parameter.matches(Regex("[1-3]/4"))) {
						parameter[0].toString().toInt()
					} else null
				if (memberCount in 1..3) {
					anons = memberCount!! - 1
					continue
				}
			}
			comment.add(parameter)
		}

		return CommandResult.ok {
			processor.bot.server.createRoleBuilder()
				.setName(era.name + " " + relicIDs.joinToString("+"))
				.setMentionable(true)
				.create()
				.whenComplete { role, ex ->
					val toPing = relics.flatMap { it.getAllUsers().toList() }
						.toSet() //removes duplicates
						.map { id -> processor.bot.server.getMemberById(id).toNullable() }
						.filterNotNull()
						.filter { user ->
							setOf(DiscordClient.DESKTOP, DiscordClient.WEB)
								.map { client -> user.getStatusOnClient(client) }
								.contains(UserStatus.ONLINE)
						}
						.forEach { it.addRole(role) }

					Thread.sleep(500) //@mentions don't work if done too quickly after role creation/assignment. GG discord

					processor.bot.openSquad(
						Squad(
							event.messageAuthor.asUser().get(),
							role,
							refinement,
							stagger,
							anons ?: 0,
							comment.joinToString(" ")
						)
					)
				}

			null
		}
	}
}