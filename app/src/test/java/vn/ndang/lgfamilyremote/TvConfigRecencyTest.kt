package vn.ndang.lgfamilyremote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TvConfigRecencyTest {
    @Test
    fun lastConnectedTimeRoundTrips() {
        val original = TvConfig(
            host = "192.168.1.20",
            uid = "uuid:tv-1",
            lastConnectedAtEpochMs = 123456789L
        )
        assertEquals(123456789L, TvConfig.fromJson(original.toJson()).lastConnectedAtEpochMs)
    }

    @Test
    fun oldSavedConfigDefaultsLastConnectedTimeToZero() {
        val old = JSONObject()
            .put("host", "192.168.1.20")
            .put("name", "Tivi")
        assertEquals(0L, TvConfig.fromJson(old).lastConnectedAtEpochMs)
    }
}
