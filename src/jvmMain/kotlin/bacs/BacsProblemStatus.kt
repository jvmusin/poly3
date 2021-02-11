package bacs

data class BacsProblemStatus(
    val name: String,
    val flags: List<String>,
    val revision: String
) {
    enum class State {
        PENDING_IMPORT,
        IMPORTED,
        NOT_FOUND,
        UNKNOWN
    }
    val state: State = when {
        name == "OK" && flags.any { it == "reserved: PENDING_IMPORT" } -> State.PENDING_IMPORT
        name == "OK" && flags.isEmpty() -> State.IMPORTED
        name == "code: NOT_FOUND" -> State.NOT_FOUND
        else -> State.UNKNOWN
    }
}