package commands

class CommandResult private constructor(
	val parsingSucceeded: Boolean,
	val execute: () -> String?
) {
	companion object {
		fun error(message: String) = CommandResult(false) { message }
		fun ok(function: () -> String?) = CommandResult(true, function)
	}
}