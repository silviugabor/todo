package com.todo.infrastructure.persistence.adapter

import com.todo.application.port.output.UserOutputPort
import com.todo.domain.model.Email
import com.todo.domain.model.User
import com.todo.domain.model.UserId
import com.todo.infrastructure.persistence.mapper.PersistenceMapper
import com.todo.infrastructure.persistence.repository.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class UserPersistenceAdapter(
    private val userRepository: UserRepository,
    private val mapper: PersistenceMapper
) : UserOutputPort {

    override fun findByEmail(email: Email): User? {
        return userRepository.findByEmail(email.value)
            ?.let { mapper.toDomain(it) }
    }

    @Transactional
    override fun save(user: User): User {
        val entity = mapper.toEntity(user)
        return mapper.toDomain(userRepository.save(entity))
    }

    override fun findById(id: UserId): User? {
        return userRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }
}