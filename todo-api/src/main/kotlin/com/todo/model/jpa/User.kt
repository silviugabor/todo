package com.todo.model.jpa

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true)
    lateinit var email: String

    lateinit var name: String

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    var todos: MutableList<Todo> = mutableListOf()
}