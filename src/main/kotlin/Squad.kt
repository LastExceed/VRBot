import enums.Refinement
import enums.Stagger
import org.javacord.api.entity.permission.Role
import org.javacord.api.entity.user.User

data class Squad(
	val host: User, //TODO: ensure user object comparability (switch to id if not)
	val role: Role,
	val refinement: Refinement?,
	val stagger: Stagger?,
	val anons: Int,
	val comment: String?
) {
	val guests = mutableListOf<User>()

	val isFull
		get() = members == maxMembers

	private val members
		get() = 1 + guests.size + anons

	fun createAdvertisement() = createDisplay(role.mentionTag)

	fun createFillNotification() = createDisplay("__**${role.name}**__")

	private fun createDisplay(header: String): String {
		var text = """
		**____**
		$header
		`${refinement ?: "any refinement"}` `${stagger ?: "?by?"}`
		> $comment
		${host.mentionTag} (host)
		""".trimIndent()
		repeat(anons) {
			text += "\n$anonName"
		}
		guests.forEach {
			text += "\n" + it.mentionTag
		}
		repeat(maxMembers - members) {
			text += "\n$openSlot"
		}

		return text
	}

	companion object {
		private const val maxMembers = 4
		private const val anonName = "@Anonymous"
		private const val openSlot = "*- open -*"
	}
}