import org.javacord.api.DiscordApiBuilder
import java.io.File
import java.util.*

fun main() {
	val tokenFile = File("bot_secret")
	if (!tokenFile.exists()) error("bot_secret not found")
	val token = tokenFile.readText()
	val api = DiscordApiBuilder().setToken(token).login().join()

	val server = api.servers.find { it.id == 697398509682425897 }!!
	val bot = VRBot(server)

//	val bots = mutableListOf<VRBot>()
//	api.servers.forEach {
//		bots.add(VRBot(it))
//	}

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

val relicIDregex = Regex("[A-Z][0-9]+")