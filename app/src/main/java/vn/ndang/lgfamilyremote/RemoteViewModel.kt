package vn.ndang.lgfamilyremote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import vn.ndang.lgfamilyremote.data.TvStore
import vn.ndang.lgfamilyremote.network.*

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TvStore(application)
    private val discovery = TvDiscovery(application)
    private val wakeOnLan = WakeOnLan(application)
    private val mutableState = MutableStateFlow(RemoteState())
    val state = mutableState.asStateFlow()
    private var foreground = false
    @Volatile private var generation = 0
    private var session: WebOsSession? = null
    private var connectionJob: Job? = null
    private var scanJob: Job? = null
    private var commandJob: Job? = null
    private var infoJob: Job? = null

    private data class RemoteTask(
        val generation: Int,
        val block: suspend (WebOsSession) -> Unit
    )

    // Keep memory/request pressure bounded under rapid tapping.
    // DROP_OLDEST favors what the user is doing now instead of replaying a long stale backlog.
    private val remoteQueue = Channel<RemoteTask>(
        capacity = 12,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private sealed interface PointerTask {
        val generation: Int
        data class Move(override val generation: Int, val dx: Float, val dy: Float) : PointerTask
        data class Click(override val generation: Int) : PointerTask
    }

    private val pointerQueue = Channel<PointerTask>(
        capacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var powerOffRequested = false
    private var pendingVoiceQuery: String? = null

    init {
        viewModelScope.launch {
            for (task in remoteQueue) {
                if (task.generation != generation || state.value.busy) continue
                val current = session
                if (current == null || !current.isOpen) continue
                try {
                    task.block(current)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (task.generation == generation) {
                        mutableState.update { it.copy(error =
                            (e as? TvException)?.message ?: "Chưa gửi được lệnh. Hãy kiểm tra kết nối tivi.") }
                    }
                }
            }
        }
        viewModelScope.launch {
            for (task in pointerQueue) {
                if (task.generation != generation || state.value.busy) continue
                val current = session
                if (current == null || !current.isOpen) continue
                try {
                    when (task) {
                        is PointerTask.Move -> current.pointerMove(task.dx, task.dy)
                        is PointerTask.Click -> current.pointerClick()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (task.generation == generation) {
                        mutableState.update { it.copy(error =
                            (e as? TvException)?.message ?: "Chưa điều khiển được con trỏ.") }
                    }
                }
            }
        }
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { store.load() }
                mutableState.update { it.copy(loaded = true, tv = saved) }
                if (foreground && saved != null) connect(saved, false)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                mutableState.update { it.copy(loaded = true,
                    error = "Không đọc được kết nối đã lưu. Hãy thiết lập lại tivi.") }
            }
        }
    }
    fun onStart() {
        if (foreground) return
        foreground = true
        val current = state.value.tv
        if (state.value.loaded && current != null && current.clientKey.isNotBlank()) connect(current, false)
    }
    fun onStop() {
        foreground = false
        stopConnection()
        scanJob?.cancel()
        mutableState.update { it.copy(connection = ConnectionState.OFFLINE, busy = false, notice = null,
            pairing = null, pinSubmitting = false, pinError = null, textInputFocused = false,
            status = if (it.tv == null) "Chưa kết nối tivi" else "Sẽ tự kết nối khi mở app") }
    }
    private fun stopConnection() {
        generation++
        commandJob?.cancel()
        drainRemoteQueue()
        drainPointerQueue()
        infoJob?.cancel()
        connectionJob?.cancel()
        session?.close()
        session = null
    }
    private fun drainRemoteQueue() {
        while (remoteQueue.tryReceive().isSuccess) {
            // Discard stale taps from the previous connection/session.
        }
    }

    private fun drainPointerQueue() {
        while (pointerQueue.tryReceive().isSuccess) {
            // Discard stale pointer motion from the previous connection/session.
        }
    }

    fun clearError() { mutableState.update { it.copy(error = null) } }
    fun clearNotice() { mutableState.update { it.copy(notice = null) } }
    fun search() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            mutableState.update { it.copy(searching = true, found = emptyList(), error = null) }
            try {
                val results = discovery.scan()
                mutableState.update { it.copy(found = results,
                    error = if (results.isEmpty()) "Chưa tìm thấy tivi. Kiểm tra cùng mạng nhà hoặc nhập IP bên dưới." else null) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                mutableState.update { it.copy(error = "Không tìm được tivi qua mạng. Bạn vẫn có thể nhập IP thủ công.") }
            } finally { mutableState.update { it.copy(searching = false) } }
        }
    }
    fun pair(target: TvConfig) {
        try { LanRules.requireHost(target.host) }
        catch (e: TvException) { mutableState.update { it.copy(error = e.message) }; return }
        val saved = state.value.tv
        val same = saved != null && target.secure == saved.secure &&
            (Protocol.sameDeviceUid(target.uid, saved.uid) || target.host == saved.host)
        val selected = if (same) saved!!.copy(host = target.host, name = target.name,
            uid = target.uid.ifBlank { saved.uid }, youtubeId = target.youtubeId.ifBlank { saved.youtubeId }) else target
        connect(selected, true)
    }
    fun retry() { state.value.tv?.let { connect(it, it.clientKey.isBlank()) } }
    fun repair() {
        state.value.tv?.let { connect(it.copy(clientKey = "", certificateSha256 = ""), true) }
    }
    fun cancelPairing() {
        stopConnection()
        mutableState.update { it.copy(connection = ConnectionState.OFFLINE, busy = false,
            pairing = null, pinSubmitting = false, pinError = null,
            status = "Đã dừng kết nối. Bấm Thử lại khi sẵn sàng.") }
    }
    fun forget() {
        pendingVoiceQuery = null
        stopConnection()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { store.clear() }
                mutableState.value = RemoteState(loaded = true)
            } catch (_: Exception) {
                mutableState.update { it.copy(error = "Chưa xóa được kết nối. Hãy thử lại.") }
            }
        }
    }
    fun saveOptions(name: String, youtubeId: String) {
        val tv = state.value.tv ?: return
        val updated = tv.copy(name = name.trim().take(80).ifBlank { "Tivi nhà mình" },
            youtubeId = youtubeId.trim().take(160))
        mutableState.update { it.copy(tv = updated) }
        viewModelScope.launch { if (updated.clientKey.isNotBlank()) save(updated) }
    }
    fun saveWakeMacs(raw: String) {
        val tv = state.value.tv ?: return
        val addresses = try { WakeProtocol.parseMacs(raw) }
        catch (e: IllegalArgumentException) { mutableState.update { it.copy(error = e.message) }; return }
        val updated = tv.copy(wakeMacs = addresses)
        mutableState.update { it.copy(tv = updated, error = null) }
        viewModelScope.launch {
            if (updated.clientKey.isNotBlank()) save(updated)
            mutableState.update { it.copy(notice = "Đã lưu tùy chọn bật tivi.") }
        }
    }
    private suspend fun save(tv: TvConfig) {
        try { withContext(Dispatchers.IO) { store.save(tv) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            mutableState.update { it.copy(error = "Đã kết nối nhưng chưa lưu được. Lần sau có thể cần ghép đôi lại.") }
        }
    }
    private suspend fun learnWakeAddresses(current: WebOsSession, token: Int, quiet: Boolean) {
        try {
            val addresses = current.wakeAddresses()
            if (generation != token) return
            if (addresses.isEmpty()) throw TvException(ErrorKind.COMMAND,
                "Tivi chưa cung cấp địa chỉ MAC. Bạn có thể nhập MAC trong tùy chọn bật tivi.")
            val tv = state.value.tv ?: return
            val updated = tv.copy(wakeMacs = (tv.wakeMacs + addresses).distinct().take(4))
            mutableState.update { it.copy(tv = updated) }
            save(updated)
            if (!quiet) mutableState.update { it.copy(notice = "Đã lưu ${updated.wakeMacs.size} địa chỉ MAC để bật lại tivi.") }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (!quiet) throw TvException(ErrorKind.COMMAND,
                "Chưa đọc được MAC từ tivi. Nhập MAC thủ công trong Cài đặt, hoặc ghép đôi lại để cấp quyền mới.", e)
        }
    }
    fun readWakeAddresses() = command { learnWakeAddresses(it, generation, false) }

    fun voiceSearch(raw: String) {
        val query = raw.trim().replace(Regex("\\s+"), " ").take(200)
        if (query.isBlank()) return
        pendingVoiceQuery = query
        if (state.value.connection == ConnectionState.CONNECTED && session?.isOpen == true && !state.value.busy) {
            pendingVoiceQuery = null
            remoteCommand { it.launchYouTubeSearch(query, state.value.tv?.youtubeId.orEmpty()) }
        } else if (foreground && state.value.tv != null &&
            state.value.connection in listOf(ConnectionState.IDLE, ConnectionState.OFFLINE)) {
            retry()
        }
    }

    fun replaceTextInput(text: String) {
        val value = text.take(500)
        remoteCommand { it.replaceText(value) }
    }

    fun textInputEnter() = remoteCommand { it.sendTextEnter() }

    fun pointerMove(dx: Float, dy: Float) {
        if (state.value.connection != ConnectionState.CONNECTED || state.value.busy) return
        val x = dx.coerceIn(-120f, 120f)
        val y = dy.coerceIn(-120f, 120f)
        pointerQueue.trySend(PointerTask.Move(generation, x, y))
    }

    fun pointerClick() {
        if (state.value.connection != ConnectionState.CONNECTED || state.value.busy) return
        pointerQueue.trySend(PointerTask.Click(generation))
    }

    fun submitPin(pin: String) {
        val current = session ?: return
        if (state.value.connection != ConnectionState.PAIRING || state.value.pairing != PairingKind.PIN ||
            state.value.pinSubmitting) return
        val token = generation
        mutableState.update { it.copy(pinSubmitting = true, pinError = null) }
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            try {
                current.submitPin(pin)
                if (generation == token) mutableState.update { it.copy(status = "Đang hoàn tất kết nối…") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generation == token) mutableState.update { it.copy(pinError =
                    (e as? TvException)?.message ?: "Chưa gửi được mã. Hãy thử lại.") }
            } finally {
                if (generation == token) mutableState.update { it.copy(pinSubmitting = false) }
            }
        }
    }

    private fun connect(initial: TvConfig, allowPairing: Boolean, wakingUp: Boolean = false) {
        stopConnection()
        powerOffRequested = false
        val token = generation
        val wakeDeadlineNanos = if (wakingUp) System.nanoTime() + 90_000_000_000L else Long.MAX_VALUE
        mutableState.update { it.copy(
            tv = initial,
            error = null,
            busy = false,
            volume = VolumeState(),
            pairing = null,
            pinSubmitting = false,
            pinError = null,
            textInputFocused = false,
            connection = if (wakingUp) ConnectionState.CONNECTING else it.connection,
            status = if (wakingUp) "Đang chờ tivi bật…" else it.status
        ) }
        connectionJob = viewModelScope.launch {
            var target = initial
            var pairingAllowed = allowPairing
            var attempt = 0
            while (isActive && foreground && generation == token) {
                target = state.value.tv ?: target
                mutableState.update { it.copy(
                    connection = ConnectionState.CONNECTING,
                    status = if (wakingUp) "Đang chờ tivi bật…"
                    else if (attempt == 0) "Đang kết nối tivi…" else "Đang thử kết nối lại…"
                ) }
                val builder = OkHttpClient.Builder()
                localNetwork(getApplication())?.let { builder.socketFactory(it.socketFactory) }
                val current = WebOsSession(target, pairingAllowed, builder,
                    onPairing = { kind ->
                        if (generation == token) mutableState.update { it.copy(
                            connection = ConnectionState.PAIRING, pairing = kind,
                            pinSubmitting = false, pinError = null,
                            status = if (kind == PairingKind.PIN) "Nhập mã đang hiện trên tivi"
                                else "Hãy chọn Cho phép trên màn hình tivi") }
                    },
                    onVolume = { value ->
                        if (generation == token) mutableState.update { it.copy(volume = VolumeState(
                            value.level ?: it.volume.level, value.muted ?: it.volume.muted)) }
                    },
                    onKeyboardFocus = { focused ->
                        if (generation == token) mutableState.update { it.copy(textInputFocused = focused) }
                    })
                session = current
                var retryable = false
                var rediscoverAfterFailure = false
                try {
                    target = current.connect(
                        registrationTimeoutMs = if (wakingUp && !pairingAllowed) 4_500L else null
                    )
                    ensureActive()
                    save(target)
                    pairingAllowed = false
                    attempt = 0
                    mutableState.update { it.copy(tv = target, connection = ConnectionState.CONNECTED,
                        status = "Đã kết nối", volume = VolumeState(), pairing = null,
                        pinSubmitting = false, pinError = null, textInputFocused = false) }
                    current.subscribeVolume()
                    current.subscribeKeyboard()
                    infoJob = launch { learnWakeAddresses(current, token, true) }
                    pendingVoiceQuery?.let { query ->
                        pendingVoiceQuery = null
                        remoteQueue.trySend(RemoteTask(token) {
                            it.launchYouTubeSearch(query, state.value.tv?.youtubeId.orEmpty())
                        })
                    }
                    throw current.awaitClosed()
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    if (generation != token) break
                    val problem = e as? TvException
                    val networkFailure = !powerOffRequested && !pairingAllowed &&
                        (problem == null || problem.kind == ErrorKind.NETWORK)
                    val wakeStillWaiting = !wakingUp || System.nanoTime() < wakeDeadlineNanos
                    retryable = networkFailure && wakeStillWaiting
                    rediscoverAfterFailure = retryable && target.uid.isNotBlank() &&
                        (attempt == 0 || attempt % 3 == 2)

                    if (rediscoverAfterFailure) {
                        mutableState.update { it.copy(
                            connection = ConnectionState.CONNECTING,
                            busy = false,
                            pairing = null,
                            pinSubmitting = false,
                            textInputFocused = false,
                            status = "Đang tìm lại tivi trong mạng nhà…"
                        ) }
                    } else if (wakingUp && retryable) {
                        // Do not flash gray/offline while the TV is still booting.
                        mutableState.update { it.copy(
                            connection = ConnectionState.CONNECTING,
                            busy = false,
                            pairing = null,
                            pinSubmitting = false,
                            textInputFocused = false,
                            status = "Đang chờ tivi bật…"
                        ) }
                    } else {
                        mutableState.update { it.copy(
                            connection = ConnectionState.OFFLINE,
                            busy = false,
                            pairing = null,
                            pinSubmitting = false,
                            textInputFocused = false,
                            status = when {
                                powerOffRequested -> "Tivi đã ngắt kết nối sau lệnh nguồn."
                                wakingUp -> "Chưa kết nối lại được sau khi chờ tivi bật."
                                else -> problem?.message ?: "Chưa kết nối được tivi. Kiểm tra mạng nhà."
                            }
                        ) }
                    }
                } finally {
                    infoJob?.cancel()
                    current.close()
                    if (session === current) session = null
                }
                if (!retryable) break

                if (rediscoverAfterFailure && generation == token && foreground) {
                    try {
                        val resolvedHost = discovery.findHostByUid(target.uid)
                        if (resolvedHost != null && generation == token) {
                            val hostChanged = resolvedHost != target.host
                            target = target.copy(host = resolvedHost)
                            mutableState.update { it.copy(
                                tv = target,
                                connection = ConnectionState.CONNECTING,
                                status = if (hostChanged)
                                    "Đã tìm thấy tivi ở địa chỉ mới. Đang kết nối…"
                                else if (wakingUp) "Đã thấy tivi. Đang chờ khởi động…"
                                else "Đã tìm thấy tivi. Đang kết nối lại…"
                            ) }
                            if (hostChanged) save(target)
                            attempt = 0
                            continue
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Discovery is best-effort; fall back to the normal reconnect loop.
                    }

                    if (generation == token && !wakingUp) {
                        mutableState.update { it.copy(
                            connection = ConnectionState.OFFLINE,
                            status = "Chưa tìm thấy tivi. App sẽ tự thử lại."
                        ) }
                    }
                }

                attempt++
                val retryDelay = if (wakingUp) {
                    listOf(1500L, 2000L, 2500L, 3000L)[(attempt - 1).coerceAtMost(3)]
                } else {
                    listOf(4000L, 8000L, 15000L, 30000L)[(attempt - 1).coerceAtMost(3)]
                }
                delay(retryDelay)
            }
        }
    }
    /**
     * Everyday remote presses never toggle RemoteState.busy, so the UI stays visually stable.
     * A single bounded worker serializes commands and prevents rapid tapping from creating
     * an unbounded number of coroutines / in-flight WebSocket requests.
     */
    private fun remoteCommand(block: suspend (WebOsSession) -> Unit) {
        val current = session
        if (current == null || !current.isOpen || state.value.busy) return
        remoteQueue.trySend(RemoteTask(generation, block))
    }

    /** Blocking operations are rare (power/setup) and may disable controls briefly. */
    private fun command(block: suspend (WebOsSession) -> Unit) {
        val current = session
        if (current == null || !current.isOpen || state.value.busy) return
        val token = generation
        mutableState.update { it.copy(busy = true, error = null) }
        commandJob = viewModelScope.launch {
            try { block(current) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generation == token) mutableState.update { it.copy(error =
                    (e as? TvException)?.message ?: "Chưa gửi được lệnh. Hãy kiểm tra kết nối tivi.") }
            } finally {
                if (generation == token) mutableState.update { it.copy(busy = false) }
            }
        }
    }
    fun key(name: String) = remoteCommand { it.button(name) }
    fun volume(up: Boolean) = remoteCommand { it.changeVolume(up) }
    fun mute() = remoteCommand { it.toggleMute() }
    fun youtube() = remoteCommand { it.launchYouTube(state.value.tv?.youtubeId.orEmpty()) }

    fun powerToggle() {
        drainRemoteQueue()
        when (powerActionFor(state.value.connection, state.value.busy)) {
            PowerAction.TURN_OFF -> powerOff()
            PowerAction.TURN_ON -> powerOn()
            PowerAction.NONE -> Unit
        }
    }

    fun powerOff() = command { current ->
        powerOffRequested = true
        try {
            if (state.value.tv?.wakeMacs.isNullOrEmpty()) {
                learnWakeAddresses(current, generation, true)
            }
            if (state.value.tv?.wakeMacs.isNullOrEmpty()) {
                powerOffRequested = false
                throw TvException(
                    ErrorKind.COMMAND,
                    "App chưa lấy được địa chỉ MAC của tivi nên chưa tắt để tránh không bật lại được. Hãy ghép đôi lại hoặc nhập MAC trong Cài đặt."
                )
            }
            val acknowledged = current.requestPowerOff()
            connectionJob?.cancel()
            infoJob?.cancel()
            current.close()
            if (session === current) session = null
            mutableState.update { it.copy(connection = ConnectionState.OFFLINE, busy = false,
                status = if (acknowledged) "Tivi đã nhận yêu cầu tắt."
                    else "Tivi đã ngắt kết nối; hãy kiểm tra màn hình để xác nhận đã tắt.",
                notice = null) }
        } catch (e: CancellationException) {
            powerOffRequested = false
            throw e
        } catch (e: Exception) {
            powerOffRequested = false
            if (e is TvException && e.kind == ErrorKind.COMMAND) throw TvException(ErrorKind.COMMAND,
                "Tivi chưa thực hiện lệnh tắt. Thử Cài đặt → Nhập IP / tùy chọn → Ghép đôi lại để cấp quyền nguồn.", e)
            throw e
        }
    }
    fun powerOn() {
        val tv = state.value.tv ?: return
        if (state.value.busy || state.value.connection == ConnectionState.PAIRING) return
        val token = generation
        mutableState.update { it.copy(busy = true, error = null) }
        commandJob = viewModelScope.launch {
            var reconnect = false
            try {
                wakeOnLan.send(tv)
                if (generation == token) {
                    mutableState.update { it.copy(notice = null) }
                    reconnect = state.value.connection != ConnectionState.CONNECTED
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generation == token) mutableState.update { it.copy(error =
                    (e as? TvException)?.message ?: "Chưa gửi được lệnh bật. Kiểm tra mạng nhà và TV On With Mobile trên tivi.") }
            } finally {
                if (generation == token) mutableState.update { it.copy(busy = false) }
            }
            if (reconnect && foreground && generation == token) {
                commandJob = null
                val latest = state.value.tv ?: tv
                val needsPairing = latest.clientKey.isBlank() ||
                    (latest.secure && latest.certificateSha256.isBlank())
                connect(latest, needsPairing, wakingUp = !needsPairing)
            }
        }
    }
    override fun onCleared() {
        stopConnection()
        super.onCleared()
    }
}
