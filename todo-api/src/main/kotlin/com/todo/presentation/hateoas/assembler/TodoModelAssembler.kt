package com.todo.presentation.hateoas.assembler

import com.todo.domain.model.Todo
import com.todo.presentation.api.controller.TodoController
import com.todo.presentation.api.dto.request.TodoRequest
import com.todo.presentation.api.dto.response.TodoResponse
import org.springframework.data.domain.Page
import org.springframework.hateoas.IanaLinkRelations
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import java.security.Principal

@Component
class TodoModelAssembler : RepresentationModelAssemblerSupport<Todo, TodoResponse>(
    TodoController::class.java,
    TodoResponse::class.java
) {
    private val dummyPrincipal: Principal = UsernamePasswordAuthenticationToken("user", "password")
    private val dummyRequest = TodoRequest("dummy", "dummy")

    override fun toModel(entity: Todo): TodoResponse {
        return createModelWithId(entity.id.value, entity).apply {
            add(
                linkTo(methodOn(TodoController::class.java).getTodos(0, 10, dummyPrincipal))
                    .withRel(IanaLinkRelations.COLLECTION)
            )

            add(
                linkTo(methodOn(TodoController::class.java).updateTodo(entity.id.value, dummyRequest, dummyPrincipal))
                    .withRel("update")
            )

            add(
                linkTo(methodOn(TodoController::class.java).deleteTodo(entity.id.value, dummyPrincipal))
                    .withRel("delete")
            )
        }
    }

    fun toPagedModel(todos: Page<Todo>): PagedModel<TodoResponse> {
        val todoResponses = todos.map { toModel(it) }

        val metadata = PagedModel.PageMetadata(
            todos.size.toLong(),
            todos.number.toLong(),
            todos.totalElements,
            todos.totalPages.toLong()
        )

        return PagedModel.of(
            todoResponses.content,
            metadata,
            linkTo(methodOn(TodoController::class.java).getTodos(todos.number, todos.size, dummyPrincipal))
                .withSelfRel(),
            linkTo(methodOn(TodoController::class.java).getTodos(0, todos.size, dummyPrincipal))
                .withRel(IanaLinkRelations.FIRST),
            linkTo(methodOn(TodoController::class.java).getTodos(todos.totalPages - 1, todos.size, dummyPrincipal))
                .withRel(IanaLinkRelations.LAST)
        ).apply {
            if (todos.hasNext()) {
                add(
                    linkTo(methodOn(TodoController::class.java).getTodos(todos.number + 1, todos.size, dummyPrincipal))
                        .withRel(IanaLinkRelations.NEXT)
                )
            }
            if (todos.hasPrevious()) {
                add(
                    linkTo(methodOn(TodoController::class.java).getTodos(todos.number - 1, todos.size, dummyPrincipal))
                        .withRel(IanaLinkRelations.PREV)
                )
            }
        }
    }
}