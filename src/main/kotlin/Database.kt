import enums.Era
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

object Database {
	private const val basePath = "database/"

	operator fun get(era: Era) = DatabaseEra(era)

	class DatabaseEra internal constructor(val era: Era) {
		operator fun get(id: String): DatabaseRelic? {
			val file = File("$basePath$era/$id")
			return if (file.exists()) DatabaseRelic(file) else null
		}

		fun addRelic(id: String) = File("$basePath$era/$id").createNewFile()

		class DatabaseRelic internal constructor(val file: File) {
			fun getAllUsers(): LongArray {
				val fileSize = file.length().toInt()
				val reader = DataInputStream(file.inputStream())
				if (fileSize % 8 != 0) {
					//TODO: database corruption
				}
				return LongArray(fileSize / 8) { reader.readLong() }
			}

			fun addUser(toAdd: Long): Boolean {
				val allUsers = getAllUsers()
				if (allUsers.contains(toAdd)) return false
				val stream = DataOutputStream(file.outputStream())
				allUsers.forEach { stream.writeLong(it) }
				stream.writeLong(toAdd)
				stream.close()
				return true
			}

			fun removeUser(toRemove: Long): Boolean {
				val allUsers = getAllUsers()
				if (!allUsers.contains(toRemove)) return false
				val stream = DataOutputStream(file.outputStream())
				for (id in allUsers) {
					if (id == toRemove) continue
					stream.writeLong(id)
				}
				stream.close()
				return true
			}

			fun delete() = file.delete()
		}
	}
}