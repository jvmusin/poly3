package api

import kotlinx.serialization.Serializable

@Serializable
enum class Verdict(val description: String, val tag: String) {
    OK("Правильное решение", "OK"),
    WRONG_ANSWER("Неправильный ответ", "WA"),
    TIME_LIMIT_EXCEEDED("Превышено время исполнения", "TL"),
    MEMORY_LIMIT_EXCEEDED("Превышена используемая память", "ML"),
    PRESENTATION_ERROR("Ошибка представления", "PE"),
    INCORRECT("Решение неверно", "FAIL"),
    ABNORMAL_EXIT("Abnormal exit", "AE"),
    SERVER_ERROR("Ошибка сервера", "SE"),
    OTHER("Другое", "OTHER"),
    NOT_TESTED("Решение пропущено", "SKIP"),
    COMPILATION_ERROR("Ошибка компиляции", "CE");

    fun isFail() = this != OK && this != NOT_TESTED
}