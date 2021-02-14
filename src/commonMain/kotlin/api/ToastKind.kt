package api

import kotlinx.serialization.Serializable

@Serializable
enum class ToastKind {
    INFORMATION,
    SUCCESS,
    FAILURE
}