package com.todo.infrastructure.persistence.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(unique = true)
    lateinit var email: String

    lateinit var name: String

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    var todos: MutableList<TodoEntity> = mutableListOf()
}