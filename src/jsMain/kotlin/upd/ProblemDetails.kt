package upd

import api.AdditionalProblemProperties
import api.Problem
import api.ProblemInfo
import downloadPackage
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.dom.button
import react.dom.div
import react.dom.h2
import react.dom.input
import react.functionalComponent
import react.useEffect
import react.useState
import transferToBacsArchive
import kotlin.math.roundToInt

external interface ProblemDetailsProps : RProps {
    var problem: Problem?
    var problemInfo: ProblemInfo?
}

val ProblemDetails = functionalComponent<ProblemDetailsProps> { props ->
    val problem = props.problem
    val problemInfo = props.problemInfo

    val (prefix, setPrefix) = useState("polybacs-")
    val (suffix, setSuffix) = useState("")
    val (timeLimitSeconds, setTimeLimitSeconds) = useState(
        if (problemInfo != null) (problemInfo.timeLimitMillis / 1000.0).toString() else ""
    )
    val (memoryLimitMegabytes, setMemoryLimitMegabytes) = useState(
        if (problemInfo != null) (problemInfo.memoryLimitMegabytes).toString() else ""
    )
    val (finalProblemName, setFinalProblemName) = useState("")
    fun buildAdditionalProperties() = AdditionalProblemProperties(
        prefix, suffix, (timeLimitSeconds.toDouble() * 1000).roundToInt(), memoryLimitMegabytes.toInt()
    )

    useEffect(listOf(problem, prefix, suffix)) {
        scope.launch {
            if (problem == null) {
                setFinalProblemName("")
            } else {
                setFinalProblemName("$prefix${problem.name}$suffix")
            }
        }
    }

    useEffect(listOf(problemInfo)) {
        if (problemInfo != null) {
            scope.launch {
                setTimeLimitSeconds((problemInfo.timeLimitMillis / 1000.0).toString())
                setMemoryLimitMegabytes(problemInfo.memoryLimitMegabytes.toString())
            }
        }
    }

    h2("my-3 text-center") { +"Свойства задачи" }
    if (problem != null) {
        div("container") {
            div("row gx-4 gy-2") {
                fun draw(name: String, value: Any?) {
                    div("col-6 text-end") {
                        +"$name:"
                    }
                    div("col-6") {
                        +value.toString()
                    }
                }

                fun drawEditable(name: String, value: Any?, setValue: (String) -> Unit) {
                    div("col-6 text-end") {
                        +"$name:"
                    }
                    div("col-6") {
                        div("w-100") {
                            input(type = InputType.text) {
                                attrs {
                                    this.value = value.toString()
                                    onChangeFunction = { setValue((it.target as HTMLInputElement).value) }
                                }
                            }
                        }
                    }
                }

                draw("Название", problem.name)
                draw("Автор", problem.owner)
                draw("Доступ", problem.accessType)
                if (problemInfo != null) {
                    draw("Ввод", problemInfo.inputFile)
                    draw("Вывод", problemInfo.outputFile)
                    draw("Интерактивная", if (problemInfo.interactive) "Да" else "Нет")
                    drawEditable("Ограничение времени (сек)", timeLimitSeconds, setTimeLimitSeconds)
                    drawEditable("Ограничение памяти (MB)", memoryLimitMegabytes, setMemoryLimitMegabytes)
                    drawEditable("Добавить префикс", prefix, setPrefix)
                    drawEditable("Добавить суффикс", suffix, setSuffix)
                    div("col text-center") { +finalProblemName }
                }
            }
            if (problemInfo != null) {
                div("d-flex mt-3") {
                    button(type = ButtonType.button, classes = "btn btn-secondary btn-lg me-1 w-50") {
                        +"Скачать пакет"
                        attrs {
                            onClickFunction = {
                                scope.launch { downloadPackage(problem, buildAdditionalProperties()) }
                            }
                        }
                    }
                    button(type = ButtonType.button, classes = "btn btn-primary btn-lg ms-1 w-50") {
                        +"Закинуть в бакс"
                        attrs {
                            onClickFunction = {
                                scope.launch { transferToBacsArchive(problem.id, buildAdditionalProperties()) }
                            }
                        }
                    }
                }
            }
        }
    }
}