import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.*
import react.child
import react.dom.render
import styled.injectGlobal

fun main() {
    injectGlobal {
        ".problem-list ul" {
            maxHeight = 800.px
            overflowY = Overflow.auto
            overflowX = Overflow.hidden
        }
        ".problem-details > .container" {
            fontSize = 1.3.em
        }
        ".problem-details input" {
            fontSize = 0.9.em
        }
    }

    window.onload = {
        render(document.getElementById("root")) {
            child(upd.App)
        }
    }
}
