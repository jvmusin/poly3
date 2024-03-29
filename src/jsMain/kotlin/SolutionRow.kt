import api.Problem
import api.Solution
import api.SubmissionResult
import api.Verdict
import kotlinx.coroutines.launch
import kotlinx.html.ThScope
import kotlinx.html.role
import react.PropsWithChildren
import react.dom.span
import react.dom.td
import react.dom.th
import react.dom.tr
import react.fc
import react.useEffect
import react.useState

external interface SolutionRowProps : PropsWithChildren {
    var problem: Problem
    var solution: Solution
    var runTriggered: Boolean
    var sybonProblemId: Int?
}

val SolutionRow = fc<SolutionRowProps> { props ->
    val (isRunning, setRunning) = useState(false)
    val (result, setResult) = useState<SubmissionResult?>(null)

    useEffect(props.problem, props.runTriggered) {
        setRunning(props.runTriggered)
        var cancelled = false
        if (props.runTriggered) {
            setResult(null)
            mainScope.launch {
                try {
                    val res = Api.testSolution(props.problem, props.sybonProblemId!!, props.solution.name)
                    if (!cancelled) setResult(res)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (!cancelled) setResult(null)
                } finally {
                    if (!cancelled) setRunning(false)
                }
            }
        }
        cleanup {
            cancelled = true
            setRunning(false)
            setResult(null)
        }
    }

    val rowStyle = "problem-solution " + when {
        result == null -> ""
        result.verdict == Verdict.COMPILATION_ERROR -> "bg-warning"
        result.verdict == Verdict.NOT_TESTED -> "bg-secondary text-white-50"
        result.verdict.isFail() != props.solution.expectedVerdict.isFail() -> "bg-danger text-white"
        else -> "bg-success text-white"
    }

    tr(rowStyle) {
        val solution = props.solution
        th {
            +solution.name
            attrs.scope = ThScope.row
        }
        td { +solution.language.description }
        td { verdict("${solution.name}-expect", solution.expectedVerdict) }
        td {
            when {
                isRunning -> span("spinner-border text-secondary") { attrs.role = "status" }
                result != null -> {
                    if (result.message != null) verdict(
                        id = "${solution.name}-result",
                        verdict = result.verdict,
                        description = result.message
                    )
                    else verdict("${solution.name}-result", result.verdict)
                }
                else -> +"-"
            }
        }
        val test = result?.failedTestNumber
        val time = result?.maxUsedTimeMillis
        val mem = result?.maxUsedMemoryBytes
        val rt = result?.executionTimeSeconds
        td { if (test != null) +"$test" else +"-" }
        td { if (time != null) +"${time}ms" else +"-" }
        td { if (mem != null) +"${(mem / 1024.0 / 1024.0).toInt()}MB" else +"-" }
        td { if (rt != null) +"${rt}s" else +"-" }
    }
}
