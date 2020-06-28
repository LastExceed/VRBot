import java.util.*

fun <T> Optional<T>.toNullable() = orElse(null)

val relicIDregex = Regex("[A-Z][0-9]+")