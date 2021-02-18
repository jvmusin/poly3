import api.Verdict
import react.RProps
import react.dom.a
import react.dom.span
import react.functionalComponent

external interface VerdictViewProps : RProps {
    var id: String
    var verdict: Verdict
}

val VerdictView = functionalComponent<VerdictViewProps> { props ->
    val id = props.id
    span {
        a {
            +props.verdict.tag
            attrs["data-tip"] = ""
            attrs["data-for"] = id
        }
        ReactTooltip.default {
            attrs {
                this.id = id
                type = "info"
            }
            span {
                +props.verdict.description
            }
        }
    }
}