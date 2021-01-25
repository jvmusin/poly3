import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.*
import react.functionalComponent
import react.useEffect
import react.useState
import styled.css
import styled.styledA
import styled.styledDiv
import styled.styledLi

val scope = MainScope()

val App = functionalComponent<RProps> {
    val (problems, setProblems) = useState(emptyList<Problem>())
    val (activeProblem, setActiveProblem) = useState<Problem?>(null)
    val (problemInfo, setProblemInfo) = useState<ProblemInfo?>(null)

    useEffect(emptyList()) {
        scope.launch {
            setProblems(getProblems().sortedByDescending { it.id }.take(25))
        }
    }

    useEffect(listOf(activeProblem)) {
        if (activeProblem != null) {
            scope.launch {
                println("selected problem $activeProblem")
                setProblemInfo(null)
                setProblemInfo(getProblemInfo(activeProblem.id))
            }
        }
    }

    div {
        h1 {
            +"Это конвертер задач из полигона в бакс Полибакс"
        }
    }

    styledDiv {
        css {
            display = Display.flex
            justifyContent = JustifyContent.flexStart
            padding = 10.px.toString()
            backgroundColor = Color.gray
            boxSizing = BoxSizing.borderBox
        }

        styledDiv {
            css {
                margin = 5.px.toString()
                backgroundColor = Color.lightGray
            }
            h2 { +"Доступные задачи. Нажми на любую:" }
            ul {
                for (problem in problems) {
                    styledLi {
                        css {
                            if (problem == activeProblem) fontWeight = FontWeight.bold
                            color = if (problem.latestPackage != null) Color.green else Color.red
                        }
                        key = problem.id.toString()
                        attrs {
                            onClickFunction = {
                                setActiveProblem(problem)
                            }
                        }
                        val packageInfo =
                            if (problem.latestPackage != null)
                                "package ${problem.latestPackage}"
                            else
                                "no packages built"
                        +"${problem.id}: ${problem.name} ($packageInfo)"
                    }
                }
            }
        }

        if (activeProblem != null) {
            styledDiv {
                css {
                    margin = 5.px.toString()
                    backgroundColor = Color.lightGray
                }
                h2 { +"Свойства задачи" }
                if (problemInfo != null) {
                    div {
                        p { +"Input: ${problemInfo.inputFile}" }
                        p { +"Output: ${problemInfo.outputFile}" }
                        p { +"Interactive: ${if (problemInfo.interactive) "Да" else "Нет"}" }
                        p { +"Time limit: ${problemInfo.timeLimitMillis / 1000.0}s" }
                        p { +"Memory limit: ${problemInfo.memoryLimitMegabytes}MB" }
                    }
                    if (activeProblem.latestPackage != null) {
                        div {
                            styledA {
                                css {
                                    fontSize = 30.px
                                    color = Color.orange

                                }
//                                    +"А теперь нажми сюда, чтобы скачать архив для сайбона. Замечательно, правда? А если кто-нибудь (Артём) запилит заливку архивов в сайбон, будет вообще чудесно(, Артём)"
                                +"Скачать архив"
                                attrs {
                                    href = downloadPackageLink(activeProblem.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
