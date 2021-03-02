import api.Problem
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.button
import react.dom.div
import react.dom.h2
import react.dom.i
import react.dom.small
import react.dom.span
import react.dom.strong
import react.dom.ul
import react.functionalComponent

external interface ProblemListProps : RProps {
    var problems: List<Problem>
    var selectedProblem: Problem?
    var setSelectedProblem: (Problem) -> Unit
}

val ProblemList = functionalComponent<ProblemListProps> { props ->
    h2("my-3 text-center") { +"Доступные задачи" }
    ul("list-group") {
        for (p in props.problems) {
            val classes = mutableListOf("list-group-item", "list-group-item-action")
            when {
                props.selectedProblem == p -> classes += "active"
                p.accessType.notSufficient -> classes += "disabled"
                p.latestPackage == null -> classes += "list-group-item-warning"
            }
            button(type = ButtonType.button, classes = classes.joinToString(" ")) {
                div("d-flex justify-content-between") {
                    span { +p.name }
                    span("text-nowrap") { i("bi bi-person-fill") { }; +p.owner }
                }
                div("d-flex justify-content-between ${if (props.selectedProblem == p) "" else "text-secondary"}") {
                    small { +p.id.toString() }
                    small {
                        when {
                            p.accessType.notSufficient ->
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
                        props.setSelectedProblem(p)
                    }
                }
            }
        }
    }
}
