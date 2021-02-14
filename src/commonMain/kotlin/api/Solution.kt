package api

import kotlinx.serialization.Serializable

@Serializable
data class Solution(
    val name: String,
    val language: String,
    val expectedVerdict: String
)