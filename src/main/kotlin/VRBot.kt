import enums.Era
import enums.Reaction
import enums.Refinement
import enums.Stagger
import org.javacord.api.entity.DiscordClient
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.server.Server
import org.javacord.api.entity.user.User
import org.javacord.api.entity.user.UserStatus
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.event.message.MessageDeleteEvent
import org.javacord.api.event.message.reaction.ReactionAddEvent
import org.javacord.api.event.message.reaction.ReactionRemoveEvent

class VRBot(private val server: Server) {
	init {
		server.addMessageCreateListener(::onMessageCreate)
		server.addMessageDeleteListener(::onMessageDelete)
		server.addReactionAddListener(::onReactionAdd)
		server.addReactionRemoveListener(::onReactionRemove)
	}

	private val squads = mutableMapOf<Message, Squad>()

	private val channels = Channels(
		commands = server.textChannels.find { it.name == "bot-spam" }!!,
		overview = server.textChannels.find { it.name == "squads-overview" }!!,
		formed = server.textChannels.find { it.name == "squads-formed" }!!
	)

	private fun onMessageCreate(event: MessageCreateEvent) {
		val author = event.messageAuthor.asUser().toNullable() ?: return
		if (author.isYourself) return //prevent recursions

		val text = event.message.content

		if (!text.startsWith(commandPrefix)) {
			if (event.channel == channels.overview) event.message.delete()
			return
		}

		val result = parseCommand(text.removePrefix(commandPrefix).trim().replace(Regex("""\s+"""), " "), author)

		if (event.channel == channels.commands) {
			event.message.addReaction(result.reaction.emoji)
			result.response?.let { channels.commands.sendMessage(it) }
		} else {
			event.message.delete()
			if (result.reaction == Reaction.ok && result.response == null) return
			channels.commands.sendMessage(
				"${event.messageAuthor.asUser()
					.get().mentionTag}```${event.message.content}```${result.reaction.emoji}${result.response}"
			)
		}
	}

	private fun onMessageDelete(event: MessageDeleteEvent) {
		squads.remove(event.message.toNullable() ?: return)?.role?.delete()
	}

	private fun onReactionAdd(event: ReactionAddEvent) {
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
					channels.formed.sendMessage(squad.createFillNotification())
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


	private fun openSquad(author: User, newSquad: SquadSoonToBe) {
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
				channels.overview.sendMessage(squad.createAdvertisement()).whenCompleteAsync { message, ex ->
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

	private fun parseCommand(text: String, author: User): Result {
		val lines = text.lines().toMutableList()
		val command = lines.first().split(' ')
		if (lines.first().startsWith("${command.first()} ")) { //note the whitespace
			lines[0] = lines.first().removePrefix("${command.first()} ") //here too
		} else {
			lines.removeAt(0)
		}

		when (command[0].toLowerCase()) {
			"h", "host" -> {
				val newSquads = mutableListOf<SquadSoonToBe>()

				lines.forEach {
					val arguments = it.split(' ')
					if (arguments.size < 2) {
						return Result(Reaction.error, "too few arguments")
					}

					val era = Era.find(arguments[0]) ?: run {
						return Result(Reaction.error, "unknown era `${arguments[0]}`")
					}
					val relicIDs = arguments[1].toUpperCase().split('+').toSet() //set removes duplicates

					var refinement: Refinement? = null
					var stagger: Stagger? = null
					var anons: Int? = null
					val comment = mutableListOf<String>()

					for (argument in arguments.drop(2)) {
						if (refinement == null) {
							refinement = Refinement.get(argument.toLowerCase())
							if (refinement != null) continue
						}
						if (stagger == null) {
							stagger = Stagger.get(argument.toLowerCase())
							if (stagger != null) continue
						}
						if (anons == null) {
							val memberCount = argument.toIntOrNull() ?: if (argument.matches(Regex("""[1-3]/4"""))) {
								argument[0].toString().toInt()
							} else null
							if (memberCount in 1..3) {
								anons = memberCount!! - 1
								continue
							}
						}
						comment.add(argument)
					}

					newSquads.add(SquadSoonToBe(era, relicIDs, refinement, stagger, anons ?: 0, comment.joinToString(" ")))
				}
				newSquads.forEach { newSquad ->
					newSquad.relicIDs.forEach { relicID ->
						if (!relicID.matches(relicIDregex)) {
							return Result(
								Reaction.error,
								"relic ID `$relicID` doesn't match regex `${relicIDregex.pattern}`"
							)
						}
						if (Database[newSquad.era][relicID] == null) {
							return Result(
								Reaction.error,
								"The relic `${newSquad.era} $relicID` doesn't exist or isn't vaulted"
							)
						}
					}
				}
				newSquads.forEach { newSquad ->
					openSquad(author, newSquad)
				}
				return Result(Reaction.ok)
			}

			"k", "kick" -> {
				if (command.size < 2) return Result(Reaction.error, "too few arguments")
				val id = command[1].removePrefix("<@!").removeSuffix(">").toLongOrNull() ?: return Result(Reaction.error, "invalid user `${command[1]}`")
				val target = server.getMemberById(id).toNullable() ?: return Result(Reaction.warning, "user not found")
				var kicked = false
				for (ad in squads) {
					if (ad.value.host != author) continue
					if (ad.value.guests.remove(target)) {
						ad.key.edit(ad.value.createAdvertisement())
						ad.key.reactions.find {
							it.emoji.asUnicodeEmoji().toNullable() == Reaction.join.emoji
						}!!.removeUser(target)
						kicked = true
					}
				}
				return if (!kicked) Result(Reaction.warning, "this user hasn't joined any of your squads")
				else Result(Reaction.ok)
			}

			"c", "close-all", "closeall", "close_all" -> {
				var count = 0
				squads.keys.toList().forEach {
					if (squads[it]!!.host == author) {
						closeSquad(it)
						count++
					}
				}
				if (count == 0) {
					return Result(Reaction.warning, "you're not hosting anything")
				}
				return Result(Reaction.ok)
			}

			"help" -> {
				if (command.size < 2) {
					return Result(
						Reaction.ok,
						"""
					- join/leave squads in <#${channels.overview.id}> by clicking the ${Reaction.join.emoji} reaction 
					- host squads like this: `${commandPrefix}host meso f2 radiant 2by2 1/4` (refinement, stagger and member count are optional)
						- for mixed runs, add multiple relic IDs together: `${commandPrefix}host meso f2+f3 flawless 2by2 1/4`
						- you can use multiple lines and abbreviations:
					```
					${commandPrefix}host
					meso f2 rad 2b2 1/4
					a h3+h4 r 4 3
					neo v1
					```- close your squads by clicking the ${Reaction.delete.emoji} reaction or using `${commandPrefix}close-all`
					- hosts can kick guests using `${commandPrefix}kick @user`
					- full squads will be closed automatically and pinged in ${channels.formed.mentionTag}
					- to get pinged for specific relics, set up your inventory -> `${commandPrefix}help inventory`
					""".trimIndent()
					)
				}
				return when (command[1].toLowerCase()) {
					"inventory" -> Result(
						Reaction.ok,
						"""
					- **you will only get pinged if your discord status is set to online** (mobile doesn't count)
					- `${commandPrefix}inventory add meso a1 a2 b1 b2 b3`
					- `${commandPrefix}inventory remove lith f1 f2 g1 k3`
					""".trimIndent()
					)
					else -> Result(Reaction.error, "unknown parameter `${command[1]}`")
				}
			}

			"i", "inventory" -> {
				if (command.size < 2) {
					return Result(Reaction.error, "too few arguments")
				}

				when (command[1].toLowerCase()) {
					"add", "remove" -> {
						if (command.size < 4) {
							return Result(Reaction.error, "too few arguments")
						}
						val era = Era.find(command[2]) ?: run {
							return Result(Reaction.error, "unknown era `${command[2]}`")
						}
						val relicIDs = command.drop(3).map { it.toUpperCase() }
						relicIDs.forEach {
							if (!it.matches(relicIDregex) || Database[era][it] == null) {
								return Result(Reaction.error, "the relic `${era} $it` doesn't exist or isn't vaulted")
							}
						}
						var skips = 0
						relicIDs.forEach {
							val databaseRelic = Database[era][it]!!
							val success = when (command[1].toLowerCase()) {
								"add" -> databaseRelic.addUser(author.id)
								else -> databaseRelic.removeUser(author.id)
							}
							if (!success) skips++
						}
						if (skips > 0) {
							return Result(Reaction.warning, "skipped $skips relics")
						}
						return Result(Reaction.ok)
					}
					else -> {
						return Result(Reaction.error, "unknown parameter `${command[1]}`")
					}
				}
			}

			"m", "manage" -> {
				if (!author.isBotOwner && !server.isAdmin(author)) {
					return Result(Reaction.error, "only the bot owner and server admins can use this")
				}

				if (command.size < 2) {
					return Result(Reaction.error, "too few arguments")
				}

				when (command[1].toLowerCase()) {
					"prefix" -> {
						if (command.size < 3) {
							return Result(Reaction.error, "too few arguments")
						}
						commandPrefix = command[2]
						return Result(Reaction.ok)
					}

					"relics" -> {
						if (command.size < 3) {
							return Result(Reaction.error, "too few arguments")
						}
						when (command[2].toLowerCase()) {
							"add", "remove" -> {
								if (command.size < 5) {
									return Result(Reaction.error, "too few arguments")
								}

								val era = Era.find(command[3]) ?: run {
									return Result(Reaction.error, "unknown era `${command[3]}`")
								}

								val relicIDs = command.drop(4).map { it.toUpperCase() }
								relicIDs.forEach {
									if (!it.matches(relicIDregex)) {
										return Result(
											Reaction.error,
											"`$it` does not match regex `${relicIDregex.pattern}`"
										)
									}
								}
								var skips = 0
								relicIDs.forEach {
									val success = when (command[2].toLowerCase()) {
										"add" -> Database[era].addRelic(it)
										else -> Database[era][it]?.delete() ?: false
									}
									if (!success) skips++
								}
								if (skips > 0) {
									return Result(Reaction.warning, "skipped $skips relic(s)")
								}
								return Result(Reaction.ok)
							}
							else -> {
								return Result(Reaction.error, "unknown parameter `${command[2]}`")
							}
						}
					}

					"reset" -> {
						var countSquads = 0
						squads.keys.toList().forEach {
							closeSquad(it)
							countSquads++
						}

						var countMessages = 0
						channels.overview.messagesAsStream.forEach {
							if (it.author.isYourself) {
								it.delete()
								countMessages++
							}
						}
						var countRoles = 0
						server.roles.forEach {
							if (it.name.matches(Regex("""^(Lith|Meso|Neo|Axi) ([A-Z]\d+)(\+[A-Z]\d+)*$"""))) {
								it.delete()
								countRoles++
							}
						}
						return Result(
							Reaction.ok,
							"""
						closed $countSquads tracked squads
						deleted $countMessages orphan messages
						deleted $countRoles orphan roles
						""".trimIndent()
						)
					}

					"set-channel", "setchannel", "set_channel" -> {
						if (command.size < 3) {
							return Result(Reaction.error, "too few arguments")
						}

						return Result(Reaction.warning, "this command is WIP")

//					when (command[2]) {
//						"overview" -> Channels.overview = ;
//						"formed" -> Channels.formed = ;
//						"commands" -> Channels.commands = ;
//						else -> return Result(Reaction.error, "unknown channel `${command[2]}`")
//					}
					}

//					"shutdown" -> {
//						api.disconnect()
//						error("emergency shutdown")
//					}

					else -> return Result(Reaction.error, "unknown parameter `${command[1]}`")
				}
			}

			else -> return Result(Reaction.error, "unknown command `${command[0]}`")
		}
	}

	private fun closeSquad(message: Message): Boolean {
		message.delete()
		squads.remove(message)?.role?.delete() ?: return false
		return true
	}
}