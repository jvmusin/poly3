import api.*
import kotlinext.js.jso
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.button
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import kotlinx.html.strong
import org.w3c.dom.get
import react.PropsWithChildren
import react.dom.*
import react.fc
import react.useEffect
import react.useState
import kotlin.time.Duration.Companion.seconds

val mainScope = MainScope()

fun showToast(toast: Toast) {
    val toastElement = document.getElementsByClassName("toast-container")[0]!!.append {
        val extraClasses = when (toast.kind) {
            ToastKind.INFORMATION -> "toast-info"
            ToastKind.SUCCESS -> "toast-success"
            ToastKind.FAILURE -> "toast-failure"
        }
        div("toast $extraClasses") {
            attributes["role"] = "alert"
            div("toast-header") {
                strong("me-auto") { +toast.title }
                button(type = ButtonType.button, classes = "btn-close") {
                    attributes["data-bs-dismiss"] = "toast"
                }
            }
            div("toast-body") { +toast.content }
        }
    }.single()
    bootstrap.Toast(toastElement, jso { delay = 60.seconds.inWholeMilliseconds.toInt() }).show()
}

val App = fc<PropsWithChildren> {
    val (problems, setProblems) = useState<List<Problem>>(emptyList())
    val (selectedProblem, setSelectedProblem) = useState<Problem?>(null)
    val (selectedProblemInfo, setSelectedProblemInfo) = useState<ProblemInfo?>(null)
    val (solutions, setSolutions) = useState<List<Solution>>(emptyList())

    useEffect(*arrayOf()) {
        mainScope.launch {
            Api.registerNotifications()
        }
        mainScope.launch {
            setProblems(Api.getProblems().sortedByDescending { it.id })
        }
    }

    useEffect(selectedProblem) {
        var cancelled = false
        if (selectedProblem != null) {
            setSelectedProblemInfo(null)
            setSolutions(emptyList())
            mainScope.launch {
                val problemInfo = Api.getProblemInfo(selectedProblem.id)
                if (!cancelled) setSelectedProblemInfo(problemInfo)
            }
            mainScope.launch {
                val newSolutions = Api.getSolutions(selectedProblem)
                if (!cancelled) setSolutions(newSolutions)
            }
        }
        cleanup { cancelled = true }
    }

    div("toast-container") {
        attrs { id = "notifications" }
    }

    div {
        header("navbar navbar-light bg-light shadow-sm flex-column text-center") {
            h1("m-0 display-4") {
                +"Это конвертер задач из полигона в бакс Полибакс"
                attrs {
                    onClickFunction = {
                        mainScope.launch {
                            Api.bumpTestNotification()
                        }
                    }
                }
            }
            span("navbar-brand m-0") {
                +"Чтобы твоя задача появилась в списке, добавь WRITE права на неё пользователю Musin"
                attrs.onClickFunction = {
                    val element = document.getElementById("notifications")!!.firstElementChild!!
                    bootstrap.Toast(element, jso { delay = 1000 }).show()
                }
            }
        }

        div("container") {
            div("row") {
                div("col-4") {
                    div("problem-list") {
                        child(
                            ProblemList,
                            jso {
                                this.problems = problems
                                this.selectedProblem = selectedProblem
                                this.setSelectedProblem = { problem -> setSelectedProblem(problem) }
                            }
                        )
                    }
                }
                div("col-8") {
                    div("problem-details") {
                        if (selectedProblem != null) {
                            child(
                                ProblemDetails,
                                jso {
                                    problem = selectedProblem
                                    problemInfo = selectedProblemInfo
                                }
                            )
                        }
                    }
                    if (solutions.isNotEmpty()) {
                        div("problem-solutions") {
                            hr { }
                            child(
                                SolutionsList,
                                jso {
                                    this.problem = selectedProblem!!
                                    this.solutions = solutions
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
