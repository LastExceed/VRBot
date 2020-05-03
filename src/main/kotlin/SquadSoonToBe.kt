import enums.Era
import enums.Refinement
import enums.Stagger

class SquadSoonToBe(
	val era: Era,
	val relicIDs: Set<String>,
	val refinement: Refinement?,
	val stagger: Stagger?,
	val anons: Int,
	val comment: String? = null
)