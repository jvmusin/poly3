package ir

data class IRProblem(
    val name: String,
    val owner: String,
    val statement: IRStatement,
    val limits: IRLimits,
    val tests: List<IRTest>?,
    val groups: List<IRTestGroup>?,
    val checker: IRChecker,
    val solutions: List<IRSolution>
) {
    val mainSolution get() = solutions.single { it.isMain }
}

data class IRStatement(val name: String, val content: List<Byte>)
data class IRTest(
    val index: Int,
    val isSample: Boolean,
    val input: String,
    val output: String,
    val points: Int?,
    val groupName: String?
)

data class IRChecker(val name: String, val content: String)
data class IRLimits(val timeLimitMillis: Int, val memoryLimitMegabytes: Int)
data class IRSolution(
    val name: String,
    val verdict: IRVerdict,
    val isMain: Boolean,
    val language: IRLanguage,
    val content: String
)

enum class IRVerdict {
    OK,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    PRESENTATION_ERROR,
    INCORRECT,
    OTHER
}

enum class IRLanguage(val fullName: String) {
    CPP("C++"),
    JAVA("Java"),
    KOTLIN("Kotlin"),
    PYTHON2("Python 2"),
    PYTHON3("Python 3"),
    OTHER("Other")
}

enum class IRTestGroupPointsPolicy {
    COMPLETE_GROUP,
    EACH_TEST
}

data class IRTestGroup(
    val name: String,
    val pointsPolicy: IRTestGroupPointsPolicy,
    val testIndices: List<Int>,
    val points: Int?
)
