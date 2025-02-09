package com.todo.application.adapter

import com.todo.application.port.input.UserInputPort
import com.todo.application.port.output.UserOutputPort
import com.todo.domain.model.Email
import com.todo.domain.model.User
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserServiceAdapter(
    private val userOutputPort: UserOutputPort
) : UserInputPort {

    @Transactional(readOnly = true)
    override fun getUserByEmail(email: String): User? {
        return userOutputPort.findByEmail(Email(email))
    }

}