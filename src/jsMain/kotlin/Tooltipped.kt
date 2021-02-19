import react.RBuilder
import react.dom.a
import react.dom.span

fun RBuilder.tooltipped(id: String, value: String, tooltip: String) {
    span {
        a {
            +value
            attrs["data-tip"] = ""
            attrs["data-for"] = id
        }
        ReactTooltip.default {
            attrs { this.id = id }
            span { +tooltip }
        }
    }
}