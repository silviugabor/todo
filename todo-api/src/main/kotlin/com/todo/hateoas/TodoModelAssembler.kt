package com.todo.hateoas

import com.todo.controller.TodoController
import com.todo.model.dto.TodoResponse
import com.todo.model.jpa.Todo
import org.springframework.data.domain.Page
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.stereotype.Component

@Component
class TodoModelAssembler : RepresentationModelAssembler<Todo, TodoResponse> {

    override fun toModel(entity: Todo): TodoResponse {
        val response = TodoResponse(
            id = entity.id!!,
            title = entity.title,
            description = entity.description
        )

        response.add(
            linkTo(TodoController::class.java).slash(entity.id).withSelfRel(),
            linkTo(TodoController::class.java).withRel("todos")
        )

        return response
    }

    fun toPagedModel(todos: Page<Todo>): PagedModel<TodoResponse> {
        val todoResponses = todos.map { toModel(it) }

        return PagedModel.of(
            todoResponses.content,
            PagedModel.PageMetadata(
                todos.size.toLong(),
                todos.number.toLong(),
                todos.totalElements,
                todos.totalPages.toLong()
            ),
            linkTo(TodoController::class.java).withSelfRel()
        )
    }
}