package commands.handlers

import commands.CommandProcessor
import commands.CommandResult
import commands.handlers.help.Inventory
import enums.Reaction
import org.javacord.api.event.message.*

class Help(processor: CommandProcessor) : CommandHandler(
	processor,
	mapOf(
		"inventory" to Inventory(processor)
	)
) {
	override fun parseParameterless(event: MessageCreateEvent) = CommandResult.ok {
		with(processor) {
			"""
			- join/leave squads in <#${bot.channels.overview.id}> by clicking the ${Reaction.join.emoji} reaction 
			- host squads like this: `${commandPrefix}host meso f2 radiant 2by2 1/4` (refinement, stagger and member count are optional)
				- for mixed runs, add multiple relic IDs together: `${commandPrefix}host meso f2+f3 flawless 2by2 1/4`
				- you can use multiple lines and abbreviations:
			```
			${commandPrefix}host meso f2 rad 2b2 1/4
			${commandPrefix}host a h3+h4 r 4 3
			${commandPrefix}host neo v1
			```- close your squads by clicking the ${Reaction.delete.emoji} reaction or using `${commandPrefix}close-all`
			- hosts can kick guests using `${commandPrefix}kick @user`
			- full squads will be closed automatically and pinged in ${bot.channels.formed.mentionTag}
			- to get pinged for specific relics, set up your inventory -> `${commandPrefix}help inventory`
			""".trimIndent()
		}
	}
}