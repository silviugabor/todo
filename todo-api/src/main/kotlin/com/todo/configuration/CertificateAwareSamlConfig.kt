package com.todo.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.provider.service.registration.*
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration.AssertingPartyDetails
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

@Configuration
class CertificateAwareSamlConfig {

    @Value("\${saml2.private-key}")
    private lateinit var privateKeyContent: String

    @Value("\${saml2.certificate}")
    private lateinit var certificateContent: String

    @Value("\${saml2.idp-metadata-url}")
    private lateinit var idpMetadataUrl: String

    @Bean
    fun relyingPartyRegistrationRepository(): RelyingPartyRegistrationRepository {
        val registration = RelyingPartyRegistrations
            .fromMetadataLocation(idpMetadataUrl)
            .assertingPartyDetails { party ->
                party
                    .entityId("http://localhost:8081/saml/metadata")
                    .singleSignOnServiceLocation("http://localhost:8081/saml/login")
                    .wantAuthnRequestsSigned(false)
                    .verificationX509Credentials { c ->
                        c.add(loadIdpVerificationCredential())
                    }
            }
            .registrationId("default")
            .signingX509Credentials { c -> c.add(loadSigningCredential()) }
            .build()

        return InMemoryRelyingPartyRegistrationRepository(registration)
    }

    private fun loadSigningCredential(): Saml2X509Credential {
        val privateKey = loadPrivateKey()
        val certificate = loadCertificate()

        return Saml2X509Credential(privateKey, certificate, Saml2X509Credential.Saml2X509CredentialType.SIGNING)
    }

    private fun loadDecryptionCredential(): Collection<Saml2X509Credential> {
        val privateKey = loadPrivateKey()
        val certificate = loadCertificate()

        return listOf(
            Saml2X509Credential(
                privateKey,
                certificate,
                Saml2X509Credential.Saml2X509CredentialType.DECRYPTION
            )
        )
    }

    private fun loadIdpVerificationCredential(): Saml2X509Credential {
        val certificate = loadCertificate()
        return Saml2X509Credential(certificate, Saml2X509Credential.Saml2X509CredentialType.VERIFICATION)
    }

    private fun loadSignatureVerificationCredential(): Saml2X509Credential {
        return Saml2X509Credential(
            loadCertificate(),
            Saml2X509Credential.Saml2X509CredentialType.ENCRYPTION
        )
    }

    private fun loadPrivateKey(): RSAPrivateKey {
        val pkcs8Lines = privateKeyContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\\s".toRegex(), "")


        val keyBytes = Base64.getDecoder().decode(pkcs8Lines)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")

        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun loadCertificate(): X509Certificate {
        val certLines = certificateContent
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\\s".toRegex(), "")

        val certBytes = Base64.getDecoder().decode(certLines)
        val certFactory = CertificateFactory.getInstance("X.509")

        return certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }
}