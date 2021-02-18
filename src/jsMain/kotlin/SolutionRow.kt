import api.Problem
import api.Solution
import api.SubmissionResult
import kotlinext.js.jsObject
import kotlinx.coroutines.launch
import kotlinx.html.ThScope
import kotlinx.html.role
import react.*
import react.dom.div
import react.dom.td
import react.dom.th
import react.dom.tr
import kotlin.math.roundToInt

external interface SolutionRowProps : RProps {
    var problem: Problem
    var solution: Solution
    var runTriggered: Boolean
    var sybonProblemId: Int?
}

val SolutionRow = functionalComponent<SolutionRowProps> { props ->
    val (isRunning, setRunning) = useState(false)
    val (result, setResult) = useState<SubmissionResult?>(null)

    useEffectWithCleanup(listOf(props.problem, props.runTriggered)) {
        setRunning(props.runTriggered)
        var cancelled = false
        if (props.runTriggered) {
            setResult(null)
            scope.launch {
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
        {
            cancelled = true
            setRunning(false)
            setResult(null)
        }
    }

    tr {
        val solution = props.solution
        th {
            +solution.name
            attrs { scope = ThScope.row }
        }
        td { +solution.language.description }
        td { child(VerdictView, jsObject { verdict = solution.expectedVerdict; id = "${solution.name}-0" }) }
        td {
            when {
                isRunning -> {
                    div("spinner-border text-secondary") { attrs { role = "status" } }
                }
                result != null -> {
                    child(VerdictView, jsObject { verdict = result.verdict; id = "${solution.name}-1" })
                }
                else -> {
                    +(result?.verdict?.tag ?: "-")
                }
            }
        }
        val test = result?.failedTestNumber
        val time = result?.maxUsedTimeMillis
        val mem = result?.maxUsedMemoryBytes
        td { if (test != null) +"$test" }
        td { if (time != null) +"${time / 1000.0}s" }
        td { if (mem != null) +"${(mem / 1024.0 / 1024.0).roundToInt()}MB" }
    }
}