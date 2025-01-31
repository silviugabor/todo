package com.todo.configuration

import com.todo.controller.SamlSuccessHandler
import com.todo.service.TokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher

@Configuration
@EnableWebSecurity
@Profile("!local")
class SecurityConfig(
    private val relyingPartyRegistrationRepository: RelyingPartyRegistrationRepository,
    private val samlSuccessHandler: SamlSuccessHandler,
    private val tokenService: TokenService
) {

    @Bean
    fun apiAuthFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(AntPathRequestMatcher("/api/**"))
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated()
            }
            .csrf { it.disable() }
            .cors { it.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .requestCache { it.disable() }
            .saml2Login { it.disable() }
            .saml2Logout { it.disable() }
            .addFilterBefore(
                JwtAuthenticationFilter(tokenService),
                UsernamePasswordAuthenticationFilter::class.java
            )
        return http.build()
    }

    @Bean
    fun samlFilterChain(http: HttpSecurity): SecurityFilterChain {
        val registrationResolver = DefaultRelyingPartyRegistrationResolver { _ ->
            relyingPartyRegistrationRepository.findByRegistrationId("default")
        }

        val metadata = Saml2MetadataFilter(registrationResolver) { registration ->
            registration.assertingPartyDetails.entityId
        }

        http
            .securityMatcher(
                OrRequestMatcher(
                    AntPathRequestMatcher("/saml2/**"),
                    AntPathRequestMatcher("/login/saml2/**")
                )
            )
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .cors { cors -> cors.disable() }
            .csrf { csrf -> csrf.disable() }
            .formLogin { form -> form.disable() }
            .saml2Login { saml2 ->
                saml2.relyingPartyRegistrationRepository(relyingPartyRegistrationRepository)
                saml2.successHandler(samlSuccessHandler)
            }
            .addFilterBefore(metadata, Saml2WebSsoAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun saml2AuthenticationProvider(): OpenSaml4AuthenticationProvider {
        val provider = OpenSaml4AuthenticationProvider()
        provider.setResponseValidator { return@setResponseValidator Saml2ResponseValidatorResult.success() }
        provider.setAssertionValidator { return@setAssertionValidator Saml2ResponseValidatorResult.success() }
        return provider
    }
}