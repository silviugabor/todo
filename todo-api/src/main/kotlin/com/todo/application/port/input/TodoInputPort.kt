package com.todo.application.port.input

import com.todo.domain.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TodoInputPort {
    fun getTodosForUser(userId: UserId, pageable: Pageable): Page<Todo>
    fun createTodo(userId: UserId, title: Title, description: Description): Todo
    fun updateTodo(todoId: TodoId, userId: UserId, title: Title, description: Description): Todo
    fun deleteTodo(todoId: TodoId, userId: UserId)
}