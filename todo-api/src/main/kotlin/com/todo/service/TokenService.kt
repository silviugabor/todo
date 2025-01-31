package com.todo.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.SIG.HS256
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import java.util.*

@Service
class TokenService {
    private val secretKey = HS256.key().build()

    fun generateToken(email: String, authorities: Collection<GrantedAuthority>): String {
        val now = Date()
        val validity = Date(now.time + 3600000)

        return Jwts.builder()
            .subject(email)
            .claim("roles", authorities.map { it.authority })
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getEmailFromToken(token: String): String {
        return getClaimsFromToken(token)
            .subject
    }
}