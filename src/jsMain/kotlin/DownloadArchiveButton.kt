import kotlinx.css.Color
import kotlinx.css.color
import kotlinx.css.fontSize
import kotlinx.css.px
import react.RProps
import react.functionalComponent
import styled.css
import styled.styledA

external interface DownloadArchiveButtonProps : RProps {
    var problemId: Int
}

val DownloadArchiveButton = functionalComponent<DownloadArchiveButtonProps> { props ->
    styledA {
        css {
            fontSize = 30.px
            color = Color.hotPink
        }
        +"Скачать архив"
        attrs {
            href = downloadPackageLink(props.problemId)
        }
    }
}