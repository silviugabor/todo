package com.todo.infrastructure.persistence.mapper

import com.todo.domain.model.*
import com.todo.infrastructure.persistence.entity.TodoEntity
import com.todo.infrastructure.persistence.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class PersistenceMapper {
    fun toDomain(entity: TodoEntity): Todo {
        return Todo(
            id = TodoId.of(entity.id),
            title = Title(entity.title),
            description = Description(entity.description),
            userId = UserId.of(entity.user.id)
        )
    }

    fun toEntity(domain: Todo, userEntity: UserEntity? = null): TodoEntity {
        return TodoEntity().apply {
            id = if (domain.id == TodoId.new()) 0 else domain.id.value
            title = domain.title.value
            description = domain.description.value
            user = userEntity ?: UserEntity().apply { id = domain.userId.value }
        }
    }

    fun toDomain(entity: UserEntity): User {
        return User(
            id = UserId.of(entity.id),
            email = Email(entity.email),
            name = entity.name,
            todos = entity.todos.map { toDomain(it) }.toMutableList()
        )
    }

    fun toEntity(domain: User): UserEntity {
        return UserEntity().apply {
            id = if (domain.id == UserId.new()) 0 else domain.id.value
            email = domain.email.value
            name = domain.name
        }
    }
}