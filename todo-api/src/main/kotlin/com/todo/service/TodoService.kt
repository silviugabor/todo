package com.todo.service

import com.todo.model.dto.TodoRequest
import com.todo.model.jpa.Todo
import com.todo.model.jpa.User
import com.todo.repository.TodoRepository
import com.todo.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class TodoService(
    private val todoRepository: TodoRepository,
    private val userRepository: UserRepository
) {
    fun getTodosForUser(username: String?, pageRequest: PageRequest): Page<Todo> {
        val user = getUser(username)
        return todoRepository.findByUser(user, pageRequest)
    }

    @Transactional
    fun createTodo(request: TodoRequest, username: String?): Todo {
        val user = getUser(username)

        val todo = Todo(
            title = request.title,
            description = request.description,
            user = user
        )

        return todoRepository.save(todo)
    }

    @Transactional
    fun updateTodo(id: Long, request: TodoRequest, username: String?): Todo {
        val user = getUser(username)
        val todo = getTodoForUser(id, user)

        todo.title = request.title
        todo.description = request.description

        return todoRepository.save(todo)
    }

    @Transactional
    fun deleteTodo(id: Long, username: String?) {
        val user = getUser(username)
        val todo = getTodoForUser(id, user)
        todoRepository.delete(todo)
    }

    private fun getUser(username: String?): User {
        if (username == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated")
        }

        return userRepository.findByEmail(username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
    }

    private fun getTodoForUser(id: Long, user: User): Todo {
        return todoRepository.findByIdAndUser(id, user)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found")
    }
}