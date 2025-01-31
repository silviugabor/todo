package com.todo.idp.controller

import com.todo.idp.service.SamlRequestProcessor
import com.todo.idp.service.SamlService
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/saml")
class SamlController(
    private val samlService: SamlService,
    private val samlRequestProcessor: SamlRequestProcessor
) {

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
        model.addAllAttributes(samlRequestProcessor.processLoginRequest(samlRequest, relayState))
        return "login"
    }

    @PostMapping("/sso")
    @ResponseBody
    fun singleSignOn(
        @RequestParam("SAMLRequest") samlRequest: String,
        @RequestParam("username") username: String,
        @RequestParam("password") password: String
    ): String {
        return samlRequestProcessor.processSingleSignOn(samlRequest, username, password)
    }

}