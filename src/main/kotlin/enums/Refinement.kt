package enums

enum class Refinement {
	Intact,
	Exceptional,
	Flawless,
	Radiant;

	companion object {
		fun find(input: String) = values().find { it.name.toLowerCase().startsWith(input) }
	}
}