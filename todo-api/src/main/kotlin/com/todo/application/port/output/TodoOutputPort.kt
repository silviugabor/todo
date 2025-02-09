package com.todo.application.port.output

import com.todo.domain.model.Todo
import com.todo.domain.model.TodoId
import com.todo.domain.model.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TodoOutputPort {
    fun findByUserId(userId: UserId, pageable: Pageable): Page<Todo>
    fun findByIdAndUserId(todoId: TodoId, userId: UserId): Todo?
    fun save(todo: Todo): Todo
    fun delete(todo: Todo)
}