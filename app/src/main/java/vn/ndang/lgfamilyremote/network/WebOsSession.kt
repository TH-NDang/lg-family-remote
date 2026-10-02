package vn.ndang.lgfamilyremote.network

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.json.JSONObject
import vn.ndang.lgfamilyremote.*
import java.io.IOException
import java.security.cert.CertificateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLPeerUnverifiedException

/** Session-owned sockets: stale callbacks cannot affect a replacement session. */
class WebOsSession internal constructor(
    private val tv: TvConfig,
    private val allowPairing: Boolean,
    clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
    private val onPairing: (PairingKind) -> Unit = {},
    private val onVolume: (VolumeState) -> Unit = {},
    private val pairingPreference: PairingKind = PairingKind.PIN,
    private val endpointOverrideForTests: String? = null
) {
    private val trust = CertificateTrust(tv.certificateSha256, allowPairing)
    private val client: OkHttpClient
    private val registration = CompletableDeferred<TvConfig>()
    private val closed = CompletableDeferred<Throwable>()
    private val pointerLock = Mutex()
    private val pinLock = Mutex()
    private val sequence = AtomicInteger()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var pointer: WebSocket? = null
    @Volatile private var registered = false
    val isOpen: Boolean get() = registered && !closed.isCompleted

    init {
        if (tv.secure) {
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf(trust), null)
            clientBuilder.sslSocketFactory(context.socketFactory, trust)
                .hostnameVerifier { host, _ -> host == tv.host }
        }
        client = clientBuilder.connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).build()
    }

    suspend fun connect(registrationTimeoutMs: Long? = null): TvConfig {
        if (endpointOverrideForTests == null) LanRules.requireHost(tv.host)
        if (!allowPairing && (tv.clientKey.isBlank() || (tv.secure && tv.certificateSha256.isBlank()))) {
            throw TvException(ErrorKind.PAIRING_REQUIRED, "Cần xác nhận kết nối lại trên tivi.")
        }
        val endpoint = endpointOverrideForTests ?: "${if (tv.secure) "wss" else "ws"}://${tv.host}:${if (tv.secure) 3001 else 3000}"
        socket = client.newWebSocket(Request.Builder().url(endpoint).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (closed.isCompleted) { webSocket.cancel(); return }
                val requested = if (allowPairing && tv.clientKey.isBlank()) pairingPreference else PairingKind.PROMPT
                if (!webSocket.send(Protocol.register(tv, requested))) fail(IOException("Registration send failed"))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (closed.isCompleted) return
                if (text.length > 1_000_000) { fail(IOException("Response too large")); return }
                try { handle(JSONObject(text)) } catch (_: Exception) {
                    fail(TvException(ErrorKind.PROTOCOL, "Tivi trả về dữ liệu không hợp lệ."))
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { fail(t) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { fail(IOException("TV disconnected")) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { fail(IOException("TV disconnected")) }
        })
        try {
            val timeoutMs = if (allowPairing) 60_000L else registrationTimeoutMs ?: 12_000L
            return withTimeout(timeoutMs.coerceIn(1_000L, 60_000L)) { registration.await() }
        } catch (e: TimeoutCancellationException) {
            val error = TvException(if (allowPairing) ErrorKind.REJECTED else ErrorKind.NETWORK,
                if (allowPairing) "Chưa ghép đôi được. Kiểm tra tivi đã bật và chọn Cho phép trên tivi."
                else "Chưa kết nối được. Kiểm tra tivi đã bật và điện thoại đang dùng mạng nhà.")
            fail(error)
            throw error
        } catch (e: CancellationException) { close(); throw e }
    }

    private fun handle(message: JSONObject) {
        val id = message.optString("id")
        val payload = message.optJSONObject("payload") ?: JSONObject()
        if (id == "register" || message.optString("type") == "registered") {
            if (Protocol.isError(message)) {
                fail(TvException(ErrorKind.REJECTED,
                    "Tivi chưa cấp quyền. Mở Cài đặt, ghép đôi lại và chọn Cho phép trên tivi."))
            } else if (message.optString("type") == "registered") {
                val key = payload.optString("client-key").ifBlank { tv.clientKey }
                if (key.isBlank()) {
                    fail(TvException(ErrorKind.PROTOCOL, "Tivi chưa trả thông tin ghép đôi.")); return
                }
                registered = true
                registration.complete(tv.copy(clientKey = key,
                    certificateSha256 = if (tv.secure) trust.observedPin else ""))
            } else if (payload.has("pairingType")) {
                val kind = if (payload.optString("pairingType").equals("PIN", true)) PairingKind.PIN else PairingKind.PROMPT
                if (allowPairing) onPairing(kind)
                else fail(TvException(ErrorKind.PAIRING_REQUIRED, "Tivi yêu cầu xác nhận lại. Mở Cài đặt để ghép đôi."))
            }
            return
        }
        if (id == "volume") {
            if (!Protocol.isError(message)) onVolume(Protocol.volume(payload))
            return
        }
        val waiting = pending.remove(id) ?: return
        if (Protocol.isError(message)) waiting.completeExceptionally(TvException(ErrorKind.COMMAND,
            "Tivi không thực hiện được lệnh này. Có thể model hoặc ứng dụng chưa hỗ trợ."))
        else waiting.complete(payload)
    }

    suspend fun submitPin(raw: String) = pinLock.withLock {
        val pin = Protocol.normalizePin(raw) ?: throw TvException(ErrorKind.REJECTED,
            "Mã ghép đôi chưa đúng định dạng. Hãy nhập đúng các chữ số đang hiện trên tivi.")
        if (!allowPairing || registered || closed.isCompleted) throw TvException(ErrorKind.PAIRING_REQUIRED,
            "Tivi hiện không chờ mã ghép đôi. Hãy bắt đầu ghép đôi lại.")
        val id = "pin${sequence.incrementAndGet()}"
        val result = CompletableDeferred<JSONObject>()
        pending[id] = result
        try {
            if (socket?.send(Protocol.pinRequest(id, pin)) != true) {
                throw TvException(ErrorKind.NETWORK, "Đã mất kết nối với tivi trước khi gửi mã.")
            }
            try {
                withTimeout(8000) { result.await() }
            } catch (e: TimeoutCancellationException) {
                if (!registered) throw TvException(ErrorKind.REJECTED,
                    "Tivi chưa chấp nhận mã. Kiểm tra mã trên màn hình tivi rồi thử lại.", e)
            }
        } catch (e: TvException) {
            if (e.kind == ErrorKind.COMMAND) throw TvException(ErrorKind.REJECTED,
                "Mã chưa đúng hoặc đã hết hạn. Hãy nhập mã mới đang hiện trên tivi.", e)
            throw e
        } finally {
            pending.remove(id)
            result.cancel()
        }
    }

    suspend fun request(uri: String, payload: JSONObject = JSONObject()): JSONObject = requestInternal(uri, payload)

    private suspend fun requestInternal(uri: String, payload: JSONObject, disconnectExpected: Boolean = false): JSONObject {
        if (!isOpen) throw TvException(ErrorKind.NETWORK, "Tivi chưa kết nối. Hãy bấm Thử lại.")
        val id = "r${sequence.incrementAndGet()}"
        val result = CompletableDeferred<JSONObject>()
        pending[id] = result
        var sent = false
        try {
            if (socket?.send(Protocol.request(id, uri, payload)) != true) {
                throw TvException(ErrorKind.NETWORK, "Đã mất kết nối với tivi.")
            }
            sent = true
            return withTimeout(5000) { result.await() }
        } catch (e: TimeoutCancellationException) {
            throw TvException(ErrorKind.COMMAND, "Tivi chưa phản hồi lệnh. Không gửi lại tự động để tránh bấm lặp.", e)
        } catch (e: TvException) {
            // A TV may close its socket before acknowledging turnOff. This only means
            // the command was queued and the connection closed, NOT verified power-off.
            if (disconnectExpected && sent && e.kind == ErrorKind.NETWORK && !isOpen) {
                return JSONObject().put("commandSent", true).put("disconnected", true)
            }
            throw e
        } finally { pending.remove(id); result.cancel() }
    }

    suspend fun turnOff(): JSONObject = requestInternal("system/turnOff", JSONObject(), disconnectExpected = true)
    suspend fun wakeAddresses(): List<String> = WakeProtocol.fromConnectionInfo(
        request("com.webos.service.connectionmanager/getinfo"))

    fun subscribeVolume() {
        if (isOpen) socket?.send(Protocol.request("volume", "audio/getVolume", subscribe = true))
    }

    private suspend fun pointerSocket(): WebSocket = pointerLock.withLock {
        pointer?.let { return@withLock it }
        val payload = request("com.webos.service.networkinput/getPointerInputSocket")
        val endpoint = LanRules.pointerUrl(payload.optString("socketPath"), tv)
        val ready = CompletableDeferred<WebSocket>()
        val opened = client.newWebSocket(Request.Builder().url(endpoint).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isOpen) { webSocket.cancel(); ready.completeExceptionally(IOException("Closed")); return }
                pointer = webSocket
                ready.complete(webSocket)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (pointer === webSocket) pointer = null
                ready.completeExceptionally(TvException(ErrorKind.COMMAND, "Chưa mở được các phím điều hướng.", t))
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (pointer === webSocket) pointer = null
                webSocket.close(1000, null)
                ready.completeExceptionally(IOException("Pointer closed"))
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (pointer === webSocket) pointer = null
            }
        })
        try { withTimeout(6000) { ready.await() } }
        catch (e: TimeoutCancellationException) {
            opened.cancel()
            throw TvException(ErrorKind.COMMAND, "Chưa mở được các phím điều hướng. Hãy thử lại.", e)
        } catch (e: CancellationException) { opened.cancel(); throw e }
    }

    suspend fun button(key: String) {
        val message = Protocol.button(key)
        val ws = pointerSocket()
        if (!isOpen || !ws.send(message)) throw TvException(ErrorKind.NETWORK, "Đã mất kết nối với tivi.")
    }

    private suspend fun resolveYouTubeId(overrideId: String): String {
        val discovered = if (overrideId.isBlank()) {
            try { Protocol.youtubeApp(request("com.webos.applicationManager/listApps")) }
            catch (e: CancellationException) { throw e }
            catch (_: TvException) { null }
        } else overrideId
        return discovered ?: "youtube.leanback.v4"
    }

    suspend fun launchYouTubeSearch(raw: String, overrideId: String): String {
        val target = Protocol.youtubeSearchUrl(raw)
            ?: throw TvException(ErrorKind.COMMAND, "Chưa nhận được nội dung cần tìm.")
        val id = resolveYouTubeId(overrideId)
        val payload = JSONObject()
            .put("id", id)
            .put("contentId", target)
            .put("params", JSONObject().put("contentTarget", target))
        request("system.launcher/launch", payload)
        return id
    }

    suspend fun launchYouTube(overrideId: String): String {
        val id = resolveYouTubeId(overrideId)
        request("system.launcher/launch", JSONObject().put("id", id))
        return id
    }
    suspend fun changeVolume(up: Boolean) { request(if (up) "audio/volumeUp" else "audio/volumeDown") }
    suspend fun toggleMute() {
        val state = Protocol.volume(request("audio/getVolume"))
        val value = state.muted ?: throw TvException(ErrorKind.COMMAND, "Chưa đọc được trạng thái tắt tiếng của tivi.")
        request("audio/setMute", JSONObject().put("mute", !value))
        onVolume(Protocol.volume(request("audio/getVolume")))
    }
    suspend fun awaitClosed(): Throwable = closed.await()

    private fun fail(t: Throwable) {
        val problem = when {
            t is TvException -> t
            generateSequence(t) { it.cause }.any { it is CertificateException || it is SSLPeerUnverifiedException } ->
                TvException(ErrorKind.CERTIFICATE,
                    "Chứng chỉ tivi không khớp hoặc không còn hợp lệ. Kiểm tra đúng tivi rồi ghép đôi lại trong Cài đặt.", t)
            else -> TvException(ErrorKind.NETWORK,
                "Chưa kết nối được tivi. Kiểm tra tivi đã bật và điện thoại đang dùng mạng nhà.", t)
        }
        if (!closed.complete(problem)) return
        registered = false
        registration.completeExceptionally(problem)
        pending.values.forEach { it.completeExceptionally(problem) }
        pending.clear()
        pointer?.cancel(); pointer = null
        socket?.cancel()
    }
    fun close() {
        fail(TvException(ErrorKind.NETWORK, "Kết nối đã đóng."))
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }
}
