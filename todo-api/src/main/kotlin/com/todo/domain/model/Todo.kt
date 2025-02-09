package com.todo.domain.model

data class Todo(
    val id: TodoId = TodoId.new(),
    val title: Title,
    val description: Description,
    val userId: UserId
) {
    fun update(title: Title, description: Description): Todo =
        copy(title = title, description = description)
}