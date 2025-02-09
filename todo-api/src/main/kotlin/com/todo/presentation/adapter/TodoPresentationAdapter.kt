package com.todo.presentation.adapter

import com.todo.application.port.input.TodoInputPort
import com.todo.application.port.input.UserInputPort
import com.todo.domain.exceptions.UserNotFoundException
import com.todo.domain.model.Description
import com.todo.domain.model.Title
import com.todo.domain.model.TodoId
import com.todo.domain.model.UserId
import com.todo.presentation.api.dto.request.TodoRequest
import com.todo.presentation.api.dto.response.TodoResponse
import com.todo.presentation.hateoas.assembler.TodoModelAssembler
import org.springframework.data.domain.Pageable
import org.springframework.hateoas.PagedModel
import org.springframework.stereotype.Component

@Component
class TodoPresentationAdapter(
    private val todoInputPort: TodoInputPort,
    private val todoAssembler: TodoModelAssembler,
    private val userInputPort: UserInputPort
) {
    fun getTodos(userEmail: String, pageable: Pageable): PagedModel<TodoResponse> {
        val userId = resolveUserId(userEmail)
        val todos = todoInputPort.getTodosForUser(userId, pageable)
        return todoAssembler.toPagedModel(todos)
    }

    fun createTodo(userEmail: String, request: TodoRequest): TodoResponse {
        validateTodoRequest(request)

        val userId = resolveUserId(userEmail)
        val todo = todoInputPort.createTodo(
            userId = userId,
            title = Title(request.title),
            description = Description(request.description)
        )
        return todoAssembler.toModel(todo)
    }

    fun updateTodo(userEmail: String, todoId: Long, request: TodoRequest): TodoResponse {
        validateTodoRequest(request)

        val userId = resolveUserId(userEmail)
        val todo = todoInputPort.updateTodo(
            todoId = TodoId.of(todoId),
            userId = userId,
            title = Title(request.title),
            description = Description(request.description)
        )
        return todoAssembler.toModel(todo)
    }

    fun deleteTodo(userEmail: String, todoId: Long) {
        val userId = resolveUserId(userEmail)
        todoInputPort.deleteTodo(
            todoId = TodoId.of(todoId),
            userId = userId
        )
    }

    private fun resolveUserId(email: String): UserId {
        return userInputPort.getUserByEmail(email)?.id
            ?: throw UserNotFoundException("User not found for email: $email")
    }

    private fun validateTodoRequest(request: TodoRequest) {
        require(request.title.isNotBlank()) { "Title cannot be blank" }
        require(request.title.length <= 100) { "Title cannot exceed 100 characters" }
        require(request.description.length <= 1000) { "Description cannot exceed 1000 characters" }
    }
}