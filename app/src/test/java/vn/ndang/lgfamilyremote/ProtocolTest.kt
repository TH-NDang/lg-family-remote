package vn.ndang.lgfamilyremote

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import okhttp3.tls.HeldCertificate
import vn.ndang.lgfamilyremote.network.*
import java.security.cert.CertificateException

class ProtocolTest {
    @Test fun acceptsOnlyPrivateIpv4() {
        listOf("10.0.0.1", "172.16.0.5", "172.31.255.1", "192.168.1.20", "169.254.2.3").forEach {
            assertTrue(it, LanRules.isPrivateIpv4(it))
        }
        listOf("8.8.8.8", "127.0.0.1", "172.32.0.1", "192.168.1.300", "192.168.01.2",
            "localhost", "::1", "https://192.168.1.2", "192.168.1.2:3001", "192.168.1.2@evil.com").forEach {
            assertFalse(it, LanRules.isPrivateIpv4(it))
        }
    }
    @Test fun descriptionIsConfinedToRespondingHost() {
        assertTrue(LanRules.descriptionAllowed("http://192.168.1.20:1234/device.xml", "192.168.1.20"))
        assertFalse(LanRules.descriptionAllowed("http://evil.com/device.xml", "192.168.1.20"))
        assertFalse(LanRules.descriptionAllowed("http://user@192.168.1.20/", "192.168.1.20"))
        assertFalse(LanRules.descriptionAllowed("file:///etc/passwd", "192.168.1.20"))
    }
    @Test fun upgradesPointerWithoutDowngrade() {
        val actual = LanRules.pointerUrl("ws://192.168.1.20:3000/resources/pointer?token=x", TvConfig("192.168.1.20"))
        assertEquals("wss://192.168.1.20:3001/resources/pointer?token=x", actual)
    }
    @Test(expected = TvException::class) fun rejectsPointerToOtherHost() {
        LanRules.pointerUrl("wss://192.168.1.99:3001/", TvConfig("192.168.1.20"))
    }
    @Test(expected = TvException::class) fun rejectsUnexpectedPointerPort() {
        LanRules.pointerUrl("ws://192.168.1.20:8080/", TvConfig("192.168.1.20"))
    }
    @Test fun parsesOldVolumeResponse() {
        assertEquals(VolumeState(25, true), Protocol.volume(JSONObject("""{"volume":25,"muted":true}""")))
    }
    @Test fun parsesNewVolumeResponse() {
        assertEquals(VolumeState(33, false), Protocol.volume(JSONObject("""{"volumeStatus":{"volume":33,"muteStatus":false}}""")))
    }
    @Test fun missingMuteIsNotAssumedFalse() {
        assertNull(Protocol.volume(JSONObject("""{"volume":20}""")).muted)
    }
    @Test fun protocolErrorsAreNotSuccess() {
        assertTrue(Protocol.isError(JSONObject("""{"type":"error","error":"403"}""")))
        assertTrue(Protocol.isError(JSONObject("""{"type":"response","payload":{"returnValue":false}}""")))
        assertFalse(Protocol.isError(JSONObject("""{"type":"response","payload":{"returnValue":true}}""")))
    }
    @Test fun correctlyFramesButtons() {
        assertEquals("type:button\nname:HOME\n\n", Protocol.button("HOME"))
        assertEquals("type:button\nname:ENTER\n\n", Protocol.button("ENTER"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsButtonInjection() { Protocol.button("HOME\nname:POWER") }
    @Test fun remembersClientKeyInRegistration() {
        val j = JSONObject(Protocol.register(TvConfig("192.168.1.20", clientKey = "secret")))
        assertEquals("register", j.getString("type"))
        assertEquals("secret", j.getJSONObject("payload").getString("client-key"))
        assertFalse(j.getJSONObject("payload").getBoolean("forcePairing"))
    }
    @Test fun neverImpersonatesLgSignedApplication() {
        val m = JSONObject(Protocol.register(TvConfig("192.168.1.20"))).getJSONObject("payload").getJSONObject("manifest")
        assertFalse(m.has("signed"))
        assertFalse(m.has("signatures"))
    }
    @Test fun youtubeSelectionExcludesKids() {
        val p = JSONObject("""{"apps":[{"id":"kids","title":"YouTube Kids"},{"id":"custom.youtube","title":"YouTube"}]}""")
        assertEquals("custom.youtube", Protocol.youtubeApp(p))
    }
    @Test fun youtubeMissingDoesNotReturnAnotherApp() {
        assertNull(Protocol.youtubeApp(JSONObject("""{"apps":[{"id":"netflix","title":"Netflix"}]}""")))
    }
    @Test fun ssdpHeadersAreCaseInsensitive() {
        val h = Protocol.ssdpHeaders("HTTP/1.1 200 OK\r\nLocation: http://192.168.1.2:8000/x\r\nUSN: uuid:tv::service\r\n\r\n")
        assertEquals("uuid:tv::service", h["usn"])
        assertEquals("http://192.168.1.2:8000/x", h["location"])
    }
    @Test fun secretsExcludedFromDebugString() {
        val tv = TvConfig("192.168.1.20", clientKey = "private-client-key", certificateSha256 = "private-pin")
        assertFalse(tv.toString().contains("private"))
        assertEquals(tv, TvConfig.fromJson(tv.toJson()))
    }
    @Test fun pinsCertificateOnFirstUse() {
        val cert = HeldCertificate.Builder().commonName("test-tv").build().certificate
        val trust = CertificateTrust("", true)
        trust.checkServerTrusted(arrayOf(cert), "RSA")
        assertEquals(CertificateTrust.fingerprint(cert), trust.observedPin)
        trust.checkServerTrusted(arrayOf(cert), "RSA")
    }
    @Test(expected = ChangedTvCertificate::class) fun rejectsChangedCertificate() {
        val first = HeldCertificate.Builder().commonName("tv1").build().certificate
        val second = HeldCertificate.Builder().commonName("tv2").build().certificate
        CertificateTrust(CertificateTrust.fingerprint(first), false).checkServerTrusted(arrayOf(second), "RSA")
    }
    @Test(expected = CertificateException::class) fun requiresExplicitFirstPairing() {
        val cert = HeldCertificate.Builder().commonName("tv").build().certificate
        CertificateTrust("", false).checkServerTrusted(arrayOf(cert), "RSA")
    }
}
