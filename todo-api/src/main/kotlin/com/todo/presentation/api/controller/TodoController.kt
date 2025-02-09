package com.todo.presentation.api.controller;

import com.todo.presentation.adapter.TodoPresentationAdapter
import com.todo.presentation.api.dto.request.TodoRequest
import com.todo.presentation.api.dto.response.TodoResponse
import org.springframework.data.domain.PageRequest
import org.springframework.hateoas.IanaLinkRelations
import org.springframework.hateoas.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/todos")
class TodoController(
    private val todoAdapter: TodoPresentationAdapter
) {
    @GetMapping
    fun getTodos(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        principal: Principal
    ): PagedModel<TodoResponse> {
        return todoAdapter.getTodos(
            userEmail = principal.name,
            pageable = PageRequest.of(page, size)
        )
    }

    @PostMapping
    fun createTodo(
        @RequestBody request: TodoRequest,
        principal: Principal
    ): ResponseEntity<TodoResponse> {
        val response = todoAdapter.createTodo(
            userEmail = principal.name,
            request = request
        )
        return ResponseEntity
            .created(response.getRequiredLink(IanaLinkRelations.SELF).toUri())
            .body(response)
    }

    @PutMapping("/{id}")
    fun updateTodo(
        @PathVariable id: Long,
        @RequestBody request: TodoRequest,
        principal: Principal
    ): ResponseEntity<TodoResponse> {
        val response = todoAdapter.updateTodo(
            userEmail = principal.name,
            todoId = id,
            request = request
        )
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun deleteTodo(
        @PathVariable id: Long,
        principal: Principal
    ): ResponseEntity<Void> {
        todoAdapter.deleteTodo(
            userEmail = principal.name,
            todoId = id
        )
        return ResponseEntity.noContent().build()
    }

}