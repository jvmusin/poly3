@file:Suppress("ArrayInDataClass")

package ir

data class IRProblem(
    val name: String,
    val owner: String,
    val statement: IRStatement,
    val limits: IRLimits,
    val tests: List<IRTest>,
    val checker: IRChecker,
    val solutions: List<IRSolution>
) {
    val mainSolution get() = solutions.singleOrNull { it.verdict == "MA" } //todo maybe make some normal verdicts
}

data class IRStatement(val name: String, val content: ByteArray)
data class IRTest(val index: Int, val isSample: Boolean, val input: String, val output: String)
data class IRChecker(val name: String, val content: String)
data class IRLimits(val timeLimitMillis: Int, val memoryLimitMegabytes: Int)
data class IRSolution(val name: String, val verdict: String, val language: String, val content: String)