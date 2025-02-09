package com.todo.domain.model

@JvmInline
value class UserId private constructor(val value: Long) {
    companion object {
        fun of(value: Long): UserId = UserId(value)
        fun new(): UserId = UserId(0)
    }
}
