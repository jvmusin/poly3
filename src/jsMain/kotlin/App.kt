import api.Problem
import api.ProblemInfo
import kotlinext.js.jsObject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import react.*
import react.dom.*
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
            scope.launch {
                setProblemInfo(null)
                if (activeProblem.isAccessible) {
                    setProblemInfo(getProblemInfo(activeProblem.id))
                }
            }
        }
    }

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
            flexGrow = 1.0
            minHeight = 0.px
        }
        child(ProblemList, jsObject {
            this@jsObject.problems = problems
            this@jsObject.activeProblem = activeProblem
            this.onProblemSelect = { problem -> scope.launch { setActiveProblem(problem) } }
        })
        child(ProblemDetails, jsObject {
            this.problem = activeProblem
            this.problemInfo = problemInfo
        })
    }
}
