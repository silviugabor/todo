package com.todo.configuration

import com.todo.controller.SamlSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher

@Configuration
@EnableWebSecurity
@Profile("!local")
class SamlSecurityConfig(
    private val relyingPartyRegistrationRepository: RelyingPartyRegistrationRepository,
    private val samlSuccessHandler: SamlSuccessHandler
) {

    @Bean
    @Order(1) // High priority
    fun apiAuthFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(AntPathRequestMatcher("/api/**")) // Match /api/auth/**
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll() // Allow all requests without security checks
            }
            .csrf { it.disable() } // Disable CSRF for these endpoints
            .cors { it.disable() } // Disable CORS
            .sessionManagement { it.disable() } // Disable session management
            .formLogin { it.disable() } // Disable form-based login
            .httpBasic { it.disable() } // Disable HTTP Basic authentication
            .requestCache { it.disable() } // Disable request cache
            .exceptionHandling { it.disable() } // Disable exception handling
            .saml2Login { it.disable() }
            .saml2Logout { it.disable() }
        return http.build()
    }

    @Bean
    @Order(2) // Lower priority
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

//    @Bean
//    fun saml2AuthenticationProvider(): OpenSaml4AuthenticationProvider {
//        val provider = OpenSaml4AuthenticationProvider()
//        val defaultValidator = OpenSaml4AuthenticationProvider.createDefaultResponseValidator()
//
//        provider.setResponseValidator { context ->
//            return@setResponseValidator Saml2ResponseValidatorResult.success()
//        }
//        provider.setAssertionValidator { assertion -> return@setAssertionValidator Saml2ResponseValidatorResult.success() }
//        return provider
//    }
}