package vn.ndang.lgfamilyremote

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.ndang.lgfamilyremote.network.Protocol
import vn.ndang.lgfamilyremote.network.WebOsSession
import java.util.concurrent.ConcurrentLinkedQueue

class VoiceSearchTest {
    @Test
    fun manifestRequestsTextInputPermission() {
        val manifest = JSONObject(Protocol.register(TvConfig("192.168.1.20")))
            .getJSONObject("payload").getJSONObject("manifest")
        val permissions = manifest.getJSONArray("permissions")
        val names = (0 until permissions.length()).map { permissions.getString(it) }
        assertTrue(names.contains("CONTROL_INPUT_TEXT"))
    }

    @Test
    fun voiceSearchInsertsTextThenPressesEnter() = runBlocking {
        val server = MockWebServer()
        val seen = ConcurrentLinkedQueue<JSONObject>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key"}}""")
                } else {
                    seen.add(json)
                    webSocket.send(
                        JSONObject()
                            .put("id", json.getString("id"))
                            .put("type", "response")
                            .put("payload", JSONObject().put("returnValue", true))
                            .toString()
                    )
                }
            }
        }))
        server.start()

        val client = WebOsSession(
            TvConfig("192.168.1.20", secure = false),
            true,
            endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://")
        )

        try {
            withTimeout(4000) {
                client.connect()
                client.voiceSearch("  nhạc   thiếu nhi  ")
            }

            assertEquals(2, seen.size)
            val first = seen.elementAt(0)
            val second = seen.elementAt(1)

            assertEquals("ssap://com.webos.service.ime/insertText", first.getString("uri"))
            assertEquals("nhạc thiếu nhi", first.getJSONObject("payload").getString("text"))
            assertEquals(0, first.getJSONObject("payload").getInt("replace"))
            assertEquals("ssap://com.webos.service.ime/sendEnterKey", second.getString("uri"))
        } finally {
            client.close()
            server.shutdown()
        }
    }
}
