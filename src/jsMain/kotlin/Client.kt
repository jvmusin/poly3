import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.*
import react.child
import react.dom.render
import styled.injectGlobal

fun main() {
    injectGlobal {
        body {
            margin(0.px)
        }
        "#root" {
            height = 100.vh
            display = Display.flex
            flexDirection = FlexDirection.column
        }
        "h1, h2" {
            marginBottom = 0.px
        }
    }

    window.onload = {
        render(document.getElementById("root")) {
            child(App)
        }
    }
}
