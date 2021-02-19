import api.Verdict
import kotlinx.css.TextAlign
import kotlinx.css.textAlign
import react.RBuilder
import react.dom.a
import styled.styledPre

fun RBuilder.tooltipped(id: String, value: String, build: RBuilder.() -> Unit) {
    a {
        +value
        attrs["data-tip"] = ""
        attrs["data-for"] = id
    }
    ReactTooltip.default {
        attrs.id = id
        build()
    }

}

fun RBuilder.tooltippedText(id: String, value: String, tooltip: String) {
    tooltipped(id, value) { +tooltip }
}

fun RBuilder.verdict(id: String, verdict: Verdict, description: String? = null) {
    if (verdict == Verdict.COMPILATION_ERROR && description != null) {
        verdictWithMessage(id, verdict, description)
    } else {
        tooltippedText(id, verdict.tag, description ?: verdict.description)
    }
}

private fun RBuilder.verdictWithMessage(id: String, verdict: Verdict, message: String) {
    tooltipped(id, verdict.tag) {
        styledPre {
            +message
            css.textAlign = TextAlign.left
        }
    }
}