package vn.ndang.lgfamilyremote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.ndang.lgfamilyremote.network.Protocol

class InputModesTest {
    @Test
    fun keyboardFocusIsReadFromCurrentWidget() {
        val focused = JSONObject()
            .put("currentWidget", JSONObject().put("focus", true))
        val blurred = JSONObject()
            .put("currentWidget", JSONObject().put("focus", false))

        assertTrue(Protocol.keyboardFocused(focused))
        assertFalse(Protocol.keyboardFocused(blurred))
        assertFalse(Protocol.keyboardFocused(JSONObject()))
    }

    @Test
    fun pointerFramesMatchWebOsMagicRemoteFormat() {
        assertEquals(
            "type:move\ndx:12.5\ndy:-4.0\ndown:0\n\n",
            Protocol.pointerMove(12.5f, -4f)
        )
        assertEquals("type:click\n\n", Protocol.pointerClick())
    }
}
