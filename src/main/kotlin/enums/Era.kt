package enums

enum class Era {
	Lith,
	Meso,
	Neo,
	Axi;

	companion object {
		fun find(input: String) = values().find { it.name.toLowerCase().startsWith(input.toLowerCase()) }
	}
}