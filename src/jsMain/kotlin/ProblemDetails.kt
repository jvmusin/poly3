import api.AdditionalProblemProperties
import api.Problem
import api.ProblemInfo
import kotlinext.js.jsObject
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import styled.*
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

    styledDiv {
        css {
            +Styles.module
            width = 40.em
        }
        h2 { +"Свойства ${problem?.name ?: "задачи"}" }
        if (problem == null || problemInfo == null || !problem.isAccessible) return@styledDiv
        styledDiv {
            css { marginTop = 1.em }
            styledTable {
                css { +Styles.problemDetailsTable }
                fun textRow(name: String, value: String) = child(ProblemDetailsTableRow, jsObject {
                    this@jsObject.name = name
                    buildValue = { it.apply { +value } }
                })

                fun textFieldRow(name: String, value: String, onUpdate: (String) -> Unit) =
                    child(ProblemDetailsTableRow, jsObject {
                        this@jsObject.name = name
                        buildValue = {
                            it.apply {
                                input(InputType.text) {
                                    attrs {
                                        this@attrs.value = value
                                        onChangeFunction = { it -> onUpdate((it.target as HTMLInputElement).value) }
                                    }
                                }
                            }
                        }
                    })

                tbody {
                    textRow("Название", problem.name)
                    textRow("Автор", problem.owner)
                    textRow("Ввод", problemInfo.inputFile)
                    textRow("Вывод", problemInfo.outputFile)
                    textRow("Интерактивная", if (problemInfo.interactive) "Да" else "Нет")
                    textFieldRow("Ограничение времени (сек)", timeLimitSeconds, setTimeLimitSeconds)
                    textFieldRow("Ограничение памяти (MB)", memoryLimitMegabytes, setMemoryLimitMegabytes)
                    textFieldRow("Добавить префикс", prefix, setPrefix)
                    textFieldRow("Добавить суффикс", suffix, setSuffix)
                    textRow("Полное название", finalProblemName)
                }
            }
        }
        if (problem.isAccessible) {
            styledDiv {
                css {
                    display = Display.flex
                    "*" {
                        fontSize = 2.5.em
                        color = Color.hotPink
                    }
                }
                styledButton {
                    css { marginRight = 0.2.em }
                    +"Скачать пакет"
                    attrs {
                        onClickFunction = {
                            scope.launch {
                                downloadPackage(problem, buildAdditionalProperties())
                            }
                        }
                    }
                }
                styledButton {
                    css { marginLeft = 0.2.em }
                    +"Загрузить в BACS"
                    attrs {
                        onClickFunction = {
                            fun logAlert(text: String) {
                                console.log(text)
                                window.alert(text)
                            }
                            scope.launch {
                                var fail = false
                                try {
                                    val sybonId = transferToBacsArchive(problem.id, buildAdditionalProperties())
                                    if (sybonId != -1) {
                                        logAlert("Задача ${problem.name} загружена в BACS и имеет SybonId $sybonId")
                                    } else {
                                        fail = true
                                    }
                                } catch (ex: Exception) {
                                    fail = true
                                    println(ex)
                                }
                                if (fail) {
                                    logAlert("Задача ${problem.name} не загрузилась в BACS((")
                                }
                            }
                            logAlert("Задача ${problem.name} поставлена в очередь на загрузку в BACS")
                        }
                    }
                }
            }
        }
    }
}