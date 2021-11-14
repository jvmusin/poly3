@file:Suppress(
    "INLINE_EXTERNAL_DECLARATION",
    "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "WRONG_BODY_OF_EXTERNAL_DECLARATION",
    "NOTHING_TO_INLINE"
)

import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import react.ComponentClass
import react.PropsWithChildren

@JsModule("react-tooltip")
@JsNonModule
external object ReactTooltip {
    val default: ComponentClass<TooltipProps>

    inline fun show(target: Element): Any = default.asDynamic().show(target)
    inline fun hide(target: Element): Any = default.asDynamic().hide(target)
    inline fun rebuild(): Any = default.asDynamic().rebuild()
}

external interface Offset {
    var top: Number?
        get() = definedExternally
        set(value) = definedExternally
    var right: Number?
        get() = definedExternally
        set(value) = definedExternally
    var left: Number?
        get() = definedExternally
        set(value) = definedExternally
    var bottom: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface `T$0` {
    var left: Number
    var top: Number
}

external interface TooltipProps : PropsWithChildren {
//        var children: Any?
//        get() = definedExternally
//        set(value) = definedExternally
    var uuid: String?
        get() = definedExternally
        set(value) = definedExternally
    var place: String? /* "top" | "right" | "bottom" | "left" */
        get() = definedExternally
        set(value) = definedExternally
    var type: String? /* "dark" | "success" | "warning" | "error" | "info" | "light" */
        get() = definedExternally
        set(value) = definedExternally
    var effect: String? /* "float" | "solid" */
        get() = definedExternally
        set(value) = definedExternally
    var offset: Offset?
        get() = definedExternally
        set(value) = definedExternally
    var multiline: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var border: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var textColor: String?
        get() = definedExternally
        set(value) = definedExternally
    var backgroundColor: String?
        get() = definedExternally
        set(value) = definedExternally
    var borderColor: String?
        get() = definedExternally
        set(value) = definedExternally
    var arrowColor: String?
        get() = definedExternally
        set(value) = definedExternally
    var insecure: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var `class`: String?
        get() = definedExternally
        set(value) = definedExternally
    var className: String?
        get() = definedExternally
        set(value) = definedExternally
    var id: String?
        get() = definedExternally
        set(value) = definedExternally
    var html: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var delayHide: Number?
        get() = definedExternally
        set(value) = definedExternally
    var delayUpdate: Number?
        get() = definedExternally
        set(value) = definedExternally
    var delayShow: Number?
        get() = definedExternally
        set(value) = definedExternally
    var event: String?
        get() = definedExternally
        set(value) = definedExternally
    var eventOff: String?
        get() = definedExternally
        set(value) = definedExternally
    var isCapture: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var globalEventOff: String?
        get() = definedExternally
        set(value) = definedExternally
    var getContent: dynamic /* GetContentFunc? | JsTuple<GetContentFunc, Number> */
        get() = definedExternally
        set(value) = definedExternally
    var afterShow: ((Any) -> Unit)?
        get() = definedExternally
        set(value) = definedExternally
    var afterHide: ((Any) -> Unit)?
        get() = definedExternally
        set(value) = definedExternally
    var overridePosition: ((position: `T$0`, currentEvent: Event, currentTarget: EventTarget, refNode: dynamic /* HTMLDivElement? | HTMLSpanElement? */, place: String? /* "top" | "right" | "bottom" | "left" */, desiredPlace: String? /* "top" | "right" | "bottom" | "left" */, effect: String? /* "float" | "solid" */, offset: Offset) -> `T$0`)?
        get() = definedExternally
        set(value) = definedExternally
    var disable: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var scrollHide: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var resizeHide: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var wrapper: String? /* "div" | "span" */
        get() = definedExternally
        set(value) = definedExternally
    var bodyMode: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var possibleCustomEvents: String?
        get() = definedExternally
        set(value) = definedExternally
    var possibleCustomEventsOff: String?
        get() = definedExternally
        set(value) = definedExternally
    var clickable: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}
