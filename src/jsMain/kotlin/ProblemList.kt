import api.Problem
import kotlinext.js.jsObject
import kotlinx.css.*
import react.*
import react.dom.h2
import styled.css
import styled.styledDiv
import styled.styledUl

external interface ProblemListProps : RProps {
    var problems: List<Problem>
    var activeProblem: Problem?
    var onProblemSelect: (Problem?) -> Unit
}

val ProblemList = functionalComponent<ProblemListProps> { props ->
    styledDiv {
        css {
            +Styles.module
            width = 30.em
            display = Display.flex
            flexDirection = FlexDirection.column
        }
        h2 { +"Доступные задачи. Нажми на любую:" }
        styledDiv {
            css {
                overflow = Overflow.hidden
                overflowY = Overflow.scroll
            }
            styledUl {
                css {
                    backgroundColor = Color.cornsilk
                    flexGrow = 1.0
                }
                for (problem in props.problems) {
                    child(ProblemListItem, jsObject {
                        this@jsObject.problem = problem
                        isSelected = problem == props.activeProblem
                        this@jsObject.onProblemSelect = props.onProblemSelect
                    })
                }
            }
        }
    }
}
