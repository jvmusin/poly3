import api.Problem
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.css.Color
import kotlinx.css.color
import kotlinx.css.fontSize
import kotlinx.css.px
import kotlinx.html.js.onClickFunction
import react.RProps
import react.functionalComponent
import styled.css
import styled.styledButton

external interface TransferProblemButtonProps: RProps {
    var problem: Problem
}

val TransferProblemButton = functionalComponent<TransferProblemButtonProps> { props ->
    val problem = props.problem

    styledButton {
        css {
            fontSize = 30.px
            color = Color.deepPink
        }
        +"Загрузить в BACS с id polybacs-${problem.name}"
        attrs {
            onClickFunction = {
                fun logAlert(text: String) {
                    console.log(text)
                    window.alert(text)
                }
                scope.launch {
                    var fail = false
                    try {
                        val sybonId = transferToBacsArchive(problem.id)
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