package com.todo.domain.model

@JvmInline
value class TodoId private constructor(val value: Long) {
    companion object {
        fun of(value: Long): TodoId = TodoId(value)
        fun new(): TodoId = TodoId(0)
    }
}