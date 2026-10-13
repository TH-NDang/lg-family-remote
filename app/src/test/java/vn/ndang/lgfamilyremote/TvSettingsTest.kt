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
import java.util.concurrent.ConcurrentLinkedQueue

class TvSettingsTest {
    private enum class LaunchReply { ACCEPT, REJECT, SILENT }

    private fun withTv(reply: LaunchReply, block: suspend (WebOsSession, ConcurrentLinkedQueue<JSONObject>) -> Unit) =
        runBlocking {
            val server = MockWebServer()
            val seen = ConcurrentLinkedQueue<JSONObject>()
            server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    val json = JSONObject(text)
                    if (json.optString("type") == "register") {
                        webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key"}}""")
                        return
                    }
                    seen.add(json)
                    val launch = json.optString("uri").endsWith("system.launcher/launch")
                    if (launch && reply == LaunchReply.SILENT) return
                    val ok = !launch || reply == LaunchReply.ACCEPT
                    webSocket.send(JSONObject().put("id", json.getString("id")).put("type", "response")
                        .put("payload", JSONObject().put("returnValue", ok)).toString())
                }
            }))
            server.start()
            val client = WebOsSession(TvConfig("192.168.1.20", secure = false), true,
                endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://"))
            try {
                withTimeout(4000) { client.connect() }
                block(client, seen)
            } finally {
                client.close()
                server.shutdown()
            }
        }

    @Test fun framesSettingsKey() {
        assertEquals("type:button\nname:MENU\n\n", Protocol.button("MENU"))
    }

    @Test fun opensSettingsAppWithoutPressingMenu() = withTv(LaunchReply.ACCEPT) { client, seen ->
        withTimeout(4000) { client.openSettings() }
        assertEquals(1, seen.size)
        val launch = seen.first()
        assertEquals("ssap://system.launcher/launch", launch.getString("uri"))
        assertEquals("com.palm.app.settings", launch.getJSONObject("payload").getString("id"))
    }

    @Test fun rejectedLaunchFallsBackToSettingsKey() = withTv(LaunchReply.REJECT) { client, seen ->
        // The mock cannot serve a pointer socket on the TV's address, so the fallback stops
        // after asking for it; reaching that request proves the MENU key path was taken.
        try { withTimeout(4000) { client.openSettings() } } catch (_: TvException) {}
        assertEquals(
            listOf("ssap://system.launcher/launch", "ssap://com.webos.service.networkinput/getPointerInputSocket"),
            seen.map { it.getString("uri") }
        )
    }

    @Test fun unansweredLaunchIsNotRepeatedWithSettingsKey() = withTv(LaunchReply.SILENT) { client, seen ->
        try { withTimeout(8000) { client.openSettings() }; fail("Expected no-reply error") }
        catch (e: TvException) { assertEquals(ErrorKind.COMMAND, e.kind) }
        assertEquals(listOf("ssap://system.launcher/launch"), seen.map { it.getString("uri") })
    }
}
