import api.Problem
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import react.PropsWithChildren
import react.dom.*
import react.fc

external interface ProblemListProps : PropsWithChildren {
    var problems: List<Problem>
    var selectedProblem: Problem?
    var setSelectedProblem: (Problem) -> Unit
}

val ProblemList = fc<ProblemListProps> { props ->
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
