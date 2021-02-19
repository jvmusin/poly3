import api.Verdict
import react.RBuilder

fun RBuilder.verdictView(id: String, verdict: Verdict, description: String? = null) {
    tooltipped(id, verdict.tag, description ?: verdict.description)
}