package com.todo.idp.service

import com.todo.idp.model.User
import com.todo.idp.repository.UserRepository
import net.shibboleth.utilities.java.support.component.ComponentInitializationException
import org.opensaml.core.config.InitializationService
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.schema.impl.XSAnyImpl
import org.opensaml.saml.saml2.core.*
import org.opensaml.saml.saml2.metadata.*
import org.opensaml.security.credential.Credential
import org.opensaml.security.credential.UsageType
import org.opensaml.security.x509.BasicX509Credential
import org.opensaml.xmlsec.signature.KeyInfo
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.X509Certificate
import org.opensaml.xmlsec.signature.X509Data
import org.opensaml.xmlsec.signature.support.SignatureConstants
import org.opensaml.xmlsec.signature.support.Signer
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Service
class SamlService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Value("\${saml2.certificate}")
    private lateinit var certificateContent: String

    @Value("\${saml2.private-key}")
    private lateinit var privateKeyContent: String

    @Value("\${saml2.entity-id}")
    private lateinit var entityId: String

    private lateinit var signingCredential: Credential

    init {
        try {
            InitializationService.initialize()
        } catch (e: ComponentInitializationException) {
            throw RuntimeException("Failed to initialize OpenSAML", e)
        }
    }

    private fun initializeCredential() {
        if (!::signingCredential.isInitialized) {
            val certificate = loadCertificate()
            val privateKey = loadPrivateKey()
            signingCredential = BasicX509Credential(certificate, privateKey)
        }
    }

    fun generateMetadata(): String {
        val entityDescriptor = createSAMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME) as EntityDescriptor
        entityDescriptor.entityID = "http://localhost:8081/saml/metadata"

        val idpDescriptor = createSAMLObject(IDPSSODescriptor.DEFAULT_ELEMENT_NAME) as IDPSSODescriptor
        (idpDescriptor as RoleDescriptor).addSupportedProtocol("urn:oasis:names:tc:SAML:2.0:protocol")

        val ssoService = createSAMLObject(SingleSignOnService.DEFAULT_ELEMENT_NAME) as SingleSignOnService
        ssoService.binding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
        ssoService.location = "http://localhost:8081/saml/sso"
        idpDescriptor.singleSignOnServices.add(ssoService)

        val signingKeyDescriptor = createKeyDescriptor(loadCertificate(), UsageType.SIGNING)
        val encryptionKeyDescriptor = createKeyDescriptor(loadCertificate(), UsageType.ENCRYPTION)

        (idpDescriptor as RoleDescriptor).keyDescriptors.add(signingKeyDescriptor)
        (idpDescriptor as RoleDescriptor).keyDescriptors.add(encryptionKeyDescriptor)

        entityDescriptor.roleDescriptors.add(idpDescriptor)

        return marshalToXml(entityDescriptor)
    }

    private fun createKeyDescriptor(certificate: java.security.cert.X509Certificate, usage: UsageType): KeyDescriptor {
        val keyDescriptor = createSAMLObject(KeyDescriptor.DEFAULT_ELEMENT_NAME) as KeyDescriptor

        val keyInfo = createSAMLObject(KeyInfo.DEFAULT_ELEMENT_NAME) as KeyInfo
        val x509Data = createSAMLObject(X509Data.DEFAULT_ELEMENT_NAME) as X509Data
        val x509Certificate = createSAMLObject(X509Certificate.DEFAULT_ELEMENT_NAME) as X509Certificate

        x509Certificate.value = Base64.getEncoder().encodeToString(certificate.encoded)
        x509Data.x509Certificates.add(x509Certificate)
        keyInfo.x509Datas.add(x509Data)

        keyDescriptor.keyInfo = keyInfo
        keyDescriptor.use = usage

        return keyDescriptor
    }

    private fun loadCertificate(): java.security.cert.X509Certificate {
        val certLines = certificateContent
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\\s".toRegex(), "")

        val certBytes = Base64.getDecoder().decode(certLines)
        val certFactory = CertificateFactory.getInstance("X.509")

        return certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as java.security.cert.X509Certificate
    }

    fun authenticateAndGenerateSamlResponse(email: String, password: String, requestId: String): String {
        val user = userRepository.findByEmail(email) ?: throw UsernameNotFoundException("User not found")

        if (!passwordEncoder.matches(password, user.password)) {
            throw BadCredentialsException("Invalid password")
        }

        initializeCredential()

        val assertion = createAssertion(user, requestId, "http://localhost:8080")
        //signAssertion(assertion)

        val response = createSuccessResponse(assertion, requestId, "http://localhost:8080")
        signResponse(response)

        return marshalToXml(response)
    }

    private fun createAssertion(user: User, requestId: String, recipient: String): Assertion {
        val assertion = createSAMLObject(Assertion.DEFAULT_ELEMENT_NAME) as Assertion
        assertion.id = "_" + UUID.randomUUID().toString()
        assertion.issueInstant = Instant.now()
        assertion.issuer = createIssuer()

        // Add Subject
        val subject = createSAMLObject(Subject.DEFAULT_ELEMENT_NAME) as Subject
        val nameID = createSAMLObject(NameID.DEFAULT_ELEMENT_NAME) as NameID
        nameID.value = user.email
        nameID.format = NameID.EMAIL
        subject.nameID = nameID
        assertion.subject = subject

        // Add SubjectConfirmation
        val subjectConfirmation = createSAMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME) as SubjectConfirmation
        subjectConfirmation.method = SubjectConfirmation.METHOD_BEARER
        val subjectConfirmationData =
            createSAMLObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME) as SubjectConfirmationData
        subjectConfirmationData.recipient = recipient
        subjectConfirmationData.notOnOrAfter = Instant.now().plusSeconds(300) // 5 minutes
        subjectConfirmationData.inResponseTo = requestId

        subjectConfirmation.subjectConfirmationData = subjectConfirmationData
        subject.subjectConfirmations.add(subjectConfirmation)

        val conditions = createSAMLObject(Conditions.DEFAULT_ELEMENT_NAME) as Conditions
        conditions.notBefore = Instant.now()
        conditions.notOnOrAfter = Instant.now().plusSeconds(300)

        val audienceRestriction = createSAMLObject(AudienceRestriction.DEFAULT_ELEMENT_NAME) as AudienceRestriction
        val audience = createSAMLObject(Audience.DEFAULT_ELEMENT_NAME) as Audience
        audience.uri = recipient
        audienceRestriction.audiences.add(audience)
        conditions.audienceRestrictions.add(audienceRestriction)

        assertion.conditions = conditions

        val authnStatement = createSAMLObject(AuthnStatement.DEFAULT_ELEMENT_NAME) as AuthnStatement
        authnStatement.authnInstant = Instant.now()
        assertion.authnStatements.add(authnStatement)

        val authnContext = createSAMLObject(AuthnContext.DEFAULT_ELEMENT_NAME) as AuthnContext
        val authnContextClassRef = createSAMLObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME) as AuthnContextClassRef
        authnContextClassRef.uri = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
        authnContext.authnContextClassRef = authnContextClassRef
        authnStatement.authnContext = authnContext

        assertion.authnStatements.add(authnStatement)

        // Add Attribute Statement with user roles
        val attrStatement = createSAMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME) as AttributeStatement

        // Add email attribute
        val emailAttr = createAttribute("email", user.email)
        attrStatement.attributes.add(emailAttr)

        // Add roles attribute
        val rolesAttr = createAttribute("roles", user.roles.joinToString(","))
        attrStatement.attributes.add(rolesAttr)

        assertion.attributeStatements.add(attrStatement)

        return assertion
    }

    private fun createAttribute(name: String, value: String): Attribute {
        val attribute = createSAMLObject(Attribute.DEFAULT_ELEMENT_NAME) as Attribute
        attribute.name = name

        val attributeValue = createSAMLObject(AttributeValue.DEFAULT_ELEMENT_NAME) as XSAnyImpl
        attributeValue.textContent = value

        attribute.attributeValues.add(attributeValue)

        return attribute
    }


    private fun createSuccessResponse(assertion: Assertion, inResponseTo: String, recipient: String): Response {
        val response = createSAMLObject(Response.DEFAULT_ELEMENT_NAME) as Response
        response.id = "_" + UUID.randomUUID().toString()
        response.issueInstant = Instant.now()
        response.destination = recipient
        response.issuer = createIssuer()
        response.inResponseTo = inResponseTo
        response.status = createStatus(StatusCode.SUCCESS)

        response.assertions.add(assertion)
        return response
    }

    private fun createStatus(statusCodeString: String): Status {
        val status = createSAMLObject(Status.DEFAULT_ELEMENT_NAME) as Status
        val statusCode = createSAMLObject(StatusCode.DEFAULT_ELEMENT_NAME) as StatusCode
        statusCode.value = statusCodeString
        status.statusCode = statusCode
        return status
    }

    private fun createIssuer(): Issuer {
        val issuer = createSAMLObject(Issuer.DEFAULT_ELEMENT_NAME) as Issuer
        issuer.value = "http://localhost:8081/saml/metadata"
        return issuer
    }

    private fun createSAMLObject(qname: javax.xml.namespace.QName): org.opensaml.core.xml.XMLObject {
        return XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname)!!.buildObject(qname)
    }

    private fun marshalToXml(samlObject: org.opensaml.core.xml.XMLObject): String {
        val marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory()
        val marshaller = marshallerFactory.getMarshaller(samlObject)
            ?: throw IllegalStateException("No marshaller found for ${samlObject.elementQName}")

        val element = marshaller.marshall(samlObject)

        val writer = StringWriter()
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()

        // Ensure OpenSAML uses the correct canonicalization
        transformer.setOutputProperty(OutputKeys.INDENT, "no")
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.setOutputProperty(OutputKeys.METHOD, "xml")
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "")

        transformer.transform(DOMSource(element), StreamResult(writer))

        return writer.toString()
    }

    private fun signAssertion(assertion: Assertion) {
        val signature = createSignature()
        assertion.signature = signature
        XMLObjectProviderRegistrySupport.getMarshallerFactory()
            .getMarshaller(assertion)
            ?.marshall(assertion)
        Signer.signObject(signature)
    }

    private fun signResponse(response: Response) {
        val signature = createSignature()
        addKeyInfo(signature)
        response.signature = signature
        XMLObjectProviderRegistrySupport.getMarshallerFactory()
            .getMarshaller(response)
            ?.marshall(response)
        Signer.signObject(signature)
    }

    private fun createSignature(): Signature {
        val signature = createSAMLObject(Signature.DEFAULT_ELEMENT_NAME) as Signature
        signature.signingCredential = signingCredential
        signature.signatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256
        signature.canonicalizationAlgorithm = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS
        return signature
    }

    private fun addKeyInfo(signature: Signature) {
        val keyInfo = createSAMLObject(KeyInfo.DEFAULT_ELEMENT_NAME) as KeyInfo
        val x509Data = createSAMLObject(X509Data.DEFAULT_ELEMENT_NAME) as X509Data
        val x509Certificate = createSAMLObject(X509Certificate.DEFAULT_ELEMENT_NAME) as X509Certificate

        val certificate = loadCertificate()
        x509Certificate.value = Base64.getEncoder().encodeToString(certificate.encoded)
        x509Data.x509Certificates.add(x509Certificate)
        keyInfo.x509Datas.add(x509Data)

        signature.keyInfo = keyInfo
    }

    private fun loadPrivateKey(): PrivateKey {
        val pkcs8Lines = privateKeyContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\\s".toRegex(), "")

        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(pkcs8Lines))
        return keyFactory.generatePrivate(keySpec)
    }

    fun getSigningCertificate(): java.security.cert.X509Certificate {
        return loadCertificate()
    }

}