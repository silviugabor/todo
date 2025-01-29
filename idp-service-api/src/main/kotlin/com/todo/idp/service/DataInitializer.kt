package com.todo.idp.service

import com.todo.idp.model.User
import com.todo.idp.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (userRepository.count() == 0L) {
            val user = User()
            user.name = "Test User"
            user.email = "test@example.com"
            user.password = passwordEncoder.encode("password")
            user.roles = mutableSetOf("USER", "ADMIN")
            userRepository.save(user)
        }
    }
}