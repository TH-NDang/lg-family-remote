package vn.ndang.lgfamilyremote

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import vn.ndang.lgfamilyremote.network.Protocol
import vn.ndang.lgfamilyremote.network.WebOsSession
import vn.ndang.lgfamilyremote.network.requestPowerOff
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class PowerCommandsTest {
    private data class Result(val acknowledged: Boolean?, val error: ErrorKind?, val count: Int, val uri: String)
    private suspend fun execute(mode: String): Result {
        val server = MockWebServer()
        val count = AtomicInteger()
        val uri = AtomicReference("")
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = JSONObject(text)
                if (message.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"test-key"}}""")
                } else {
                    count.incrementAndGet()
                    uri.set(message.optString("uri"))
                    if (mode == "disconnect") webSocket.close(1001, "TV shutting down")
                    else webSocket.send(JSONObject().put("id", message.getString("id")).put("type", "response")
                        .put("payload", JSONObject().put("returnValue", mode == "accept")).toString())
                }
            }
        }))
        server.start()
        val client = WebOsSession(TvConfig("192.168.1.20", secure = false), true,
            endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://"))
        try {
            client.connect()
            return try {
                val acknowledged = withTimeout(3000) { client.requestPowerOff() }
                Result(acknowledged, null, count.get(), uri.get())
            } catch (e: TvException) {
                Result(null, e.kind, count.get(), uri.get())
            }
        } finally { client.close(); server.shutdown() }
    }

    @Test fun sendsExactlyOnePowerOffRequest() = runBlocking {
        val result = execute("accept")
        assertEquals(true, result.acknowledged)
        assertNull(result.error)
        assertEquals("ssap://system/turnOff", result.uri)
        assertEquals(1, result.count)
    }
    @Test fun permissionFailureRemainsAnError() = runBlocking {
        val result = execute("reject")
        assertNull(result.acknowledged)
        assertEquals(ErrorKind.COMMAND, result.error)
        assertEquals(1, result.count)
    }
    @Test fun disconnectIsUnknownNotConfirmedSuccessAndNotReplayed() = runBlocking {
        val result = execute("disconnect")
        assertEquals(false, result.acknowledged)
        assertNull(result.error)
        assertEquals(1, result.count)
    }
    @Test fun disconnectedSessionCannotSendPower() = runBlocking {
        val client = WebOsSession(TvConfig("192.168.1.20", secure = false), true)
        try {
            try { client.requestPowerOff(); fail("Must reject before connecting") }
            catch (e: TvException) { assertEquals(ErrorKind.NETWORK, e.kind) }
        } finally { client.close() }
    }
    @Test fun pairingRequestsPowerPermissionWithoutDiscardingSavedKey() {
        val payload = JSONObject(Protocol.register(TvConfig("192.168.1.20", clientKey = "keep-key")))
            .getJSONObject("payload")
        val permissions = payload.getJSONObject("manifest").getJSONArray("permissions")
        assertTrue((0 until permissions.length()).any { permissions.getString(it) == "CONTROL_POWER" })
        assertEquals("keep-key", payload.getString("client-key"))
        assertFalse(payload.getBoolean("forcePairing"))
    }
}
