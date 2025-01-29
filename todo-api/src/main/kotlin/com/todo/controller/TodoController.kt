package com.todo.controller;

import com.todo.hateoas.TodoModelAssembler
import com.todo.model.dto.TodoRequest
import com.todo.model.dto.TodoResponse;
import com.todo.service.TodoService
import org.springframework.data.domain.PageRequest
import org.springframework.hateoas.IanaLinkRelations
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import java.security.Principal;

@RestController
@RequestMapping("/api/todos")
class TodoController(
    private val todoService: TodoService,
    private val assembler: TodoModelAssembler
) {

    @GetMapping
    fun getTodos(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        principal: Principal
    ): PagedModel<TodoResponse> {
        val todos = todoService.getTodosForUser(principal.name, PageRequest.of(page, size))
        return assembler.toPagedModel(todos)
    }

    @PostMapping
    fun createTodo(
        @RequestBody request: TodoRequest,
        principal: Principal
    ): ResponseEntity<TodoResponse> {
        val todo = todoService.createTodo(request, principal.name)
        val response = assembler.toModel(todo)
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
        val todo = todoService.updateTodo(id, request, principal.name)
        return ResponseEntity.ok(assembler.toModel(todo))
    }

    @DeleteMapping("/{id}")
    fun deleteTodo(
        @PathVariable id: Long,
        principal: Principal
    ): ResponseEntity<Void> {
        todoService.deleteTodo(id, principal.name)
        return ResponseEntity.noContent().build()
    }
}