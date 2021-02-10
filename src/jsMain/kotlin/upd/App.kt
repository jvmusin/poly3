package upd

import api.Problem
import api.ProblemInfo
import getProblemInfo
import getProblems
import kotlinext.js.jsObject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

val scope = MainScope()

val App = functionalComponent<RProps> {
    val (problems, setProblems) = useState<List<Problem>>(emptyList())
    val (selectedProblem, setSelectedProblem) = useState<Problem?>(null)
    val (selectedProblemInfo, setSelectedProblemInfo) = useState<ProblemInfo?>(null)

    useEffect(emptyList()) {
        scope.launch {
            setProblems(getProblems().sortedByDescending { it.id })
        }
    }

    div {
        header("navbar navbar-light bg-light shadow-sm") {
            div("container justify-content-center") {
                h1("m-0") {
                    +"Это конвертер задач из полигона в бакс Полибакс"
                }
                span("navbar-brand m-0") {
                    +"Чтобы твоя задача появилась в списке, добавь WRITE права на неё пользователю Musin"
                }
            }
        }

        div("container") {
            div("row") {
                div("col-4 problem-list") {
                    h2("my-3 text-center") { +"Доступные задачи" }
                    ul("overflow-auto list-group") {
                        for (p in problems) {
                            val classes = mutableListOf("list-group-item", "list-group-item-action")
                            if (selectedProblem == p) classes += "active"
                            else if (p.latestPackage == null) classes += "list-group-item-warning"
                            button(type = ButtonType.button, classes = classes.joinToString(" ")) {
                                div("d-flex justify-content-between") {
                                    span { +p.name }
                                    span("text-nowrap") { i("bi bi-person-fill") { }; +p.owner }
                                }
                                div("d-flex justify-content-between ${if (selectedProblem == p) "" else "text-secondary"}") {
                                    small { +p.id.toString() }
                                    small { +(p.latestPackage?.let { "rev. $it" } ?: "") }
                                }
                                attrs {
                                    onClickFunction = {
                                        setSelectedProblem(null)
                                        setSelectedProblem(p)
                                        setSelectedProblemInfo(null)
                                        scope.launch {
                                            setSelectedProblemInfo(getProblemInfo(p.id))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                div("col-8 problem-details") {
                    child(ProblemDetails, jsObject {
                        problem = selectedProblem
                        problemInfo = selectedProblemInfo
                    })
                }
            }
        }
    }
}