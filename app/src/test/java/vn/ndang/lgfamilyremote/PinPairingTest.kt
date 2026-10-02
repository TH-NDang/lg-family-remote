package vn.ndang.lgfamilyremote

import kotlinx.coroutines.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import vn.ndang.lgfamilyremote.network.Protocol
import vn.ndang.lgfamilyremote.network.WebOsSession
import java.util.concurrent.atomic.AtomicInteger

class PinPairingTest {
    @Test fun pinRegisterAndSetPinSaveOnlyClientKey() = runBlocking {
        val server = MockWebServer()
        val pairing = CompletableDeferred<PairingKind>()
        val pinRequests = AtomicInteger()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = JSONObject(text)
                if (j.optString("type") == "register") {
                    assertEquals("PIN", j.getJSONObject("payload").getString("pairingType"))
                    webSocket.send("""{"id":"register","type":"response","payload":{"pairingType":"PIN"}}""")
                } else if (j.optString("uri") == "ssap://pairing/setPin") {
                    pinRequests.incrementAndGet()
                    assertEquals("482913", j.getJSONObject("payload").getString("pin"))
                    webSocket.send(JSONObject().put("id", j.getString("id")).put("type", "response")
                        .put("payload", JSONObject().put("returnValue", true)).toString())
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"saved-client-key"}}""")
                }
            }
        }))
        server.start()
        val client = WebOsSession(
            TvConfig("192.168.1.20", secure = false), true,
            onPairing = { pairing.complete(it) },
            endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://")
        )
        try {
            val connection = async { client.connect() }
            assertEquals(PairingKind.PIN, withTimeout(2000) { pairing.await() })
            client.submitPin("482913")
            val saved = withTimeout(2000) { connection.await() }
            assertEquals("saved-client-key", saved.clientKey)
            assertEquals(1, pinRequests.get())
            assertFalse(saved.toJson().toString().contains("482913"))
        } finally { client.close(); server.shutdown() }
    }

    @Test fun wrongPinCanBeRetriedWithoutPersistingIt() = runBlocking {
        val server = MockWebServer()
        val pairing = CompletableDeferred<Unit>()
        val attempts = AtomicInteger()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = JSONObject(text)
                if (j.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"response","payload":{"pairingType":"PIN"}}""")
                    pairing.complete(Unit)
                } else if (j.optString("uri") == "ssap://pairing/setPin") {
                    if (attempts.incrementAndGet() == 1) {
                        webSocket.send(JSONObject().put("id", j.getString("id")).put("type", "error")
                            .put("error", "401 invalid pin").put("payload", JSONObject()).toString())
                    } else {
                        webSocket.send(JSONObject().put("id", j.getString("id")).put("type", "response")
                            .put("payload", JSONObject().put("returnValue", true)).toString())
                        webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key-after-retry"}}""")
                    }
                }
            }
        }))
        server.start()
        val client = WebOsSession(TvConfig("192.168.1.20", secure = false), true,
            endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://"))
        try {
            val connection = async { client.connect() }
            withTimeout(2000) { pairing.await() }
            try { client.submitPin("111111"); fail("Expected wrong PIN") }
            catch (e: TvException) { assertEquals(ErrorKind.REJECTED, e.kind) }
            client.submitPin("222222")
            assertEquals("key-after-retry", withTimeout(2000) { connection.await() }.clientKey)
            assertEquals(2, attempts.get())
        } finally { client.close(); server.shutdown() }
    }

    @Test fun promptResponseStillUsesFirstScreenFlow() = runBlocking {
        val server = MockWebServer()
        val kind = CompletableDeferred<PairingKind>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = JSONObject(text)
                if (j.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"response","payload":{"pairingType":"PROMPT"}}""")
                    if (!kind.isCompleted) kind.complete(PairingKind.PROMPT)
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"prompt-key"}}""")
                }
            }
        }))
        server.start()
        val client = WebOsSession(TvConfig("192.168.1.20", secure = false), true,
            onPairing = { if (!kind.isCompleted) kind.complete(it) },
            endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://"))
        try {
            assertEquals("prompt-key", client.connect().clientKey)
            assertEquals(PairingKind.PROMPT, kind.await())
        } finally { client.close(); server.shutdown() }
    }

    @Test fun pinValidationIsStrictAndTransient() {
        assertEquals("1234", Protocol.normalizePin(" 1234 "))
        assertEquals("123456789012", Protocol.normalizePin("123456789012"))
        listOf("", "123", "1234567890123", "12 34", "12a4", "１２３４").forEach {
            assertNull(it, Protocol.normalizePin(it))
        }
        assertEquals("ssap://pairing/setPin",
            JSONObject(Protocol.pinRequest("p1", "123456")).getString("uri"))
    }
}
