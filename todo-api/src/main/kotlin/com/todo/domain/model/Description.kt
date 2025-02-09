package com.todo.domain.model

@JvmInline
value class Description(val value: String) {
    init {
        require(value.length <= 1000) { "Description cannot exceed 1000 characters" }
    }
}