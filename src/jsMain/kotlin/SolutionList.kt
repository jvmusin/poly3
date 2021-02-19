import api.Problem
import api.Solution
import api.Toast
import kotlinext.js.jsObject
import kotlinx.coroutines.launch
import kotlinx.html.ThScope
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import react.*
import react.dom.*

external interface SolutionListProps : RProps {
    var problem: Problem
    var solutions: List<Solution>
}

val SolutionList = functionalComponent<SolutionListProps> { props ->
    val (isRunning, setRunning) = useState(false)
    val (solutionsTriggered, setSolutionsTriggered) = useState(false)
    val (sybonProblemId, setSybonProblemId) = useState<Int?>(null)

    useEffect(listOf(props.problem)) {
        setSolutionsTriggered(false)
    }

    useEffectWithCleanup(listOf(props.problem, isRunning)) {
        if (!isRunning) return@useEffectWithCleanup {}
        var cancelled = false

        scope.launch {
            val newSybonProblemId = Api.prepareProblem(props.problem)
            if (!cancelled) {
                setSybonProblemId(newSybonProblemId)
                setSolutionsTriggered(true)
                setRunning(false)
                showToast(Toast(props.problem.name, "Тестируем решения"))
            }
        }

        return@useEffectWithCleanup {
            cancelled = true
            setRunning(false)
        }
    }

    h3("my-3 text-center") {
        span("me-2") { +"Решения" }
        if (isRunning) {
            span("spinner-border text-secondary") {
                attrs { role = "status" }
            }
        } else {
            span("btn btn-primary") {
                +"Протестировать все решения в баксе на ограничениях из полигона"
                attrs { onClickFunction = { setRunning(true) } }
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
                            th { +"Полигон"; attrs { scope = ThScope.col } }
                            th { +"Сайбон"; attrs { scope = ThScope.col } }
                            th { +"Тест"; attrs { scope = ThScope.col } }
                            th { +"Время"; attrs { scope = ThScope.col } }
                            th { +"Память"; attrs { scope = ThScope.col } }
                            th { +"Рантайм"; attrs { scope = ThScope.col } }
                        }
                    }
                    tbody {
                        for (solution in props.solutions) {
                            child(SolutionRow, jsObject {
                                this.problem = props.problem
                                this.solution = solution
                                this.runTriggered = solutionsTriggered
                                this.sybonProblemId = sybonProblemId
                            })
                        }
                    }
                }
            }
        }
    }
}