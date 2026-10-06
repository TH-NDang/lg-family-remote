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
import vn.ndang.lgfamilyremote.network.*
import java.util.concurrent.ConcurrentLinkedQueue

class PowerTest {
    @Test fun oneClickPowerUsesConnectionState() {
        assertEquals(PowerAction.TURN_OFF, powerActionFor(ConnectionState.CONNECTED, false))
        assertEquals(PowerAction.TURN_ON, powerActionFor(ConnectionState.OFFLINE, false))
        assertEquals(PowerAction.TURN_ON, powerActionFor(ConnectionState.IDLE, false))
        assertEquals(PowerAction.TURN_ON, powerActionFor(ConnectionState.CONNECTING, false))
        assertEquals(PowerAction.NONE, powerActionFor(ConnectionState.PAIRING, false))
        assertEquals(PowerAction.NONE, powerActionFor(ConnectionState.SHUTTING_DOWN, false))
        assertEquals(PowerAction.NONE, powerActionFor(ConnectionState.CONNECTED, true))
    }

    @Test fun normalizesSupportedMacFormats() {
        listOf("aa:bb:cc:dd:ee:02", "AA-BB-CC-DD-EE-02", "aabbccddee02").forEach {
            assertEquals("AA:BB:CC:DD:EE:02", WakeProtocol.normalizeMac(it))
        }
    }
    @Test fun rejectsInvalidMulticastAndZeroAddresses() {
        listOf("00:00:00:00:00:00", "FF:FF:FF:FF:FF:FF", "01:00:5e:00:00:01",
            "invalid:aa:bb:cc:dd:ee:02", "aa:bb:cc:dd:ee", "aa-bb:cc-dd:ee-02", "https://aabbccddee02").forEach {
            assertNull(it, WakeProtocol.normalizeMac(it))
        }
    }
    @Test fun parsesAndDeduplicatesManualAddresses() {
        assertEquals(listOf("AA:BB:CC:DD:EE:02", "02:11:22:33:44:55"),
            WakeProtocol.parseMacs("aa:bb:cc:dd:ee:02, AA-BB-CC-DD-EE-02; 02:11:22:33:44:55"))
        assertEquals(emptyList<String>(), WakeProtocol.parseMacs("  "))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidManualList() {
        WakeProtocol.parseMacs("AA:BB:CC:DD:EE:02,wrong")
    }
    @Test fun constructsExactMagicPacket() {
        val packet = WakeProtocol.packet("02:11:22:33:44:55")
        assertEquals(102, packet.size)
        assertArrayEquals(ByteArray(6) { 0xff.toByte() }, packet.copyOfRange(0, 6))
        repeat(16) {
            assertArrayEquals(byteArrayOf(2, 17, 34, 51, 68, 85), packet.copyOfRange(6 + it * 6, 12 + it * 6))
        }
    }
    @Test fun readsModernLgWifiInfoAndWiredInfo() {
        val payload = JSONObject("""{"wifiInfo":{"macAddress":"02:11:22:33:44:55"},"wiredInfo":{"macAddress":"aa:bb:cc:dd:ee:02"},"gateway":{"macAddress":"02:22:22:22:22:22"}}""")
        assertEquals(listOf("02:11:22:33:44:55", "AA:BB:CC:DD:EE:02"), WakeProtocol.fromConnectionInfo(payload))
        assertFalse(WakeProtocol.fromConnectionInfo(payload).contains("02:22:22:22:22:22"))
    }

    @Test fun stillReadsLegacyWifiAndWiredAliases() {
        val payload = JSONObject("""{"wired":{"macAddress":"aa:bb:cc:dd:ee:02"},"wifi":{"macAddress":"02:11:22:33:44:55"}}""")
        assertEquals(
            setOf("AA:BB:CC:DD:EE:02", "02:11:22:33:44:55"),
            WakeProtocol.fromConnectionInfo(payload).toSet()
        )
        assertEquals(emptyList<String>(), WakeProtocol.fromConnectionInfo(JSONObject()))
    }

    @Test fun wakeDeliveryIncludesBroadcastSubnetAndUnicast() {
        assertEquals(
            listOf("255.255.255.255", "192.168.1.255", "192.168.1.20"),
            WakeProtocol.deliveryTargets("192.168.1.7", 24, "192.168.1.20")
        )
    }
    @Test fun broadcastUsesActualSubnetPrefix() {
        assertEquals("192.168.1.255", WakeProtocol.broadcastFor("192.168.1.7", 24, "192.168.1.20"))
        assertEquals("192.168.3.255", WakeProtocol.broadcastFor("192.168.2.7", 22, "192.168.1.20"))
        assertEquals("10.7.255.255", WakeProtocol.broadcastFor("10.7.2.8", 16, "10.7.3.12"))
    }
    @Test fun noBroadcastForForeignOrPointToPointNetwork() {
        assertNull(WakeProtocol.broadcastFor("192.168.2.7", 24, "192.168.1.20"))
        assertNull(WakeProtocol.broadcastFor("192.168.1.7", 32, "192.168.1.7"))
        assertNull(WakeProtocol.broadcastFor("8.8.8.8", 24, "8.8.8.9"))
    }
    @Test fun loadsPreviousConfigWithoutRepairingOrLosingCredentials() {
        val old = JSONObject("""{"host":"192.168.1.20","clientKey":"old-key","certificateSha256":"old-pin","youtubeId":"custom.youtube"}""")
        val tv = TvConfig.fromJson(old)
        assertEquals("old-key", tv.clientKey)
        assertEquals("old-pin", tv.certificateSha256)
        assertEquals("custom.youtube", tv.youtubeId)
        assertTrue(tv.wakeMacs.isEmpty())
    }
    @Test fun persistsWakeAddressesWithExistingConfig() {
        val tv = TvConfig("192.168.1.20", clientKey = "key", wakeMacs = listOf("AA:BB:CC:DD:EE:02"))
        assertEquals(tv, TvConfig.fromJson(tv.toJson()))
        assertFalse(tv.toString().contains("AA:BB"))
    }
    @Test fun requestsPowerAndNetworkPermissionsWithoutSignedImpersonation() {
        val manifest = JSONObject(Protocol.register(TvConfig("192.168.1.20")))
            .getJSONObject("payload").getJSONObject("manifest")
        val permissions = manifest.getJSONArray("permissions")
        val names = (0 until permissions.length()).map { permissions.getString(it) }
        assertTrue(names.contains("CONTROL_POWER"))
        assertTrue(names.contains("READ_NETWORK_STATE"))
        assertFalse(manifest.has("signed"))
    }

    private suspend fun withTv(reply: (WebSocket, JSONObject) -> Unit,
                               block: suspend (WebOsSession, ConcurrentLinkedQueue<JSONObject>) -> Unit) {
        val server = MockWebServer()
        val seen = ConcurrentLinkedQueue<JSONObject>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = JSONObject(text)
                if (j.optString("type") == "register") {
                    webSocket.send("""{"id":"register","type":"registered","payload":{"client-key":"key"}}""")
                } else { seen.add(j); reply(webSocket, j) }
            }
        }))
        server.start()
        val client = WebOsSession(TvConfig("192.168.1.20", secure = false), true,
            endpointOverrideForTests = server.url("/").toString().replace("http://", "ws://"))
        try {
            withTimeout(4000) { client.connect(); block(client, seen) }
        } finally { client.close(); server.shutdown() }
    }
    private fun response(ws: WebSocket, j: JSONObject, payload: JSONObject) {
        ws.send(JSONObject().put("id", j.getString("id")).put("type", "response").put("payload", payload).toString())
    }
    @Test fun powerOffUsesCorrectEndpointExactlyOnce() = runBlocking {
        withTv({ ws, j -> response(ws, j, JSONObject().put("returnValue", true)) }) { client, seen ->
            assertTrue(client.turnOff().getBoolean("returnValue"))
            assertEquals(1, seen.size)
            assertEquals("ssap://system/turnOff", seen.single().getString("uri"))
        }
    }
    @Test fun shutdownDisconnectIsNotMisrepresentedAsAcknowledgement() = runBlocking {
        withTv({ ws, _ -> ws.close(1001, "Standby") }) { client, seen ->
            val result = client.turnOff()
            assertTrue(result.getBoolean("commandSent"))
            assertTrue(result.getBoolean("disconnected"))
            assertFalse(result.has("returnValue"))
            assertFalse(client.isOpen)
            assertEquals(1, seen.size)
        }
    }
    @Test fun rejectedPowerPermissionIsStillAnError() = runBlocking {
        withTv({ ws, j -> response(ws, j, JSONObject().put("returnValue", false)) }) { client, _ ->
            try { client.turnOff(); fail("Expected rejection") }
            catch (e: TvException) { assertEquals(ErrorKind.COMMAND, e.kind) }
        }
    }
    @Test fun wakeAddressDiscoveryUsesNetworkInfoEndpoint() = runBlocking {
        withTv({ ws, j -> response(ws, j, JSONObject("""{"returnValue":true,"wifiInfo":{"macAddress":"02:11:22:33:44:55"}}""")) }) { client, seen ->
            assertEquals(listOf("02:11:22:33:44:55"), client.wakeAddresses())
            assertEquals("ssap://com.webos.service.connectionmanager/getinfo", seen.single().getString("uri"))
        }
    }
    @Test fun offlinePowerOffDoesNotPretendCommandWasSent() = runBlocking {
        val client = WebOsSession(TvConfig("192.168.1.20", secure = false), true)
        try {
            try { client.turnOff(); fail("Expected offline failure") }
            catch (e: TvException) { assertEquals(ErrorKind.NETWORK, e.kind) }
        } finally { client.close() }
    }
}
