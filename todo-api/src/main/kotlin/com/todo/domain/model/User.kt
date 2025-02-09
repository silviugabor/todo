package com.todo.domain.model

data class User(
    val id: UserId = UserId.new(),
    val email: Email,
    val name: String,
    val todos: MutableList<Todo> = mutableListOf()
) {
    fun addTodo(todo: Todo) {
        require(todo.userId == id) { "Todo doesn't belong to this user" }
        todos.add(todo)
    }

    fun removeTodo(todoId: TodoId) {
        todos.removeIf { it.id == todoId }
    }
}