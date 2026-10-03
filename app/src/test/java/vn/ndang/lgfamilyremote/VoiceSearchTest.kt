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
    fun youtubeSearchUrlEncodesVietnameseQuery() {
        assertEquals(
            "https://www.youtube.com/tv?q=nh%E1%BA%A1c%20thi%E1%BA%BFu%20nhi",
            Protocol.youtubeSearchUrl("  nhạc   thiếu nhi  ")
        )
        assertEquals(null, Protocol.youtubeSearchUrl("   "))
    }

    @Test
    fun remoteSupportsLaunchAndTextInputPermissions() {
        val manifest = JSONObject(Protocol.register(TvConfig("192.168.1.20")))
            .getJSONObject("payload").getJSONObject("manifest")
        val permissions = manifest.getJSONArray("permissions")
        val names = (0 until permissions.length()).map { permissions.getString(it) }
        assertTrue(names.contains("LAUNCH"))
        assertTrue(names.contains("CONTROL_INPUT_TEXT"))
    }

    @Test
    fun voiceSearchLaunchesYoutubeSearchDeepLink() = runBlocking {
        val server = MockWebServer()
        val seen = ConcurrentLinkedQueue<JSONObject>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key"}}""")
                } else {
                    seen.add(json)
                    val payload = if (json.optString("uri").endsWith("listApps")) {
                        JSONObject()
                            .put("returnValue", true)
                            .put("apps", org.json.JSONArray().put(
                                JSONObject().put("id", "youtube.leanback.v4").put("title", "YouTube")
                            ))
                    } else {
                        JSONObject().put("returnValue", true).put("sessionId", "yt")
                    }
                    webSocket.send(
                        JSONObject()
                            .put("id", json.getString("id"))
                            .put("type", "response")
                            .put("payload", payload)
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
                client.launchYouTubeSearch("nhạc thiếu nhi", "")
            }

            assertEquals(2, seen.size)
            val launch = seen.elementAt(1)
            assertEquals("ssap://system.launcher/launch", launch.getString("uri"))

            val payload = launch.getJSONObject("payload")
            val target = "https://www.youtube.com/tv?q=nh%E1%BA%A1c%20thi%E1%BA%BFu%20nhi"
            assertEquals("youtube.leanback.v4", payload.getString("id"))
            assertEquals(target, payload.getString("contentId"))
            assertEquals(target, payload.getJSONObject("params").getString("contentTarget"))
        } finally {
            client.close()
            server.shutdown()
        }
    }
}
