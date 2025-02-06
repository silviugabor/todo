package com.todo.idp

import com.todo.idp.model.User
import com.todo.idp.repository.UserRepository
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensaml.core.config.InitializationService
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.Issuer
import org.opensaml.security.credential.Credential
import org.opensaml.security.x509.BasicX509Credential
import org.opensaml.xmlsec.signature.KeyInfo
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.X509Certificate
import org.opensaml.xmlsec.signature.X509Data
import org.opensaml.xmlsec.signature.support.SignatureConstants
import org.opensaml.xmlsec.signature.support.Signer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@SpringBootTest(classes = [IdpApplication::class])
@AutoConfigureMockMvc
class IdpIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Value("\${saml2.certificate}")
    private lateinit var spCertificateContent: String

    @Value("\${saml2.private-key}")
    private lateinit var spPrivateKeyContent: String

    private val testEmail = "test@example.com"
    private val testPassword = "password123"

    @BeforeEach
    fun setup() {
        InitializationService.initialize()
        userRepository.deleteAll()
        createTestUser()
    }

    private fun createTestUser() {
        val user = User().apply {
            email = testEmail
            password = passwordEncoder.encode(testPassword)
            name = "Test User"
            roles = mutableSetOf("USER")
        }
        userRepository.save(user)
    }

    @Test
    fun testSAMLMetadataEndpoint() {
        mockMvc.perform(get("/saml/metadata"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(containsString("EntityDescriptor")))
            .andExpect(content().string(containsString("IDPSSODescriptor")))
            .andExpect(content().string(containsString("SingleSignOnService")))
    }

    @Test
    fun testSAMLLoginForm() {
        val samlRequest = createSamlRequest()
        val encodedRequest = Base64.getEncoder().encodeToString(samlRequest.toByteArray())

        mockMvc.perform(
            post("/saml/login")
                .param("SAMLRequest", encodedRequest)
                .param("RelayState", "someState")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("login"))
            .andExpect(model().attributeExists("samlRequest"))
            .andExpect(model().attributeExists("relayState"))
    }

    @Test
    fun testSuccessfulSAMLAuthentication() {
        val samlRequest = createSamlRequest()
        val encodedRequest = Base64.getEncoder().encodeToString(samlRequest.toByteArray())

        val result = mockMvc.perform(
            post("/saml/sso")
                .param("SAMLRequest", encodedRequest)
                .param("username", testEmail)
                .param("password", testPassword)
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("SAMLResponse")))
            .andExpect(content().string(containsString("form")))
            .andReturn()

        val response = result.response.contentAsString
        assertTrue(response.contains("<form method=\"post\""))
        assertTrue(response.contains("name=\"SAMLResponse\""))
        assertTrue(response.contains("document.forms[0].submit()"))
    }

    @Test
    fun testFailedSAMLAuthenticationWithInvalidCredentials() {
        val samlRequest = createSamlRequest()
        val encodedRequest = Base64.getEncoder().encodeToString(samlRequest.toByteArray())

        mockMvc.perform(
            post("/saml/sso")
                .param("SAMLRequest", encodedRequest)
                .param("username", testEmail)
                .param("password", "wrongpassword")
        ).andExpect(status().isUnauthorized)
    }

    private fun createSamlRequest(): String {
        val authnRequest = createSAMLObject(AuthnRequest.DEFAULT_ELEMENT_NAME) as AuthnRequest
        authnRequest.apply {
            id = "_" + UUID.randomUUID().toString()
            issueInstant = Instant.now()
            destination = "http://localhost:8081/saml/sso"
            assertionConsumerServiceURL = "http://localhost:8080/login/saml2/sso"
            protocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
            issuer = createIssuer()
        }

        val signature = createSAMLObject(Signature.DEFAULT_ELEMENT_NAME) as Signature
        signature.signingCredential = createSigningCredential()
        signature.signatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256
        signature.canonicalizationAlgorithm = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS

        val keyInfo = createSAMLObject(KeyInfo.DEFAULT_ELEMENT_NAME) as KeyInfo
        val x509Data = createSAMLObject(X509Data.DEFAULT_ELEMENT_NAME) as X509Data
        val x509Certificate = createSAMLObject(X509Certificate.DEFAULT_ELEMENT_NAME) as X509Certificate
        x509Certificate.value = Base64.getEncoder().encodeToString(loadCertificate().encoded)
        x509Data.x509Certificates.add(x509Certificate)
        keyInfo.x509Datas.add(x509Data)
        signature.keyInfo = keyInfo

        authnRequest.signature = signature

        val marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory()
        val marshaller = marshallerFactory.getMarshaller(authnRequest)!!
        marshaller.marshall(authnRequest)
        Signer.signObject(signature)

        return marshalToString(authnRequest)
    }

    private fun createSigningCredential(): Credential {
        val certificate = loadCertificate()
        val privateKey = loadPrivateKey()
        return BasicX509Credential(certificate, privateKey)
    }

    private fun loadCertificate(): java.security.cert.X509Certificate {
        val certContent = spCertificateContent.trimIndent()

        val certBytes = Base64.getDecoder().decode(
            certContent
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "")
        )

        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as java.security.cert.X509Certificate
    }

    private fun loadPrivateKey(): PrivateKey {
        val privateKeyContent = spPrivateKeyContent.trimIndent()

        val keyBytes = Base64.getDecoder().decode(
            privateKeyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
        )

        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return keyFactory.generatePrivate(keySpec)
    }

    private fun createIssuer(): Issuer {
        return (createSAMLObject(Issuer.DEFAULT_ELEMENT_NAME) as Issuer).apply {
            value = "http://localhost:8080"
        }
    }

    private fun createSAMLObject(qname: javax.xml.namespace.QName): org.opensaml.core.xml.XMLObject {
        return XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname)!!.buildObject(qname)
    }

    private fun marshalToString(samlObject: org.opensaml.core.xml.XMLObject): String {
        val marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory()
        val marshaller = marshallerFactory.getMarshaller(samlObject)!!
        val element = marshaller.marshall(samlObject)

        val writer = StringWriter()
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.transform(DOMSource(element), StreamResult(writer))

        return writer.toString()
    }
}