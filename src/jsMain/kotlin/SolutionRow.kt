import api.Problem
import api.Solution
import api.SubmissionResult
import kotlinx.coroutines.launch
import kotlinx.html.ThScope
import kotlinx.html.role
import react.RProps
import react.dom.div
import react.dom.td
import react.dom.th
import react.dom.tr
import react.functionalComponent
import react.useEffectWithCleanup
import react.useState

external interface SolutionRowProps : RProps {
    var problem: Problem
    var solution: Solution
    var runTriggered: Boolean
    var sybonProblemId: Int?
}

val SolutionRow = functionalComponent<SolutionRowProps> { props ->
    val (isRunning, setRunning) = useState(false)
    val (result, setResult) = useState<SubmissionResult?>(null)

    console.log("Run triggered = ${props.runTriggered}")
    useEffectWithCleanup(listOf(props.problem, props.runTriggered)) {
        console.log("SETTING RUN TRIGGERED FOR ${props.solution.name} TO ${props.runTriggered}")
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
        td { +solution.expectedVerdict.tag }
        td {
            console.log("Solution ${solution.name} is ${if (isRunning) "RUNNING" else "NOT RUNNING"}")
            if (isRunning) {
                div("spinner-border text-secondary") {
                    attrs { role = "status" }
                }
            } else {
                +(result?.overallVerdict?.tag ?: "-")
            }
        }
    }
}