@file:OptIn(ExperimentalTime::class)

import api.Problem
import api.ProblemInfo
import api.Toast
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
import react.dom.*
import kotlin.time.ExperimentalTime

val scope = MainScope()

fun showToast(toast: Toast) {
    document.getElementById("notifications")!!.append {
        div("toast") {
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
    js("new bootstrap.Toast(document.getElementsByClassName('toast-container')[0].lastChild).show()")
}

val App = functionalComponent<RProps> {
    val (problems, setProblems) = useState<List<Problem>>(emptyList())
    val (selectedProblem, setSelectedProblem) = useState<Problem?>(null)
    val (selectedProblemInfo, setSelectedProblemInfo) = useState<ProblemInfo?>(null)

    useEffect(emptyList()) {
        scope.launch {
            registerNotifications()
            setProblems(getProblems().sortedByDescending { it.id })
        }
    }

    div("toast-container") {
        attrs { id = "notifications" }
    }

    div {
        header("navbar navbar-light bg-light shadow-sm flex-column") {
            h1("m-0 display-4 text-center") {
                +"Это конвертер задач из полигона в бакс Полибакс"
                attrs {
                    onClickFunction = {
                        scope.launch {
                            postRequest("bump-test-notification")
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
                    h2("my-3 text-center") { +"Доступные задачи" }
                    ul("list-group") {
                        for (p in problems) {
                            val classes = mutableListOf("list-group-item", "list-group-item-action")
                            when {
                                selectedProblem == p -> classes += "active"
                                p.accessType == Problem.AccessType.READ -> classes += "disabled"
                                p.latestPackage == null -> classes += "list-group-item-warning"
                            }
                            button(type = ButtonType.button, classes = classes.joinToString(" ")) {
                                div("d-flex justify-content-between") {
                                    span { +p.name }
                                    span("text-nowrap") { i("bi bi-person-fill") { }; +p.owner }
                                }
                                div("d-flex justify-content-between ${if (selectedProblem == p) "" else "text-secondary"}") {
                                    small { +p.id.toString() }
                                    small {
                                        when {
                                            p.accessType == Problem.AccessType.READ ->
                                                strong("text-dark") { +"Нет WRITE доступа" }
                                            p.latestPackage == null ->
                                                strong("text-dark") { +"Не собран пакет" }
                                            else ->
                                                +"rev. ${p.latestPackage}"
                                        }
                                    }
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