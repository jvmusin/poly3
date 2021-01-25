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
            println("selected problem $activeProblem")
            scope.launch {
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
        }

        styledDiv {
            css {
                margin = 5.px.toString()
                padding = 5.px.toString()
                backgroundColor = Color.aliceBlue
            }
            h2 { +"Доступные задачи. Нажми на любую:" }
            ul {
                for (problem in problems) {
                    styledLi {
                        css {
                            if (problem == activeProblem) fontWeight = FontWeight.bold
                            color = if (problem.latestPackage != null) Color.green else Color.red
                        }
                        key = "problem-${problem.id}"
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
                    padding = 5.px.toString()
                    backgroundColor = Color.aliceBlue
                }
                h2 { +"Свойства задачи:" }
                if (problemInfo != null) {
                    div {
                        p { +"Ввод: ${problemInfo.inputFile}" }
                        p { +"Вывод: ${problemInfo.outputFile}" }
                        p { +"Интерактивная: ${if (problemInfo.interactive) "Да" else "Нет"}" }
                        p { +"Ограничение времени: ${problemInfo.timeLimitMillis / 1000.0}s" }
                        p { +"Ограничение памяти: ${problemInfo.memoryLimitMegabytes}MB" }
                    }
                    if (activeProblem.latestPackage != null) {
                        div {
                            styledA {
                                css {
                                    fontSize = 30.px
                                    color = Color.hotPink
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
