package com.todo.service

import com.todo.model.jpa.Todo
import com.todo.model.jpa.User
import com.todo.repository.TodoRepository
import com.todo.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val todoRepository: TodoRepository
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments?) {
        if (userRepository.count() == 0L) {
            val todo = Todo()
            todo.title = "TODO"
            todo.description = "Description"

            val user = User()
            user.name = "Test User"
            user.email = "test@example.com"
            userRepository.save(user)

            user.todos.add(todo)
            todo.user = user
            todoRepository.save(todo)
        }
    }
}