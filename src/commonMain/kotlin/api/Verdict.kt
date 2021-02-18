package api

import kotlinx.serialization.Serializable

@Serializable
enum class Verdict(val description: String, val tag: String) {
    OK("OK", "OK"),
    WRONG_ANSWER("Неправильный ответ", "WA"),
    TIME_LIMIT_EXCEEDED("Превышено время исполнения", "TL"),
    MEMORY_LIMIT_EXCEEDED("Превышена используемая память", "ML"),
    PRESENTATION_ERROR("Ошибка представления", "PE"),
    INCORRECT("Решение неверно", "INCORRECT"),
    ABNORMAL_EXIT("Abnormal exit", "ABNORMAL EXIT"),
    SERVER_ERROR("Ошибка сервера", "SERVER ERROR"),
    OTHER("Другое", "OTHER"),
    COMPILATION_ERROR("Ошибка компиляции", "CE")
}