package com.todo.model.dto

import org.springframework.hateoas.RepresentationModel

data class TodoResponse(
    val id: Long,
    val title: String,
    val description: String
) : RepresentationModel<TodoResponse>()