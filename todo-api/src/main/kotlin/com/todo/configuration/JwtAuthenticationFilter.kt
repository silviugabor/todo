package com.todo.configuration

import com.todo.service.TokenService
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilter(private val tokenService: TokenService) : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        if (!shouldProcessRequest(httpRequest)) {
            chain.doFilter(request, response)
        }

        try {
            val token = extractToken(httpRequest)
            if (token == null || !tokenService.validateToken(token)) {
                return
            }
            updateSecurityContext(token)
        } catch (e: Exception) {
            SecurityContextHolder.clearContext()
        }

        chain.doFilter(request, response)
    }

    private fun updateSecurityContext(token: String?) {
        val email = tokenService.getEmailFromToken(token!!)
        val claims = tokenService.getClaimsFromToken(token)
        val authorities = (claims["roles"] as List<*>)
            .map { SimpleGrantedAuthority(it.toString()) }

        val authentication = UsernamePasswordAuthenticationToken(
            email,
            null,
            authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }

    private fun shouldProcessRequest(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return path.startsWith("/api/") && !path.startsWith("/api/auth/")
    }
}