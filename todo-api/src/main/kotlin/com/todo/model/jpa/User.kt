package com.todo.model.jpa

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true)
    val email: String,

    val name: String,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    val todos: MutableList<Todo> = mutableListOf()
)