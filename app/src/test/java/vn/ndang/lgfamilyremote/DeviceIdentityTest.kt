package vn.ndang.lgfamilyremote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.ndang.lgfamilyremote.network.Protocol

class DeviceIdentityTest {
    @Test
    fun normalizesCommonLgSsdpUidFormats() {
        assertEquals("1234-abcd", Protocol.deviceUidKey("uuid:1234-ABCD"))
        assertEquals("1234-abcd", Protocol.deviceUidKey("urn:uuid:1234-ABCD"))
        assertEquals(
            "1234-abcd",
            Protocol.deviceUidKey("uuid:1234-ABCD::urn:lge-com:service:webos-second-screen:1")
        )
        assertEquals("1234-abcd", Protocol.deviceUidKey("{1234-ABCD}"))
    }

    @Test
    fun recognizesSameTvAcrossUidSpellings() {
        assertTrue(
            Protocol.sameDeviceUid(
                "uuid:ABCDEF01-2345",
                "urn:uuid:abcdef01-2345::urn:lge-com:service:webos-second-screen:1"
            )
        )
        assertFalse(Protocol.sameDeviceUid("", "uuid:abcdef"))
        assertFalse(Protocol.sameDeviceUid("uuid:first", "uuid:second"))
    }
}
