@file:OptIn(ExperimentalTime::class)

import api.Problem
import api.ProblemInfo
import api.Toast
import api.ToastKind
import kotlinext.js.jsObject
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
import react.*
import react.dom.div
import react.dom.h1
import react.dom.header
import react.dom.span
import kotlin.time.ExperimentalTime

val scope = MainScope()

fun showToast(toast: Toast) {
    document.getElementById("notifications")!!.append {
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
    }
    js("new bootstrap.Toast(document.getElementsByClassName('toast-container')[0].lastChild,{animation:true,autohide:true,delay:60000}).show()")
}

val App = functionalComponent<RProps> {
    val (problems, setProblems) = useState<List<Problem>>(emptyList())
    val (selectedProblem, setSelectedProblem) = useState<Problem?>(null)
    val (selectedProblemInfo, setSelectedProblemInfo) = useState<ProblemInfo?>(null)

    useEffect(emptyList()) {
        scope.launch {
            Api.registerNotifications()
        }
        scope.launch {
            setProblems(Api.getProblems().sortedByDescending { it.id })
        }
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
                        scope.launch {
                            Api.bumpTestNotification()
                        }
                    }
                }
            }
            span("navbar-brand m-0") {
                +"Чтобы твоя задача появилась в списке, добавь WRITE права на неё пользователю Musin"
            }
        }

        div("container") {
            div("row") {
                div("col-4 problem-list") {
                    child(ProblemList, jsObject {
                        this.problems = problems
                        this.selectedProblem = selectedProblem
                        this.setSelectedProblem = { problem ->
                            setSelectedProblem(null)
                            setSelectedProblem(problem)
                            setSelectedProblemInfo(null)
                            scope.launch {
                                setSelectedProblemInfo(Api.getProblemInfo(problem.id))
                            }
                        }
                    })
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