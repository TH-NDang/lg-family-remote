package vn.ndang.lgfamilyremote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.ndang.lgfamilyremote.*

@Composable
fun RemoteApp(vm: RemoteViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var settings by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf("") }
    var powerMenu by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.connection) {
        if (state.connection == ConnectionState.CONNECTED) settings = false
    }
    LaunchedEffect(state.notice) {
        state.notice?.let { snackbar.showSnackbar(it); vm.clearNotice() }
    }
    BackHandler(enabled = settings) { settings = false }
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            if (!state.loaded) {
                CircularProgressIndicator(Modifier.padding(48.dp))
            } else Column(Modifier.widthIn(max = 460.dp).fillMaxSize()) {
                // Keep the fixed compact header and the tappable dot from 0.1.1.
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (settings && state.tv != null) {
                        IconButton(onClick = { settings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Về màn hình điều khiển")
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (settings) "Thiết lập tivi" else "Điều khiển TV",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (settings) Text(state.tv?.name ?: "Dành cho cả nhà",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!settings && state.tv != null) {
                        IconButton(onClick = { settings = true }, modifier = Modifier.semantics {
                            contentDescription = "Kết nối tivi: ${state.status}. Mở cài đặt kết nối."
                        }) { ConnectionDot(state) }
                        IconButton(onClick = { powerMenu = true }, enabled = !state.busy,
                            modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.PowerSettingsNew, "Nguồn tivi: bật hoặc tắt",
                                Modifier.size(29.dp), tint = Color(0xFFB3261E))
                        }
                        IconButton(onClick = { settings = true }) {
                            Icon(Icons.Default.Settings, "Cài đặt kết nối tivi")
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.error != null) ErrorCard(state.error!!, vm::clearError)
                    if (settings || state.tv == null) {
                        SetupPanel(state, vm, { confirm = "repair" }, { confirm = "forget" })
                    } else {
                        RemotePanel(state, vm::youtube, vm::key, vm::volume, vm::mute)
                        Text("Không quảng cáo · Chỉ dùng mạng nhà", Modifier.fillMaxWidth().padding(top = 4.dp),
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).widthIn(max = 460.dp).padding(16.dp))
        }
    }
    if (powerMenu && state.tv != null) PowerDialog(state,
        close = { powerMenu = false },
        turnOn = { powerMenu = false; vm.powerOn() },
        turnOff = { powerMenu = false; vm.powerOff() },
        settings = { powerMenu = false; settings = true })
    if (confirm.isNotEmpty()) AlertDialog(
        onDismissRequest = { confirm = "" },
        title = { Text(if (confirm == "forget") "Quên tivi này?" else "Ghép đôi lại với tivi?") },
        text = { Text(if (confirm == "forget") "Lần sau cần thiết lập kết nối lại trên điện thoại này."
            else "Chỉ tiếp tục khi bạn đang dùng mạng nhà và đã kiểm tra đúng tivi. App sẽ ghi nhận lại chứng chỉ và yêu cầu Cho phép trên tivi.") },
        confirmButton = { TextButton(onClick = {
            if (confirm == "forget") vm.forget() else vm.repair()
            confirm = ""
        }) { Text("Tiếp tục") } },
        dismissButton = { TextButton(onClick = { confirm = "" }) { Text("Hủy") } }
    )
}

@Composable
private fun ConnectionDot(state: RemoteState) {
    val color = when (state.connection) {
        ConnectionState.CONNECTED -> Color(0xFF257545)
        ConnectionState.CONNECTING, ConnectionState.PAIRING -> Color(0xFFB07900)
        else -> Color(0xFF8A929E)
    }
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@Composable
private fun PowerDialog(state: RemoteState, close: () -> Unit, turnOn: () -> Unit,
                        turnOff: () -> Unit, settings: () -> Unit) {
    // Two explicit actions avoid treating an offline socket as proof that the TV is off.
    AlertDialog(onDismissRequest = close,
        icon = { Icon(Icons.Default.PowerSettingsNew, null, tint = Color(0xFFB3261E)) },
        title = { Text("Nguồn tivi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = turnOn,
                    enabled = !state.busy && state.connection != ConnectionState.PAIRING,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                    Text("Bật tivi", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = turnOff,
                    enabled = !state.busy && state.connection == ConnectionState.CONNECTED,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                    Text("Tắt tivi", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                Text("Bật qua mạng cần tivi hỗ trợ và bật TV On With Mobile / Turn on via Wi-Fi.",
                    style = MaterialTheme.typography.bodyMedium)
                if (state.tv?.wakeMacs.isNullOrEmpty()) TextButton(onClick = settings) {
                    Text("Thiết lập bật tivi")
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text("Đóng") } })
}

@Composable
private fun ErrorCard(message: String, dismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = dismiss, modifier = Modifier.align(Alignment.End)) { Text("Đóng thông báo") }
        }
    }
}

/** Detailed connection controls exist only inside setup, never on the main remote. */
@Composable
private fun ConnectionCard(state: RemoteState, retry: () -> Unit, settings: () -> Unit, cancel: () -> Unit) {
    val connected = state.connection == ConnectionState.CONNECTED
    val connecting = state.connection in listOf(ConnectionState.CONNECTING, ConnectionState.PAIRING)
    Surface(shape = RoundedCornerShape(18.dp), color = if (connected) Color(0xFFE8F3EC) else Color(0xFFEAF0F7)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (connecting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Box(Modifier.size(10.dp).background(if (connected) Color(0xFF257545) else Color(0xFF7B8491), CircleShape))
                Text(state.status, Modifier.weight(1f), fontSize = 15.sp,
                    color = if (connected) Color(0xFF215535) else MaterialTheme.colorScheme.onSurface)
            }
            if (!connected) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = if (connecting) cancel else retry) {
                    Text(if (connecting) "Dừng kết nối" else "Thử lại")
                }
                TextButton(onClick = settings) { Text("Thiết lập") }
            }
        }
    }
}

@Composable
private fun RemotePanel(state: RemoteState, youtube: () -> Unit, key: (String) -> Unit,
                        volume: (Boolean) -> Unit, mute: () -> Unit) {
    val enabled = state.connection == ConnectionState.CONNECTED && !state.busy
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Shortcut("YouTube", Icons.Default.PlayArrow, Color(0xFFC62828), enabled, Modifier.weight(1f), youtube)
        Shortcut("Trang chủ", Icons.Default.Home, Color(0xFF315ACB), enabled, Modifier.weight(1f)) { key("HOME") }
    }
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Direction(Icons.Default.KeyboardArrowUp, "Lên", enabled, Modifier.width(96.dp)) { key("UP") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Direction(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Trái", enabled, Modifier.weight(1f)) { key("LEFT") }
                Button(onClick = { key("ENTER") }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 76.dp), shape = RoundedCornerShape(22.dp)) {
                    Text("OK", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Direction(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Phải", enabled, Modifier.weight(1f)) { key("RIGHT") }
            }
            Direction(Icons.Default.KeyboardArrowDown, "Xuống", enabled, Modifier.width(96.dp)) { key("DOWN") }
        }
    }
    FilledTonalButton(onClick = { key("BACK") }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(18.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
        Spacer(Modifier.width(10.dp)); Text("Quay lại", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SoundButton("Giảm tiếng", Icons.AutoMirrored.Filled.VolumeDown, enabled, Modifier.weight(1f)) { volume(false) }
        SoundButton("Tăng tiếng", Icons.AutoMirrored.Filled.VolumeUp, enabled, Modifier.weight(1f)) { volume(true) }
    }
    OutlinedButton(onClick = mute, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)) {
        Icon(if (state.volume.muted == true) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, null)
        Spacer(Modifier.width(10.dp))
        Text(if (state.volume.muted == true) "Bật lại tiếng" else "Tắt tiếng", fontSize = 19.sp)
        if (state.volume.level != null) Text("  ·  ${state.volume.level}", fontSize = 17.sp)
    }
}

@Composable
private fun Shortcut(label: String, icon: ImageVector, color: Color, enabled: Boolean,
                     modifier: Modifier, click: () -> Unit) {
    Button(onClick = click, enabled = enabled, modifier = modifier.heightIn(min = 98.dp),
        shape = RoundedCornerShape(23.dp), colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, Modifier.size(32.dp))
            Text(label, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Direction(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier, click: () -> Unit) {
    FilledTonalButton(onClick = click, enabled = enabled, modifier = modifier.heightIn(min = 62.dp),
        shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(8.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun SoundButton(label: String, icon: ImageVector, enabled: Boolean, modifier: Modifier, click: () -> Unit) {
    FilledTonalButton(onClick = click, enabled = enabled, modifier = modifier.heightIn(min = 80.dp),
        shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, Modifier.size(29.dp))
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SetupPanel(state: RemoteState, vm: RemoteViewModel, repair: () -> Unit, forget: () -> Unit) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var host by rememberSaveable { mutableStateOf(state.tv?.host.orEmpty()) }
    var legacy by rememberSaveable { mutableStateOf(state.tv?.secure == false) }
    var name by rememberSaveable(state.tv?.host) { mutableStateOf(state.tv?.name ?: "Tivi nhà mình") }
    var youtubeId by rememberSaveable(state.tv?.host) { mutableStateOf(state.tv?.youtubeId.orEmpty()) }
    var wakeMacs by rememberSaveable(state.tv?.wakeMacs) { mutableStateOf(state.tv?.wakeMacs?.joinToString(", ").orEmpty()) }
    val connecting = state.connection in listOf(ConnectionState.CONNECTING, ConnectionState.PAIRING)
    if (state.tv != null) ConnectionCard(state, vm::retry, { advanced = true }, vm::cancelPairing)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Tv, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Kết nối một lần", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Bật tivi bằng remote thường. Điện thoại và tivi cần dùng cùng mạng nhà. Sau khi chọn tivi, bấm Cho phép trên màn hình tivi.", fontSize = 17.sp)
            Button(onClick = vm::search, enabled = !state.searching && !connecting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
                if (state.searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.searching) "Đang tìm tivi…" else "Tìm tivi", fontSize = 19.sp)
            }
            state.found.forEach { tv ->
                OutlinedButton(onClick = { vm.pair(tv) }, enabled = !connecting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(tv.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(tv.host, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    TextButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(if (advanced) "Thu gọn tùy chọn" else "Nhập IP / tùy chọn", fontSize = 16.sp)
    }
    if (advanced) {
        OutlinedTextField(value = host, onValueChange = { host = it.take(30) }, label = { Text("IP của tivi") },
            placeholder = { Text("192.168.1.20") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = legacy, onCheckedChange = { legacy = it })
            Text("TV cũ: dùng WS cổng 3000", Modifier.weight(1f))
        }
        if (legacy) Text("Chế độ TV cũ không mã hóa kết nối. Chỉ bật trên mạng nhà đáng tin cậy.",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { vm.pair(TvConfig(host = host.trim(), name = name, secure = !legacy, youtubeId = youtubeId)) },
            enabled = host.isNotBlank() && !connecting, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Kết nối bằng IP", fontSize = 18.sp)
        }
        HorizontalDivider()
        OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Tên tivi") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = youtubeId, onValueChange = { youtubeId = it.take(160) },
            label = { Text("Mã YouTube (để trống = tự tìm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (state.tv != null) {
            OutlinedButton(onClick = { vm.saveOptions(name, youtubeId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Lưu tên và tùy chọn YouTube")
            }
            HorizontalDivider()
            Text("Bật tivi qua mạng", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("App thử đọc MAC tự động khi kết nối. Trên tivi, bật TV On With Mobile → Turn on via Wi-Fi. Một số model không hỗ trợ; tivi phải còn cắm điện.")
            OutlinedTextField(value = wakeMacs, onValueChange = { wakeMacs = it.take(100) },
                label = { Text("MAC Wi-Fi / LAN của tivi") },
                placeholder = { Text("AA:BB:CC:DD:EE:02") },
                supportingText = { Text("Nhiều địa chỉ cách nhau bằng dấu phẩy. Không nhập MAC của điện thoại.") },
                modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = vm::readWakeAddresses,
                enabled = state.connection == ConnectionState.CONNECTED && !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Lấy địa chỉ từ tivi") }
            OutlinedButton(onClick = { vm.saveWakeMacs(wakeMacs) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Lưu địa chỉ bật tivi")
            }
            HorizontalDivider()
            OutlinedButton(onClick = repair, enabled = !connecting, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Ghép đôi lại với tivi")
            }
            TextButton(onClick = forget, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Quên tivi này", color = MaterialTheme.colorScheme.error)
            }
        }
        Text("Phiên bản ${BuildConfig.VERSION_NAME}\nDành cho LG webOS. Bật tivi qua mạng tùy model. Không phải app chính thức của LG hay YouTube.",
            style = MaterialTheme.typography.bodySmall)
    }
}
