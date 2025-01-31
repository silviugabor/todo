package com.todo.idp.controller

import com.todo.idp.service.SamlService
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

@Controller
@RequestMapping("/saml")
class SamlController(private val samlService: SamlService) {

    @GetMapping("/metadata", produces = [MediaType.APPLICATION_XML_VALUE])
    @ResponseBody
    fun metadata(): String {
        return samlService.generateMetadata()
    }

    @PostMapping("/login")
    fun showLoginForm(
        @RequestParam("SAMLRequest") samlRequest: String,
        @RequestParam(name = "RelayState", required = false) relayState: String?,
        model: Model
    ): String {
        model.addAttribute("samlRequest", samlRequest)
        model.addAttribute("relayState", relayState)
        return "login"
    }

    @PostMapping("/sso")
    @ResponseBody
    fun singleSignOn(
        @RequestParam("SAMLRequest") samlRequest: String,
        @RequestParam("username") username: String,
        @RequestParam("password") password: String
    ): String {
        val decodedRequest = String(Base64.getDecoder().decode(samlRequest))

        val requestId = extractRequestId(decodedRequest)

        val samlResponse = samlService.authenticateAndGenerateSamlResponse(username, password, requestId)
        return generateAutoSubmitForm(samlResponse, requestId)
    }

    private fun extractRequestId(samlRequest: String): String {
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()

            val doc: Document = builder.parse(ByteArrayInputStream(samlRequest.toByteArray()))
            val authnRequest = doc.documentElement

            val id = authnRequest.getAttribute("ID")
            if (!id.isNullOrEmpty()) {
                return id
            }
            return "_" + UUID.randomUUID().toString()

        } catch (e: Exception) {
            println("Error parsing SAML request: ${e.message}")
            return "_" + UUID.randomUUID().toString()
        }
    }

    private fun generateAutoSubmitForm(samlResponse: String, requestId: String): String {
        val encodedResponse = Base64.getEncoder().encodeToString(samlResponse.toByteArray())
        val destination = "http://localhost:8080/login/saml2/sso/default"
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