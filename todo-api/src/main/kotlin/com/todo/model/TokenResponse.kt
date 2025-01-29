package com.todo.model

data class TokenResponse(
    val token: String,
    val email: String,
    val attributes: Map<String, List<Any>>
)