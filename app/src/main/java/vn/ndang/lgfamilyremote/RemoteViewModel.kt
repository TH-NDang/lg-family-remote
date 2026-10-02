package vn.ndang.lgfamilyremote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import vn.ndang.lgfamilyremote.data.TvStore
import vn.ndang.lgfamilyremote.network.*

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TvStore(application)
    private val discovery = TvDiscovery(application)
    private val mutableState = MutableStateFlow(RemoteState())
    val state = mutableState.asStateFlow()
    private var foreground = false
    @Volatile private var generation = 0
    private var session: WebOsSession? = null
    private var connectionJob: Job? = null
    private var scanJob: Job? = null
    private var commandJob: Job? = null

    init {
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
        mutableState.update { it.copy(connection = ConnectionState.OFFLINE, busy = false,
            status = if (it.tv == null) "Chưa kết nối tivi" else "Sẽ tự kết nối khi mở app") }
    }
    private fun stopConnection() {
        generation++
        commandJob?.cancel()
        connectionJob?.cancel()
        session?.close()
        session = null
    }
    fun clearError() { mutableState.update { it.copy(error = null) } }
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
            ((target.uid.isNotBlank() && target.uid == saved.uid) || target.host == saved.host)
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
            status = "Đã dừng kết nối. Bấm Thử lại khi sẵn sàng.") }
    }
    fun forget() {
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
        viewModelScope.launch {
            if (updated.clientKey.isNotBlank()) save(updated)
        }
    }
    private suspend fun save(tv: TvConfig) {
        try { withContext(Dispatchers.IO) { store.save(tv) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            mutableState.update { it.copy(error = "Đã kết nối nhưng chưa lưu được. Lần sau có thể cần ghép đôi lại.") }
        }
    }
    private fun connect(initial: TvConfig, allowPairing: Boolean) {
        stopConnection()
        val token = generation
        mutableState.update { it.copy(tv = initial, error = null, busy = false, volume = VolumeState()) }
        connectionJob = viewModelScope.launch {
            var target = initial
            var pairingAllowed = allowPairing
            var attempt = 0
            while (isActive && foreground && generation == token) {
                if (attempt > 0 && attempt % 3 == 1 && target.uid.isNotBlank()) {
                    try {
                        val relocated = discovery.scan().firstOrNull { it.uid == target.uid }
                        if (relocated != null) target = target.copy(host = relocated.host)
                    } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Retry saved IP. */ }
                }
                mutableState.update { it.copy(connection = ConnectionState.CONNECTING,
                    status = if (attempt == 0) "Đang kết nối tivi…" else "Đang thử kết nối lại…") }
                val builder = OkHttpClient.Builder()
                localNetwork(getApplication())?.let { builder.socketFactory(it.socketFactory) }
                val current = WebOsSession(target, pairingAllowed, builder,
                    onPairing = {
                        if (generation == token) mutableState.update { it.copy(
                            connection = ConnectionState.PAIRING, status = "Hãy chọn Cho phép trên màn hình tivi") }
                    },
                    onVolume = { value ->
                        if (generation == token) mutableState.update { it.copy(volume = VolumeState(
                            value.level ?: it.volume.level, value.muted ?: it.volume.muted)) }
                    })
                session = current
                var retryable = false
                try {
                    target = current.connect()
                    ensureActive()
                    save(target)
                    pairingAllowed = false
                    attempt = 0
                    mutableState.update { it.copy(tv = target, connection = ConnectionState.CONNECTED,
                        status = "Đã kết nối", volume = VolumeState()) }
                    current.subscribeVolume()
                    throw current.awaitClosed()
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    if (generation != token) break
                    val problem = e as? TvException
                    retryable = !pairingAllowed && (problem == null || problem.kind == ErrorKind.NETWORK)
                    mutableState.update { it.copy(connection = ConnectionState.OFFLINE, busy = false,
                        status = problem?.message ?: "Chưa kết nối được tivi. Kiểm tra mạng nhà.") }
                } finally {
                    current.close()
                    if (session === current) session = null
                }
                if (!retryable) break
                attempt++
                delay(listOf(4000L, 8000L, 15000L, 30000L)[(attempt - 1).coerceAtMost(3)])
            }
        }
    }
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
    fun key(name: String) = command { it.button(name) }
    fun volume(up: Boolean) = command { it.changeVolume(up) }
    fun mute() = command { it.toggleMute() }
    fun youtube() = command { it.launchYouTube(state.value.tv?.youtubeId.orEmpty()) }
    override fun onCleared() {
        stopConnection()
        super.onCleared()
    }
}
