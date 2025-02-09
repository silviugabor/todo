package com.todo.domain.model

@JvmInline
value class Title(val value: String) {
    init {
        require(value.isNotBlank()) { "Title cannot be blank" }
        require(value.length <= 100) { "Title cannot exceed 100 characters" }
    }
}