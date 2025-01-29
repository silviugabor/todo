package com.todo.idp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IdpApplication

fun main(args: Array<String>) {
    runApplication<IdpApplication>(*args)
}