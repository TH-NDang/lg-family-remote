package vn.ndang.lgfamilyremote.network

import org.json.JSONArray
import org.json.JSONObject
import vn.ndang.lgfamilyremote.*
import java.net.URI

object LanRules {
    fun isPrivateIpv4(host: String): Boolean {
        if (!host.matches(Regex("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))) return false
        val p = host.split('.').map { it.toIntOrNull() ?: return false }
        if (p.any { it !in 0..255 }) return false
        if (host.split('.').any { it.length > 1 && it.startsWith('0') }) return false
        return p[0] == 10 || (p[0] == 172 && p[1] in 16..31) ||
            (p[0] == 192 && p[1] == 168) || (p[0] == 169 && p[1] == 254)
    }
    fun requireHost(host: String) {
        if (!isPrivateIpv4(host)) throw TvException(ErrorKind.NETWORK,
            "Hãy nhập IP nội bộ của tivi, ví dụ 192.168.1.20. Không nhập đường dẫn trang web.")
    }
    fun pointerUrl(raw: String, tv: TvConfig): String {
        val u = runCatching { URI(raw) }.getOrNull()
            ?: throw TvException(ErrorKind.PROTOCOL, "Tivi trả về địa chỉ điều khiển không hợp lệ.")
        if (u.host != tv.host || u.scheme !in listOf("ws", "wss") || u.userInfo != null ||
            u.port !in listOf(3000, 3001) || u.fragment != null) {
            throw TvException(ErrorKind.PROTOCOL, "Địa chỉ điều khiển không khớp tivi đã chọn.")
        }
        // Some firmware advertises ws:3000 even over the secure main socket.
        // Upgrade the same-TV endpoint; never silently downgrade a secure session.
        if (tv.secure) return URI("wss", null, tv.host, 3001, u.path, u.query, null).toString()
        if (u.scheme != "ws") throw TvException(ErrorKind.PROTOCOL, "Hãy chọn kết nối bảo mật cho tivi này.")
        return raw
    }
    fun descriptionAllowed(raw: String, sender: String): Boolean {
        val u = runCatching { URI(raw) }.getOrNull() ?: return false
        return isPrivateIpv4(sender) && u.scheme == "http" && u.host == sender &&
            u.userInfo == null && u.fragment == null
    }
}

object Protocol {
    private val permissions = listOf("LAUNCH", "CONTROL_AUDIO", "CONTROL_INPUT_JOYSTICK",
        "CONTROL_MOUSE_AND_KEYBOARD", "READ_INSTALLED_APPS", "READ_RUNNING_APPS", "READ_APP_STATUS",
        "CONTROL_POWER", "READ_NETWORK_STATE")
    fun register(tv: TvConfig, pairing: PairingKind = PairingKind.PROMPT): String {
        val manifest = JSONObject().put("manifestVersion", 1).put("appVersion", "1.0")
            .put("appId", "vn.ndang.lgfamilyremote")
            .put("localizedAppNames", JSONObject().put("", "Dieu khien TV"))
            .put("permissions", JSONArray(permissions))
        val payload = JSONObject().put("forcePairing", false)
            .put("pairingType", if (pairing == PairingKind.PIN) "PIN" else "PROMPT")
            .put("manifest", manifest)
        if (tv.clientKey.isNotBlank()) payload.put("client-key", tv.clientKey)
        return JSONObject().put("id", "register").put("type", "register").put("payload", payload).toString()
    }
    fun request(id: String, uri: String, payload: JSONObject = JSONObject(), subscribe: Boolean = false): String =
        JSONObject().put("id", id).put("type", if (subscribe) "subscribe" else "request")
            .put("uri", "ssap://$uri").put("payload", payload).toString()
    fun pinRequest(id: String, pin: String): String =
        request(id, "pairing/setPin", JSONObject().put("pin", pin))
    fun normalizePin(raw: String): String? {
        val pin = raw.trim()
        return pin.takeIf { it.length in 4..12 && it.all { ch -> ch in '0'..'9' } }
    }
    fun button(key: String): String {
        require(key in setOf("UP", "DOWN", "LEFT", "RIGHT", "ENTER", "HOME", "BACK", "MUTE"))
        return "type:button\nname:$key\n\n"
    }
    fun isError(message: JSONObject): Boolean = message.optString("type") == "error" ||
        message.optJSONObject("payload")?.let { it.has("returnValue") && !it.optBoolean("returnValue") } == true
    fun volume(payload: JSONObject): VolumeState {
        val p = payload.optJSONObject("volumeStatus") ?: payload
        val muteKey = if (p.has("muteStatus")) "muteStatus" else "muted"
        return VolumeState(
            if (p.has("volume") && !p.isNull("volume")) p.optInt("volume").takeIf { it in 0..100 } else null,
            if (p.has(muteKey) && !p.isNull(muteKey)) p.optBoolean(muteKey) else null
        )
    }
    fun youtubeApp(payload: JSONObject): String? {
        val apps = payload.optJSONArray("apps") ?: return null
        val candidates = (0 until apps.length()).mapNotNull { apps.optJSONObject(it) }
        return candidates.firstOrNull {
            it.optString("id") in listOf("youtube.leanback.v4", "youtube")
        }?.optString("id") ?: candidates.firstOrNull {
            val title = it.optString("title", it.optString("name"))
            title.contains("youtube", true) && !title.contains("kids", true)
        }?.optString("id")?.takeIf { it.isNotBlank() }
    }
    fun ssdpHeaders(packet: String): Map<String, String> = packet.lineSequence().drop(1)
        .mapNotNull { line ->
            val i = line.indexOf(':')
            if (i <= 0) null else line.substring(0, i).trim().lowercase() to line.substring(i + 1).trim()
        }.toMap()
}
