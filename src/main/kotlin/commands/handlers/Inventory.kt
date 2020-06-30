package commands.handlers

import Database
import commands.*
import enums.Era
import org.javacord.api.event.message.*
import relicIDregex
import java.util.*

class Inventory(processor: CommandProcessor, private val isUserInventory: Boolean = true) : CommandHandler(processor) {
	override fun parseParameterless(event: MessageCreateEvent) = CommandResult.ok {
		Era.values().joinToString("\n") { era ->
			val ids = Database[era].getAllRelics()
				.run {
					if (isUserInventory) {
						filter { it.getAllUsers().contains(event.messageAuthor.id) }
					} else this
				}
				.joinToString { it.file.name }
			"__**$era:**__ $ids"
		}
	}

	override fun parseWithParameters(parameters: List<String>, event: MessageCreateEvent): CommandResult {
		val paramQueue = LinkedList(parameters)

		val actionInput = paramQueue.remove()
		val add = when (actionInput) {
			"add" -> true
			"remove" -> false
			else -> return CommandResult.error("unknown inventory action `$actionInput`")
		}

		val eraInput = paramQueue.poll() ?: return CommandResult.error("Era not specified")
		val era = Era.find(eraInput) ?: return CommandResult.error("unknown era `$eraInput`")

		val relicIDs = paramQueue.map { it.toUpperCase() }
		paramQueue.clear()

		relicIDs.forEach {
			if (!it.matches(relicIDregex)) return CommandResult.error("the relic ID `$it` doesn't match regex `${relicIDregex.pattern}` (aka you probably have a typo")
			if (isUserInventory && Database[era][it] == null) return CommandResult.error("relic `$eraInput $it` not found")
		}

		return CommandResult.ok {
			val databaseEra = Database[era]
			val skips = relicIDs.map {
				if (isUserInventory) {
					val databaseRelic = databaseEra[it]!!
					if (add) databaseRelic.addUser(event.messageAuthor.id)
					else databaseRelic.removeUser(event.messageAuthor.id)
				} else {
					if (add) databaseEra.addRelic(it)
					else databaseEra[it]?.delete() ?: false
				}
			}.count { !it }
			if (skips > 0) "skipped $skips relics because they were already present/absent" else null
		}
	}
}