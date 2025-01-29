package com.todo.idp

import org.opensaml.security.credential.Credential
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.support.SignatureException
import org.opensaml.xmlsec.signature.support.SignatureValidator
import java.util.*
import javax.annotation.Nonnull

class App

fun main(args: Array<String>) {
    verifySignature(null, null)
}
 fun verifySignature(@Nonnull signature: Signature?, @Nonnull credential: Credential?): Boolean {
    try {
        SignatureValidator.validate(signature!!, credential!!)
    } catch (e: SignatureException) {
        return false
    }

    return true
}