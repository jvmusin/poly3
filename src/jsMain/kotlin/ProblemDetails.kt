import api.*
import api.NameAvailability.CHECK_FAILED
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import kotlin.math.roundToInt

external interface ProblemDetailsProps : PropsWithChildren {
    var problem: Problem
    var problemInfo: ProblemInfo?
}

val ProblemDetails = fc<ProblemDetailsProps> { props ->
    val problem = props.problem
    val problemInfo = props.problemInfo

    val (prefix, setPrefix) = useState("polybacs-")
    val (suffix, setSuffix) = useState("")
    val (timeLimitSeconds, setTimeLimitSeconds) = useState("")
    val (memoryLimitMegabytes, setMemoryLimitMegabytes) = useState("")
    val (finalProblemName, setFinalProblemName) = useState("")
    val (statementFormat, setStatementFormat) = useState(StatementFormat.PDF)
    val (nameAvailability, setNameAvailability) = useState(NameAvailability.LOADING)
    fun buildAdditionalProperties() = AdditionalProblemProperties(
        prefix = prefix,
        suffix = suffix,
        timeLimitMillis = ((timeLimitSeconds.toDoubleOrNull() ?: 0.0) * 1000).roundToInt(),
        memoryLimitMegabytes = memoryLimitMegabytes.toIntOrNull() ?: 0,
        statementFormat = statementFormat
    )

    useEffect(problem) {
        setPrefix("polybacs-")
        setSuffix("")
    }
    useEffect(problem, problemInfo) {
        if (problemInfo == null) {
            setTimeLimitSeconds("")
            setMemoryLimitMegabytes("")
        } else {
            setTimeLimitSeconds("${problemInfo.timeLimitMillis / 1000.0}")
            setMemoryLimitMegabytes("${problemInfo.memoryLimitMegabytes}")
        }
    }
    useEffect(problem, prefix, suffix) {
        setFinalProblemName(buildAdditionalProperties().buildFullName(problem.name))
    }
    useEffect(finalProblemName) {
        setNameAvailability(NameAvailability.LOADING)
        var cancelled = false
        mainScope.launch {
            val availability = try {
                Api.getNameAvailability(finalProblemName)
            } catch (e: Throwable) {
                e.printStackTrace()
                CHECK_FAILED
            }
            if (!cancelled)
                setNameAvailability(availability)
        }
        cleanup { cancelled = true }
    }

    h2("my-3 text-center") { +"Свойства задачи" }
    div("container gy-3") {
        fun draw(name: String, property: String, initialValue: String, setValue: StateSetter<String>? = null) {
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

        fun drawStatementFormat() {
            div("row align-items-center") {
                label("col col-form-label text-end") {
                    +"Тип условия:"
                    attrs["htmlFor"] = "problem-statement"
                }
                div("col") {
                    for (format in StatementFormat.values()) {
                        div("form-check") {
                            input(InputType.radio, classes = "form-check-input", name = "problem-statement") {
                                attrs {
                                    id = format.name
                                    defaultChecked = format == StatementFormat.PDF
                                    onChangeFunction = { setStatementFormat(format) }
                                }
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = format
                                +format.name
                            }
                        }
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
            drawStatementFormat()
            div("row mt-1") {
                div("col text-center") {
                    span("me-2") { +finalProblemName }
                    val classes = when (nameAvailability) {
                        NameAvailability.AVAILABLE -> "bg-success"
                        NameAvailability.TAKEN -> "bg-warning text-dark"
                        NameAvailability.LOADING -> "bg-secondary"
                        CHECK_FAILED -> "bg-danger"
                    }
                    span("badge $classes") { +nameAvailability.description }
                }
            }
        }
        if (problemInfo != null && problem.accessType.isSufficient && problem.latestPackage != null) {
            div("row my-3") {
                div("col") {
                    button(type = ButtonType.button, classes = "btn btn-secondary btn-lg w-100") {
                        +"Скачать пакет"
                        attrs {
                            onClickFunction = {
                                mainScope.launch {
                                    Api.downloadPackage(problem, buildAdditionalProperties())
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
                                mainScope.launch {
                                    Api.transferToBacsArchive(problem, buildAdditionalProperties())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
