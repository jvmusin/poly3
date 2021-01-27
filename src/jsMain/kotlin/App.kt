import api.Problem
import api.ProblemInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.*
import react.functionalComponent
import react.useEffect
import react.useState
import styled.*

val scope = MainScope()

val Problem.isAccessible get() = latestPackage != null && accessType != Problem.AccessType.READ

val App = functionalComponent<RProps> {
    val (problems, setProblems) = useState(emptyList<Problem>())
    val (activeProblem, setActiveProblem) = useState<Problem?>(null)
    val (problemInfo, setProblemInfo) = useState<ProblemInfo?>(null)

    useEffect(emptyList()) {
        scope.launch {
            setProblems(getProblems().sortedByDescending { it.id })
        }
    }

    useEffect(listOf(activeProblem)) {
        if (activeProblem != null) {
            println("selected problem $activeProblem")
            scope.launch {
                setProblemInfo(null)
                if (activeProblem.isAccessible) {
                    setProblemInfo(getProblemInfo(activeProblem.id))
                }
            }
        }
    }

    styledDiv {
        styledDiv {
            css {
                display = Display.flex
                flexDirection = FlexDirection.column
                alignItems = Align.center
            }
            h1 { +"Это конвертер задач из полигона в бакс Полибакс" }
            h2 { +"Чтобы твоя задача появилась в списке, добавь WRITE права на неё пользователю Musin" }
        }

        styledDiv {
            css {
                display = Display.flex
                justifyContent = JustifyContent.center
            }

            styledDiv {
                css {
                    margin = 0.5.em.toString()
                    padding = 0.5.em.toString()
                    backgroundColor = Color.aliceBlue
                }
                h2 { +"Доступные задачи. Нажми на любую:" }
                nav {
                    styledUl {
                        css {
                            backgroundColor = Color.cornsilk
                            height = 40.em
                            width = 30.em
                            overflow = Overflow.hidden
                            overflowY = Overflow.scroll
                            fontSize = 1.20.em
                        }
                        for (problem in problems) {
                            styledLi {
                                css {
                                    if (problem == activeProblem) fontWeight = FontWeight.bold
                                    color = if (problem.isAccessible) Color.green else Color.red
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
                                var text = "${problem.id}: ${problem.name} ($packageInfo)"
                                if (problem.accessType == Problem.AccessType.READ)
                                    text += " (NEED WRITE ACCESS)"
                                +text
                            }
                        }
                    }
                }
            }

            styledDiv {
                css {
                    width = 25.em
                    margin = 0.5.em.toString()
                    padding = 0.5.em.toString()
                    backgroundColor = Color.aliceBlue
                }
                if (activeProblem != null && activeProblem.isAccessible) {
                    h2 { +"Свойства задачи:" }
                    if (problemInfo != null) {
                        div {
                            h3 { +"Название: ${activeProblem.name}" }
                            h3 { +"Автор: ${activeProblem.owner}" }
                            h3 { +"Ввод: ${problemInfo.inputFile}" }
                            h3 { +"Вывод: ${problemInfo.outputFile}" }
                            h3 { +"Интерактивная: ${if (problemInfo.interactive) "Да" else "Нет"}" }
                            h3 { +"Ограничение времени: ${problemInfo.timeLimitMillis / 1000.0}s" }
                            h3 { +"Ограничение памяти: ${problemInfo.memoryLimitMegabytes}MB" }
                        }
                        if (activeProblem.isAccessible) {
                            div {
                                styledA {
                                    css {
                                        fontSize = 30.px
                                        color = Color.hotPink
                                    }
//                                +"А теперь нажми сюда, чтобы скачать архив для сайбона. Замечательно, правда? А если кто-нибудь (Артём) запилит заливку архивов в сайбон, будет вообще чудесно(, Артём)"
                                    +"Скачать архив"
                                    attrs {
                                        href = downloadPackageLink(activeProblem.id)
                                    }
                                }
                            }
                        } else {
                            styledP {
                                css {
                                    fontSize = 30.px
                                    color = Color.red
                                }
                                +"Пакет не собран!"
                            }
                        }
                    }
                }
            }
        }
    }
}
