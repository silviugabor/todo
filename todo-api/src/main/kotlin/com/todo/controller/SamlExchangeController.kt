package com.todo.controller

import com.todo.service.TokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class SamlExchangeController(
    private val tokenService: TokenService
) {

    @PostMapping("/saml/login")
    fun handleSamlLogin(authentication: Authentication): ResponseEntity<Map<String, String>> {
        val email =
            (authentication.principal as Saml2AuthenticatedPrincipal).getAttribute<String>("email")?.firstOrNull()
                ?: throw IllegalStateException("No email attribute found in SAML response")

        val authorities = authentication.authorities as Collection<GrantedAuthority>
        val token = tokenService.generateToken(email, authorities)

        return ResponseEntity.ok(mapOf("token" to token))
    }
}