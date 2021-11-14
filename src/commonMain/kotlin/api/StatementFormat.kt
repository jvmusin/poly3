package api

enum class StatementFormat {
    PDF,
    HTML;

    val lowercase = name.lowercase()
}