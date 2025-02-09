package com.todo.presentation.api.dto.response

data class TokenResponse(
    val token: String,
    val email: String,
    val attributes: Map<String, List<Any>>
)