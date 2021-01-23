import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import polygon.Problem
import react.RProps
import react.dom.div
import react.dom.h1
import react.dom.li
import react.dom.ul
import react.functionalComponent
import react.useEffect
import react.useState

val scope = MainScope()

val App = functionalComponent<RProps> {
    val (problems, setProblems) = useState(emptyList<Problem>())

    useEffect(emptyList()) {
        scope.launch {
            setProblems(getProblems())
        }
    }

    h1 {
        +"Hello, here are the problems:"
    }
    div {
        ul {
            for (problem in problems) {
                li {
                    key = problem.id.toString()
                    +"$problem"
                }
            }
        }
    }
}
