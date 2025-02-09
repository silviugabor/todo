package com.todo.infrastructure.persistence.repository

import com.todo.infrastructure.persistence.entity.TodoEntity
import com.todo.infrastructure.persistence.entity.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<TodoEntity, Long> {
    fun findByUser(user: UserEntity, pageable: Pageable): Page<TodoEntity>
    fun findByIdAndUser(id: Long, user: UserEntity): TodoEntity?

    fun findByUserId(userId: Long, pageable: Pageable): Page<TodoEntity>
    fun findByIdAndUserId(id: Long, userId: Long): TodoEntity?
}