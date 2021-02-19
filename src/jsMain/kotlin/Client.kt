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
        ".toast-container" {
            position = Position.fixed
            bottom = 15.px
            right = 15.px
            zIndex = 1
        }
        ".toast-info .toast-header" {
            backgroundColor = Color("#00AC6B")
        }
        ".toast-info .toast-body" {
            backgroundColor = Color("#35D699")
        }
        ".toast-success .toast-header" {
            backgroundColor = Color("#7CE700")
        }
        ".toast-success .toast-body" {
            backgroundColor = Color("#B5F36D")
        }
        ".toast-failure .toast-header" {
            backgroundColor = Color("#FF8C00")
        }
        ".toast-failure .toast-body" {
            backgroundColor = Color("#FFA940")
        }
        ".toast-header" {
            color = Color("#212529")
        }
        ".problem-solutions table" {
            fontSize = 0.9.em
        }
    }

    window.onload = {
        render(document.getElementById("root")) {
            child(App)
        }
    }
}
