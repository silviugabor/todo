package com.todo.application.port.output

import com.todo.domain.model.Email
import com.todo.domain.model.User
import com.todo.domain.model.UserId

interface UserOutputPort {
    fun findByEmail(email: Email): User?
    fun findById(id: UserId): User?
    fun save(user: User): User
}