import api.Problem
import api.Solution
import api.Verdict
import kotlinx.coroutines.launch
import kotlinx.html.ThScope
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import react.*
import react.dom.*

external interface ProblemSolutionsProps : RProps {
    var problem: Problem
    var solutions: List<Solution>
}

val ProblemSolutions = functionalComponent<ProblemSolutionsProps> { props ->

    val (verdicts, setVerdicts) = useState(mapOf<String, Verdict>())
    val (isRunning, setRunning) = useState(false)

    useEffectWithCleanup(listOf(isRunning)) {
        if (!isRunning) return@useEffectWithCleanup {}
        var cancelled = false

        scope.launch {
            try {
                Api.testAllSolutions(props.problem) {
                    console.log("[INFO] cancelled = $cancelled")
                    console.log("[INFO] new verdicts = $it")
                    if (!cancelled) {
                        setVerdicts(it)
                    }
                }
            } finally {
                if (!cancelled) {
                    setRunning(false)
                }
            }
        }

        return@useEffectWithCleanup {
            cancelled = true
            setRunning(false)
        }
    }

    console.log("[INFO] verdicts = $verdicts")

    h3("my-3 text-center") {
        span("me-2") { +"Решения" }
        if (isRunning) {
            span("spinner-border text-secondary") {
                attrs { role = "status" }
            }
        } else {
            span("btn btn-primary") {
                +"Протестировать все"
                attrs {
                    onClickFunction = {
                        setRunning(true)
                    }
                }
            }
        }
    }
    div("container") {
        div("row") {
            div("col") {
                table("table") {
                    thead {
                        tr {
                            th { +"Имя"; attrs { scope = ThScope.col } }
                            th { +"Язык"; attrs { scope = ThScope.col } }
                            th { +"Ожидаемый вердикт"; attrs { scope = ThScope.col } }
                            th { +"Вердикт на баксе"; attrs { scope = ThScope.col } }
                        }
                    }
                    tbody {
                        for (solution in props.solutions) {
                            tr {
                                th {
                                    +solution.name
                                    attrs { scope = ThScope.row }
                                }
                                td { +solution.language.description }
                                td { +solution.expectedVerdict.tag }
                                td {
                                    if (isRunning) {
                                        div("spinner-border text-secondary") {
                                            attrs { role = "status" }
                                        }
                                    } else {
                                        +(verdicts[solution.name]?.tag ?: "-")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}