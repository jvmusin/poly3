import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.em
import kotlinx.css.fontSize
import kotlinx.css.maxHeight
import kotlinx.css.px
import react.child
import react.dom.render
import styled.injectGlobal

fun main() {
    injectGlobal {
        ".problem-list ul" {
            maxHeight = 800.px
        }
        ".problem-details > .container" {
            fontSize = 1.3.em
        }
    }

    window.onload = {
        render(document.getElementById("root")) {
            child(upd.App)
        }
    }
}
