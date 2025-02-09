package com.todo.application.port.input

import com.todo.domain.model.User

interface UserInputPort {
    fun getUserByEmail(email: String): User?
}