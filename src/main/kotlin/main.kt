import org.javacord.api.DiscordApiBuilder
import java.io.File
import java.util.*

fun main() {
	val tokenFile = File("bot_secret")
	if (!tokenFile.exists()) {
		println("bot_secret not found")
		return
	}
	val token = tokenFile.readText()
	val api = DiscordApiBuilder().setToken(token).login().join()

	val server = api.servers.find { it.id == 697398509682425897 }!!
	val bot = VRBot(api, server)

	println("ready")

	while (true) {
		when (readLine()) {
			"exit" -> {
				api.disconnect()
				return
			}
		}
	}
}