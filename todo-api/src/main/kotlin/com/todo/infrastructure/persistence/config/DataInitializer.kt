package com.todo.infrastructure.persistence.config

import com.todo.infrastructure.persistence.entity.TodoEntity
import com.todo.infrastructure.persistence.entity.UserEntity
import com.todo.infrastructure.persistence.repository.TodoRepository
import com.todo.infrastructure.persistence.repository.UserRepository
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
            val todo = TodoEntity()
            todo.title = "TODO"
            todo.description = "Description"

            val user = UserEntity()
            user.name = "Test User"
            user.email = "test@example.com"
            userRepository.save(user)

            user.todos.add(todo)
            todo.user = user
            todoRepository.save(todo)
        }
    }
}