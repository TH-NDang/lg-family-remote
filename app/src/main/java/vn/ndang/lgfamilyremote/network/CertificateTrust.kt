package vn.ndang.lgfamilyremote.network

import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class ChangedTvCertificate : CertificateException("TV certificate fingerprint changed")

/** Per-TV trust on first use, never a global trust-all manager. */
class CertificateTrust(private val savedPin: String, private val allowFirstUse: Boolean) : X509TrustManager {
    @Volatile var observedPin: String = ""
        private set
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client certificates not supported")
    }
    @Synchronized override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("No certificate")
        val pin = fingerprint(leaf)
        val expected = savedPin.ifBlank { observedPin }
        if (expected.isBlank() && !allowFirstUse) throw CertificateException("Pairing required")
        if (expected.isNotBlank() && !MessageDigest.isEqual(expected.toByteArray(), pin.toByteArray())) {
            throw ChangedTvCertificate()
        }
        leaf.checkValidity()
        observedPin = pin
    }
    companion object {
        fun fingerprint(cert: X509Certificate): String = MessageDigest.getInstance("SHA-256")
            .digest(cert.encoded).joinToString("") { "%02x".format(it) }
    }
}
