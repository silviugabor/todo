package com.todo.infrastructure.persistence.adapter

import com.todo.application.port.output.TodoOutputPort
import com.todo.domain.model.Todo
import com.todo.domain.model.TodoId
import com.todo.domain.model.UserId
import com.todo.infrastructure.persistence.mapper.PersistenceMapper
import com.todo.infrastructure.persistence.repository.TodoRepository
import com.todo.infrastructure.persistence.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class TodoPersistenceAdapter(
    private val todoRepository: TodoRepository,
    private val userRepository: UserRepository,
    private val mapper: PersistenceMapper
) : TodoOutputPort {

    override fun findByUserId(userId: UserId, pageable: Pageable): Page<Todo> {
        return todoRepository.findByUserId(userId.value, pageable)
            .map { mapper.toDomain(it) }
    }

    override fun findByIdAndUserId(todoId: TodoId, userId: UserId): Todo? {
        return todoRepository.findByIdAndUserId(todoId.value, userId.value)
            ?.let { mapper.toDomain(it) }
    }

    @Transactional
    override fun save(todo: Todo): Todo {
        val userEntity = if (todo.id == TodoId.new()) {
            userRepository.getReferenceById(todo.userId.value)
        } else {
            null
        }

        val entity = mapper.toEntity(todo, userEntity)
        return mapper.toDomain(todoRepository.save(entity))
    }

    @Transactional
    override fun delete(todo: Todo) {
        todoRepository.deleteById(todo.id.value)
    }
}