package bacs

data class BacsProblemStatus(
    val name: String,
    val flags: List<String>,
    val revision: String
) {
    val state: BacsProblemState = when {
        name == "OK" && flags.any { it == "reserved: PENDING_IMPORT" } -> BacsProblemState.PENDING_IMPORT
        name == "OK" && flags.isEmpty() -> BacsProblemState.IMPORTED
        name == "code: NOT_FOUND" -> BacsProblemState.NOT_FOUND
        else -> BacsProblemState.UNKNOWN
    }
}

enum class BacsProblemState {
    PENDING_IMPORT,
    IMPORTED,
    NOT_FOUND,
    UNKNOWN
}