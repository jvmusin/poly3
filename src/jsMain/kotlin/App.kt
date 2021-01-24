import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import polygon.Package
import polygon.Problem
import react.RProps
import react.dom.div
import react.dom.h1
import react.dom.ul
import react.functionalComponent
import react.useEffect
import react.useState
import styled.css
import styled.styledA
import styled.styledLi
import kotlin.js.Date

val scope = MainScope()

val App = functionalComponent<RProps> {
    val (problems, setProblems) = useState(emptyList<Problem>())
    val (activeProblem, setActiveProblem) = useState<Problem?>(null)
    val (packages, setPackages) = useState<List<Package>>(emptyList())
    val (activePackage, setActivePackage) = useState<Package?>(null)

    useEffect(emptyList()) {
        scope.launch {
            setProblems(getProblems().sortedByDescending { it.id }.take(15))
        }
    }

    useEffect(listOf(activeProblem)) {
        if (activeProblem != null) {
            scope.launch {
                println("selected problem $activeProblem")
                setPackages(emptyList())
                setActivePackage(null)
                setPackages(getPackages(activeProblem.id).sortedByDescending { it.revision })
            }
        }
    }

    div {
        h1 {
            +"Так, ну значит здесь все доступные задачи. Нажми на любую:"
        }
        ul {
            for (problem in problems) {
                styledLi {
                    css {
                        if (problem == activeProblem) fontWeight = FontWeight.bold
                        color = if (problem.accessType == Problem.AccessType.READ) Color.red else Color.green
                    }
                    if (problem.accessType != Problem.AccessType.READ) {
                        attrs {
                            onClickFunction = {
                                setActiveProblem(problem)
                            }
                        }
                    }
                    key = problem.id.toString()
                    +"${problem.id}: ${problem.name} (${problem.accessType})"
                }
            }
        }
    }

    if (activeProblem != null) {
        div {
            h1 { +"Вот тут значится у нас пакеты (на самом деле бесполезные, потом выпилю). Нажми на любой:" }
            ul {
                for (pack in packages) {
                    styledLi {
                        if (pack == activePackage) {
                            css {
                                fontWeight = FontWeight.bold
                            }
                        }
                        key = pack.id.toString()
                        +"revision: ${pack.revision}, created ${
                            Date(pack.creationTimeSeconds * 1000)
                                .toLocaleString(locales = arrayOf("Russian"))
                        }"
                        attrs {
                            onClickFunction = {
                                setActivePackage(pack)
                            }
                        }
                    }
                }
            }
        }

        if (activePackage != null) {
            styledA {
                css {
                    fontSize = 30.px
                    color = Color.orange
                }
                +"А теперь нажми сюда, чтобы скачать архив для сайбона. Замечательно, правда? А если кто-нибудь (Артём) запилит заливку архивов в сайбон, будет вообще чудесно(, Артём)"
                attrs {
                    href = getDownloadPackageLink(activeProblem.id, activePackage.id)
                }
            }
        }
    }
}
