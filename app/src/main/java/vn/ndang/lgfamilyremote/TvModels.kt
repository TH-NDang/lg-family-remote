package vn.ndang.lgfamilyremote

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Deliberately excludes credentials from toString(). */
data class TvConfig(
    val host: String,
    val name: String = "Tivi nhà mình",
    val uid: String = "",
    val secure: Boolean = true,
    val clientKey: String = "",
    val certificateSha256: String = "",
    val youtubeId: String = "",
    val wakeMacs: List<String> = emptyList()
) {
    override fun toString(): String = "$name ($host)"
    fun toJson(): JSONObject = JSONObject().put("host", host).put("name", name)
        .put("uid", uid).put("secure", secure).put("clientKey", clientKey)
        .put("certificateSha256", certificateSha256).put("youtubeId", youtubeId)
        .put("wakeMacs", JSONArray(wakeMacs))
    companion object {
        fun fromJson(j: JSONObject): TvConfig = TvConfig(
            host = j.getString("host"), name = j.optString("name", "Tivi nhà mình"),
            uid = j.optString("uid"), secure = j.optBoolean("secure", true),
            clientKey = j.optString("clientKey"), certificateSha256 = j.optString("certificateSha256"),
            youtubeId = j.optString("youtubeId"),
            wakeMacs = j.optJSONArray("wakeMacs")?.let { a ->
                (0 until a.length().coerceAtMost(4)).map { a.optString(it) }.filter { it.isNotBlank() }
            }.orEmpty()
        )
    }
}
data class VolumeState(val level: Int? = null, val muted: Boolean? = null)
enum class ErrorKind { NETWORK, PAIRING_REQUIRED, REJECTED, CERTIFICATE, COMMAND, PROTOCOL }
class TvException(val kind: ErrorKind, message: String, cause: Throwable? = null) : IOException(message, cause)
enum class ConnectionState { IDLE, CONNECTING, PAIRING, CONNECTED, OFFLINE }
data class RemoteState(
    val loaded: Boolean = false,
    val tv: TvConfig? = null,
    val connection: ConnectionState = ConnectionState.IDLE,
    val status: String = "Chưa kết nối tivi",
    val error: String? = null,
    val found: List<TvConfig> = emptyList(),
    val searching: Boolean = false,
    val volume: VolumeState = VolumeState(),
    val busy: Boolean = false,
    val notice: String? = null
)
