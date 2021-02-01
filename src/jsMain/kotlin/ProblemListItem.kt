import api.Problem
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RProps
import react.functionalComponent
import styled.css
import styled.styledLi

external interface ProblemListItemProps : RProps {
    var problem: Problem
    var isSelected: Boolean
    var onProblemSelect: (Problem) -> Unit
}

val ProblemListItem = functionalComponent<ProblemListItemProps> { props ->
    val problem = props.problem
    styledLi {
        css {
            fontSize = 1.20.em
            if (props.isSelected) fontWeight = FontWeight.bold
            color = if (problem.isAccessible) Color.green else Color.red
        }
        key = "problem-${problem.id}"
        attrs {
            onClickFunction = {
                props.onProblemSelect(problem)
            }
        }
        val packageInfo = when (val pack = problem.latestPackage) {
            null -> "no packages built"
            else -> "package $pack"
        }
        var text = "${problem.id}: ${problem.name} ($packageInfo)"
        if (problem.accessType == Problem.AccessType.READ)
            text += " (NEED WRITE ACCESS)"
        +text
    }
}