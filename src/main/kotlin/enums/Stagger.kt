package enums

enum class Stagger(private val shortName: String) {
	`1by1`("1b1"),
	`2by2`("2b2"),
	`3by3`("3b3"),
	`4by4`("4b4");

	companion object {
		fun find(input: String) = values().find {
			it.name.startsWith(input) || it.shortName.startsWith(input)
		}
	}
}