import api.AdditionalProblemProperties
import api.Problem
import api.ProblemInfo
import api.Toast
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.dom.*
import react.functionalComponent
import react.useEffect
import react.useState
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
        div("container gy-3") {
            fun draw(name: String, property: String, initialValue: String, setValue: ((String) -> Unit)? = null) {
                div("row align-items-center") {
                    label("col col-form-label text-end") {
                        +"$name:"
                        attrs["htmlFor"] = "problem-$property"
                    }
                    if (setValue != null) {
                        div("col") {
                            input(InputType.text, classes = "form-control") {
                                attrs {
                                    id = "problem-$property"
                                    value = initialValue
                                    onChangeFunction = { setValue((it.target as HTMLInputElement).value) }
                                }
                            }
                        }
                    } else {
                        label("col col-form-label") {
                            +initialValue
                            attrs { id = "problem-$property" }
                        }
                    }
                }
            }

            draw("Название", "name", problem.name)
            draw("Автор", "author", problem.owner)
            if (problemInfo != null) {
                draw("Ввод", "input", problemInfo.inputFile)
                draw("Вывод", "output", problemInfo.outputFile)
                draw("Ограничение времени (сек)", "tl", timeLimitSeconds, setTimeLimitSeconds)
                draw("Ограничение памяти (MB)", "ml", memoryLimitMegabytes, setMemoryLimitMegabytes)
                draw("Добавить префикс", "prefix", prefix, setPrefix)
                draw("Добавить суффикс", "suffix", suffix, setSuffix)
                div("row") { div("col text-center") { +finalProblemName } }
            }
            //todo add normal check
            if (problemInfo != null && problem.accessType != Problem.AccessType.READ && problem.latestPackage != null) {
                div("row my-3") {
                    div("col") {
                        button(type = ButtonType.button, classes = "btn btn-secondary btn-lg w-100") {
                            +"Скачать пакет"
                            attrs {
                                onClickFunction = {
                                    showToast(Toast(finalProblemName, "Начата сборка архива"))
                                    scope.launch {
                                        downloadPackage(problem, buildAdditionalProperties())
                                        showToast(Toast(finalProblemName, "Пакет собран"))
                                    }
                                }
                            }
                        }
                    }
                    div("col") {
                        button(type = ButtonType.button, classes = "btn btn-primary btn-lg w-100") {
                            +"Закинуть в бакс"
                            attrs {
                                onClickFunction = {
                                    showToast(Toast(finalProblemName, "Начата сборка архива и загрузка его в бакс"))
                                    scope.launch {
                                        transferToBacsArchive(problem.id, buildAdditionalProperties())
                                        showToast(Toast(finalProblemName, "Задача загружена в бакс"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}