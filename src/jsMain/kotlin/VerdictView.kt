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
    span {
        a {
            +props.verdict.tag
            attrs["data-tip"] = ""
            attrs["data-for"] = props.id
        }
        ReactTooltip.default {
            attrs {
                id = props.id
                type = "dark"
            }
            span {
                +props.verdict.description
            }
        }
    }
}