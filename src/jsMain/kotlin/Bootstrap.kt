@file:Suppress("ClassName")

import org.w3c.dom.Element

external object bootstrap {
    class Toast(target: Element, props: BootstrapToastProps) {
        fun show()
        fun hide()
        fun dispose()
    }
}

external interface BootstrapToastProps {
    var animation: Boolean
    var autohide: Boolean
    var delay: Int
}
