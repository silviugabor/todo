package com.todo.repository

import com.todo.model.jpa.Todo
import com.todo.model.jpa.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {
    fun findByUser(user: User, pageable: Pageable): Page<Todo>
    fun findByIdAndUser(id: Long, user: User): Todo?
}