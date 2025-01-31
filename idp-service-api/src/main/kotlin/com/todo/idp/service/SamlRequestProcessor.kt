package com.todo.idp.service

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.io.Unmarshaller
import org.opensaml.core.xml.io.UnmarshallerFactory
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.RequestAbstractType
import org.opensaml.security.x509.BasicX509Credential
import org.opensaml.xmlsec.signature.support.SignatureValidator
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

@Service
class SamlRequestProcessor(
    private val samlService: SamlService
) {
    private val unmarshallerFactory: UnmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory()

    fun processLoginRequest(
        samlRequest: String,
        relayState: String?
    ): Map<String, String?> {
        return mapOf(
            "samlRequest" to samlRequest,
            "relayState" to relayState
        )
    }

    fun processSingleSignOn(
        samlRequest: String,
        username: String,
        password: String
    ): String {
        val decodedRequest = String(Base64.getDecoder().decode(samlRequest))

        val authnRequest = parseSamlRequest(decodedRequest)
        validateSamlRequest(authnRequest)

        val requestId = authnRequest.id
        val destination = authnRequest.assertionConsumerServiceURL

        val samlResponse = samlService.authenticateAndGenerateSamlResponse(
            username,
            password,
            requestId
        )

        return generateAutoSubmitForm(samlResponse, requestId, destination)
    }

    private fun parseSamlRequest(samlRequest: String): AuthnRequest {
        val document = parseDocument(samlRequest)
        val unmarshaller = unmarshallerFactory.getUnmarshaller(document.documentElement) as Unmarshaller
        return unmarshaller.unmarshall(document.documentElement) as AuthnRequest
    }

    private fun parseDocument(xmlString: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isXIncludeAware = false
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }

        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xmlString.toByteArray()))
    }

    private fun validateSamlRequest(request: AuthnRequest) {
        validateSignature(request)
        validateDestination(request)
        validateIssuer(request)
        validateRequestExpiration(request)
    }

    private fun validateSignature(request: AuthnRequest) {
        val credential = BasicX509Credential(samlService.getSigningCertificate())
        try {
            SignatureValidator.validate(request.signature!!, credential)
        } catch (e: Exception) {
            throw SamlValidationException("Invalid SAML request signature: ${e.message}")
        }
    }

    private fun validateDestination(request: RequestAbstractType) {
        val destination = request.destination
        if (destination.isNullOrEmpty() || !destination.startsWith("http://localhost:8081")) {
            throw SamlValidationException("Invalid destination: $destination")
        }
    }

    private fun validateIssuer(request: RequestAbstractType) {
        val issuer = request.issuer
        if (issuer == null || issuer.value.isNullOrEmpty()) {
            throw SamlValidationException("Missing or invalid issuer")
        }
    }

    private fun validateRequestExpiration(request: AuthnRequest) {
        val now = Instant.now()

        request.conditions?.let { conditions ->
            conditions.notBefore?.let { notBefore ->
                if (now.isBefore(notBefore)) {
                    throw SamlValidationException("Request is not yet valid")
                }
            }

            conditions.notOnOrAfter?.let { notOnOrAfter ->
                if (now.isAfter(notOnOrAfter)) {
                    throw SamlValidationException("Request has expired")
                }
            }
        }
    }

    private fun generateAutoSubmitForm(
        samlResponse: String,
        requestId: String,
        destination: String
    ): String {
        val encodedResponse = Base64.getEncoder().encodeToString(samlResponse.toByteArray())

        return """
            <!DOCTYPE html>
            <html>
            <body onload="document.forms[0].submit()">
                <form method="post" action="$destination">
                    <input type="hidden" name="SAMLResponse" value="$encodedResponse"/>
                    <input type="hidden" name="requestId" value="$requestId"/>
                </form>
            </body>
            </html>
        """.trimIndent()
    }
}

class SamlValidationException(message: String) : RuntimeException(message)