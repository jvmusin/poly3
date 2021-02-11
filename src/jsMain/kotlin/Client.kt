import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.*
import react.child
import react.dom.render
import styled.injectGlobal

fun main() {
    injectGlobal {
        ".problem-list ul" {
            maxHeight = 700.px
            overflowY = Overflow.auto
            overflowX = Overflow.hidden
        }
        ".problem-details > .container" {
            fontSize = 1.3.em
        }
        ".problem-details input" {
            fontSize = 0.9.em
        }
        ".problem-list button.disabled" {
            color = Color("#842029")
            backgroundColor = Color("#f8d7da")
        }
        "#notifications" {
            position = Position.fixed
            bottom = 15.px
            right = 15.px
        }
    }

    window.onload = {
        render(document.getElementById("root")) {
            child(upd.App)
        }
    }
}
