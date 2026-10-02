package vn.ndang.lgfamilyremote

import kotlinx.coroutines.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import vn.ndang.lgfamilyremote.network.WebOsSession
import java.util.concurrent.atomic.AtomicInteger

class WebOsSessionTest {
    private fun session(server: MockWebServer, allowPairing: Boolean = true, key: String = "") = WebOsSession(
        TvConfig("192.168.1.20", secure = false, clientKey = key), allowPairing,
        endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://"))

    @Test fun pairsAndExecutesRequest() = runBlocking {
        val server = MockWebServer()
        val seen = CompletableDeferred<JSONObject>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = JSONObject(text)
                if (j.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"saved-key"}}""")
                } else {
                    seen.complete(j)
                    webSocket.send(JSONObject().put("id", j.getString("id")).put("type", "response")
                        .put("payload", JSONObject().put("returnValue", true)).toString())
                }
            }
        }))
        server.start()
        val client = session(server)
        try {
            assertEquals("saved-key", client.connect().clientKey)
            assertTrue(client.request("audio/volumeUp").getBoolean("returnValue"))
            assertEquals("ssap://audio/volumeUp", seen.await().getString("uri"))
        } finally { client.close(); server.shutdown() }
    }

    @Test fun serverErrorDoesNotReportSuccess() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = JSONObject(text)
                if (j.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key"}}""")
                } else webSocket.send(JSONObject().put("id", j.getString("id")).put("type", "response")
                    .put("payload", JSONObject().put("returnValue", false)).toString())
            }
        }))
        server.start()
        val client = session(server)
        try {
            client.connect()
            try { client.request("system.launcher/launch"); fail("Expected command rejection") }
            catch (e: TvException) { assertEquals(ErrorKind.COMMAND, e.kind) }
        } finally { client.close(); server.shutdown() }
    }

    @Test fun concurrentRemoteRequestsAreMultiplexedWithoutGlobalLock() = runBlocking {
        val server = MockWebServer()
        val count = AtomicInteger()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = JSONObject(text)
                if (j.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key"}}""")
                } else {
                    count.incrementAndGet()
                    launch {
                        delay(if (j.optString("uri").endsWith("volumeUp")) 120 else 20)
                        webSocket.send(JSONObject().put("id", j.getString("id")).put("type", "response")
                            .put("payload", JSONObject().put("returnValue", true)).toString())
                    }
                }
            }
        }))
        server.start()
        val client = session(server)
        try {
            client.connect()
            val up = async { client.request("audio/volumeUp") }
            val down = async { client.request("audio/volumeDown") }
            assertTrue(withTimeout(3000) { down.await() }.getBoolean("returnValue"))
            assertTrue(withTimeout(3000) { up.await() }.getBoolean("returnValue"))
            assertEquals(2, count.get())
        } finally { client.close(); server.shutdown() }
    }

    @Test fun disconnectFailsPendingCommandWithoutReplay() = runBlocking {
        val server = MockWebServer()
        val count = AtomicInteger()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (JSONObject(text).optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key"}}""")
                } else { count.incrementAndGet(); webSocket.close(1001, "Test disconnect") }
            }
        }))
        server.start()
        val client = session(server)
        try {
            client.connect()
            try { withTimeout(3000) { client.request("audio/volumeDown") }; fail("Expected disconnect") }
            catch (e: TvException) { assertEquals(ErrorKind.NETWORK, e.kind) }
            assertEquals(1, count.get())
            assertFalse(client.isOpen)
        } finally { client.close(); server.shutdown() }
    }

    @Test fun bootReconnectCanUseShortRegistrationTimeout() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Accept the socket but deliberately never finish registration.
            }
        }))
        server.start()
        val client = session(server, false, "old-key")
        try {
            try {
                withTimeout(2000) { client.connect(registrationTimeoutMs = 1000) }
                fail("Expected wake reconnect timeout")
            } catch (e: TvException) {
                assertEquals(ErrorKind.NETWORK, e.kind)
            }
        } finally {
            client.close()
            server.shutdown()
        }
    }

    @Test fun backgroundReconnectDoesNotAcceptNewPairingPrompt() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                assertEquals("old-key", JSONObject(text).getJSONObject("payload").getString("client-key"))
                webSocket.send("""{"id":"register","type":"response","payload":{"pairingType":"PROMPT"}}""")
            }
        }))
        server.start()
        val client = session(server, false, "old-key")
        try {
            try { client.connect(); fail("Must request explicit pairing") }
            catch (e: TvException) { assertEquals(ErrorKind.PAIRING_REQUIRED, e.kind) }
        } finally { client.close(); server.shutdown() }
    }
}
