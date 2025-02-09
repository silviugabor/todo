package com.todo.application.adapter

import com.todo.application.port.input.TodoInputPort
import com.todo.application.port.output.TodoOutputPort
import com.todo.application.port.output.UserOutputPort
import com.todo.domain.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TodoServiceAdapter(
    private val todoOutputPort: TodoOutputPort,
    private val userOutputPort: UserOutputPort
) : TodoInputPort {

    @Transactional(readOnly = true)
    override fun getTodosForUser(userId: UserId, pageable: Pageable): Page<Todo> {
        return todoOutputPort.findByUserId(userId, pageable)
    }

    @Transactional
    override fun createTodo(userId: UserId, title: Title, description: Description): Todo {
        userOutputPort.findById(userId) ?: throw NoSuchElementException("User not found")

        val todo = Todo(
            userId = userId,
            title = title,
            description = description
        )

        return todoOutputPort.save(todo)
    }

    @Transactional
    override fun updateTodo(todoId: TodoId, userId: UserId, title: Title, description: Description): Todo {
        val todo = todoOutputPort.findByIdAndUserId(todoId, userId)
            ?: throw NoSuchElementException("Todo not found")

        val updatedTodo = todo.update(title, description)
        return todoOutputPort.save(updatedTodo)
    }

    @Transactional
    override fun deleteTodo(todoId: TodoId, userId: UserId) {
        val todo = todoOutputPort.findByIdAndUserId(todoId, userId)
            ?: throw NoSuchElementException("Todo not found")

        todoOutputPort.delete(todo)
    }
}