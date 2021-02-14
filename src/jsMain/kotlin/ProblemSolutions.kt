import api.Solution
import kotlinx.html.ThScope
import react.RProps
import react.dom.*
import react.functionalComponent

external interface ProblemSolutionsProps : RProps {
    var solutions: List<Solution>
}

val ProblemSolutions = functionalComponent<ProblemSolutionsProps> { props ->
    h2("my-3 text-center") { +"Решения" }
    div("container") {
        div("row") {
            div("col") {
                table("table") {
                    thead {
                        tr {
                            th { +"Имя"; attrs { scope = ThScope.col } }
                            th { +"Язык"; attrs { scope = ThScope.col } }
                            th { +"Ожидаемый вердикт"; attrs { scope = ThScope.col } }
//                            th { +"Вердикт на баксе"; attrs { scope = ThScope.col } }
                        }
                    }
                    tbody {
                        for (solution in props.solutions) {
                            tr {
                                th {
                                    +solution.name
                                    attrs { scope = ThScope.row }
                                }
                                td { +solution.language }
                                td { +solution.expectedVerdict }
//                                td { +"" }
                            }
                        }
                    }
                }
            }
        }
    }
}