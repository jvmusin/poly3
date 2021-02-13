package api

import kotlinx.serialization.Serializable

@Serializable
data class Toast(
    val title: String,
    val content: String
)