package api

import kotlinx.serialization.Serializable

@Serializable
data class Toast(
    val title: String,
    val content: String,
    val kind: ToastKind = ToastKind.INFORMATION
)

@Serializable
enum class ToastKind {
    INFORMATION,
    SUCCESS,
    FAILURE,
}