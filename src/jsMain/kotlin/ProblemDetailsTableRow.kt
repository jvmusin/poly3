import kotlinx.css.*
import react.RBuilder
import react.RProps
import react.functionalComponent
import styled.css
import styled.styledTd
import styled.styledTr

external interface ProblemDetailsTableRowProps : RProps {
    var name: String
    var buildValue: (RBuilder) -> Unit
}

val ProblemDetailsTableRow = functionalComponent<ProblemDetailsTableRowProps> { props ->
    styledTr {
        styledTd {
            css {
                padding(0.5.em)
                paddingRight = 1.em
                textAlign = TextAlign.right
            }
            +props.name
        }
        styledTd {
            props.buildValue(this)
        }
    }
}