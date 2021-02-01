import api.Problem
import api.ProblemInfo
import kotlinext.js.jsObject
import kotlinx.css.*
import react.RProps
import react.child
import react.dom.div
import react.dom.h2
import react.dom.h3
import react.functionalComponent
import styled.*

external interface ProblemDetailsProps : RProps {
    var problem: Problem?
    var problemInfo: ProblemInfo?
}

val ProblemDetails = functionalComponent<ProblemDetailsProps> { props ->
    val problem = props.problem
    val problemInfo = props.problemInfo

    styledDiv {
        css {
            +Styles.module
            width = 40.em
        }
        h2 { +"Свойства задачи:" }
        if (problem == null || problemInfo == null || !problem.isAccessible) return@styledDiv
        div {
            h3 { +"Название: ${problem.name}" }
            h3 { +"Автор: ${problem.owner}" }
            h3 { +"Ввод: ${problemInfo.inputFile}" }
            h3 { +"Вывод: ${problemInfo.outputFile}" }
            h3 { +"Интерактивная: ${if (problemInfo.interactive) "Да" else "Нет"}" }
            h3 { +"Ограничение времени: ${problemInfo.timeLimitMillis / 1000.0}s" }
            h3 { +"Ограничение памяти: ${problemInfo.memoryLimitMegabytes}MB" }
        }
        if (problem.isAccessible) {
            child(DownloadArchiveButton, jsObject { problemId = problem.id })
            child(TransferProblemButton, jsObject { this@jsObject.problem = problem })
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