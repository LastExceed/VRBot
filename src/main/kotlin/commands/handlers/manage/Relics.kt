package commands.handlers.manage

import Database
import commands.CommandProcessor
import commands.CommandResult
import commands.handlers.CommandHandler
import enums.Era
import org.javacord.api.event.message.*
import relicIDregex
import java.util.*

//TODO: prevent duplication
class Relics(processor: CommandProcessor) : CommandHandler(processor) {
	override fun parseParameterless(event: MessageCreateEvent) = CommandResult.ok {
		Era.values().joinToString("\n") { era ->
			val ids = Database[era].getAllRelics()
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
			if (!add && Database[era][it] == null) return CommandResult.error("relic `$eraInput $it` not found")
		}

		return CommandResult.ok {
			val skips = relicIDs.map {
				val databaseEra = Database[era]
				if (add) databaseEra.addRelic(it)
				else databaseEra[it]?.delete() ?: false
			}.count { !it }
			if (skips > 0) "skipped $skips relics because they were already present/absent" else null
		}
	}
}