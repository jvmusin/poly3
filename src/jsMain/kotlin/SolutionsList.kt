import api.Problem
import api.Solution
import api.Toast
import api.ToastKind
import kotlinext.js.jso
import kotlinx.coroutines.launch
import kotlinx.html.ThScope
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import react.PropsWithChildren
import react.dom.*
import react.fc
import react.useEffect
import react.useState

external interface SolutionsListProps : PropsWithChildren {
    var problem: Problem
    var solutions: List<Solution>
}

val SolutionsList = fc<SolutionsListProps> { props ->
    val (isRunning, setRunning) = useState(false)
    val (solutionsTriggered, setSolutionsTriggered) = useState(false)
    val (sybonProblemId, setSybonProblemId) = useState<Int?>(null)

    useEffect(props.problem) {
        setSolutionsTriggered(false)
    }

    useEffect(props.problem, isRunning) {
        if (!isRunning) return@useEffect
        var cancelled = false

        mainScope.launch {
            val newSybonProblemId = try {
                Api.prepareProblem(props.problem)
            } catch (e: Exception) {
                if (!cancelled) {
                    e.printStackTrace()
                    showToast(
                        Toast(
                            "Соединение",
                            "Произошла ошибка, решения не будут протестированы: ${e.message}",
                            ToastKind.FAILURE
                        )
                    )
                    setSolutionsTriggered(false)
                    setRunning(false)
                }
                return@launch
            }
            if (!cancelled) {
                setSybonProblemId(newSybonProblemId)
                setSolutionsTriggered(true)
                setRunning(false)
                showToast(Toast(props.problem.name + "-test", "Тестируем решения"))
            }
        }

        cleanup {
            cancelled = true
            setRunning(false)
        }
    }

    h3("my-3 text-center") {
        span("me-2") { +"Решения" }
        if (isRunning) {
            span("spinner-border text-secondary") {
                attrs.role = "status"
            }
        } else {
            span("btn btn-primary") {
                +"Протестировать все решения в баксе на ограничениях из полигона"
                attrs.onClickFunction = { setRunning(true) }
            }
        }
    }
    div("container") {
        div("row") {
            div("col") {
                table("table table-sm") {
                    thead {
                        tr {
                            th { +"Имя"; attrs.scope = ThScope.col }
                            th { +"Язык"; attrs.scope = ThScope.col }
                            th { +"Полигон"; attrs.scope = ThScope.col }
                            th { +"Сайбон"; attrs.scope = ThScope.col }
                            th { +"Тест"; attrs.scope = ThScope.col }
                            th { +"Время"; attrs.scope = ThScope.col }
                            th { +"Память"; attrs.scope = ThScope.col }
                            th { +"Рантайм"; attrs.scope = ThScope.col }
                        }
                    }
                    tbody {
                        for (solution in props.solutions) {
                            child(
                                SolutionRow,
                                jso {
                                    this.problem = props.problem
                                    this.solution = solution
                                    this.runTriggered = solutionsTriggered
                                    this.sybonProblemId = sybonProblemId
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
