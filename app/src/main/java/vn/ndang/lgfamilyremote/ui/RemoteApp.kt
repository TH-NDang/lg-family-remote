package vn.ndang.lgfamilyremote.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.ndang.lgfamilyremote.*
import kotlin.math.abs

@Composable
fun RemoteApp(vm: RemoteViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var settings by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf("") }
    var pin by rememberSaveable(state.tv?.host) { mutableStateOf("") }
    val context = LocalContext.current
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói nội dung cần tìm trên YouTube")
        }
    }
    val voiceAvailable = remember(context) {
        voiceIntent.resolveActivity(context.packageManager) != null
    }
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let(vm::voiceSearch)
        }
    }
    LaunchedEffect(state.connection) {
        if (state.connection == ConnectionState.CONNECTED) settings = false
    }
    LaunchedEffect(state.pairing, state.connection) {
        if (state.pairing != PairingKind.PIN || state.connection == ConnectionState.CONNECTED) pin = ""
    }
    BackHandler(enabled = settings) { settings = false }
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            if (!state.loaded) {
                CircularProgressIndicator(Modifier.padding(48.dp))
            } else Column(Modifier.widthIn(max = 460.dp).fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (settings || state.tv == null) {
                        if (state.tv != null) {
                            IconButton(onClick = { settings = false }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Về màn hình điều khiển")
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Thiết lập tivi", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            if (state.tv != null) {
                                Text(
                                    state.tv?.name ?: "Tivi nhà mình",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { settings = true },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Default.Settings, "Cài đặt kết nối tivi", Modifier.size(28.dp))
                        }
                        IconButton(
                            onClick = { settings = true },
                            modifier = Modifier
                                .size(44.dp)
                                .semantics {
                                    contentDescription = "Kết nối tivi: ${state.status}. Mở cài đặt kết nối."
                                }
                        ) {
                            ConnectionDot(state)
                        }
                        Spacer(Modifier.weight(1f))
                        val powerAction = powerActionFor(state.connection, state.busy)
                        IconButton(
                            onClick = vm::powerToggle,
                            enabled = powerAction != PowerAction.NONE,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFFFE4E2), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = when (powerAction) {
                                    PowerAction.TURN_OFF -> "Tắt tivi"
                                    PowerAction.TURN_ON -> "Bật tivi"
                                    PowerAction.NONE -> "Nguồn tivi"
                                },
                                modifier = Modifier.size(36.dp),
                                tint = if (powerAction == PowerAction.NONE)
                                    Color(0xFF9A8A89) else Color(0xFFB3261E)
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if ((settings || state.tv == null) && state.error != null) {
                        ErrorCard(state.error!!, vm::clearError)
                    }
                    if (settings || state.tv == null) {
                        SetupPanel(state, vm, { confirm = "repair" }, { confirm = "forget" })
                    } else {
                        RemotePanel(
                            state = state,
                            youtube = vm::youtube,
                            voiceAvailable = voiceAvailable,
                            voiceSearch = { if (voiceAvailable) voiceLauncher.launch(voiceIntent) },
                            key = vm::key,
                            replaceText = vm::replaceTextInput,
                            textEnter = vm::textInputEnter,
                            pointerMove = vm::pointerMove,
                            pointerClick = vm::pointerClick,
                            volume = vm::volume,
                            mute = vm::mute
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
    if (state.connection == ConnectionState.PAIRING && state.pairing == PairingKind.PIN) {
        PinPairingDialog(state = state, pin = pin,
            onPinChange = { pin = it.filter { ch -> ch in '0'..'9' }.take(12) },
            submit = { vm.submitPin(pin) }, cancel = vm::cancelPairing)
    }
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
private fun PinPairingDialog(state: RemoteState, pin: String, onPinChange: (String) -> Unit,
                             submit: () -> Unit, cancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Default.Dialpad, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Nhập mã trên tivi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nhập các chữ số đang hiện trên màn hình tivi. Mã này chỉ dùng cho lần ghép đôi hiện tại và không được lưu.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = { Text("Mã trên tivi") },
                    placeholder = { Text("Ví dụ: 123456") },
                    singleLine = true,
                    enabled = !state.pinSubmitting,
                    isError = state.pinError != null,
                    supportingText = {
                        Text(state.pinError ?: "Sau khi kết nối thành công, app chỉ lưu khóa ghép đôi để lần sau tự kết nối.")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.pinSubmitting) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = submit, enabled = pin.length in 4..12 && !state.pinSubmitting) {
                Text(if (state.pinSubmitting) "Đang kết nối…" else "Kết nối")
            }
        },
        dismissButton = { TextButton(onClick = cancel, enabled = !state.pinSubmitting) { Text("Hủy") } }
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
private fun RemotePanel(
    state: RemoteState,
    youtube: () -> Unit,
    voiceAvailable: Boolean,
    voiceSearch: () -> Unit,
    key: (String) -> Unit,
    replaceText: (String) -> Unit,
    textEnter: () -> Unit,
    pointerMove: (Float, Float) -> Unit,
    pointerClick: () -> Unit,
    volume: (Boolean) -> Unit,
    mute: () -> Unit
) {
    val enabled = state.connection == ConnectionState.CONNECTED && !state.busy
    var textMode by rememberSaveable { mutableStateOf(false) }
    var mouseMode by rememberSaveable { mutableStateOf(false) }
    var textValue by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.textInputFocused) {
        if (state.textInputFocused) {
            mouseMode = false
            textMode = true
        } else {
            textValue = ""
        }
    }
    LaunchedEffect(textMode, state.textInputFocused, enabled) {
        if (textMode && enabled) {
            kotlinx.coroutines.delay(100)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        } else if (!textMode) {
            keyboardController?.hide()
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Shortcut("YouTube", Icons.Default.PlayArrow, Color(0xFFC62828), enabled, Modifier.weight(1f), youtube)
        Shortcut("Trang chủ", Icons.Default.Home, Color(0xFF315ACB), enabled, Modifier.weight(1f)) { key("HOME") }
    }
    FilledTonalButton(
        onClick = voiceSearch,
        enabled = enabled && voiceAvailable,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(Icons.Default.Mic, null, Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text("Tìm YouTube", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (mouseMode) 184.dp else 236.dp)
        ) {
            if (mouseMode) {
                TouchpadSurface(
                    enabled = enabled,
                    move = pointerMove,
                    click = pointerClick,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularDpad(
                    enabled = enabled,
                    key = key,
                    modifier = Modifier
                        .size(236.dp)
                        .align(Alignment.Center)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ModeButton(
                    icon = Icons.Default.Keyboard,
                    description = "Nhập chữ bằng bàn phím điện thoại",
                    active = textMode || state.textInputFocused,
                    enabled = enabled
                ) {
                    textMode = !textMode
                    if (textMode) mouseMode = false
                }
                ModeButton(
                    icon = Icons.Default.Mouse,
                    description = "Chế độ rê chuột",
                    active = mouseMode,
                    enabled = enabled
                ) {
                    mouseMode = !mouseMode
                    if (mouseMode) {
                        textMode = false
                        keyboardController?.hide()
                    }
                }
            }
        }

        if (textMode || state.textInputFocused) {
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    val next = it.take(500)
                    textValue = next
                    replaceText(next)
                },
                label = {
                    Text(if (state.textInputFocused) "Nhập trên tivi" else "Nhập chữ")
                },
                placeholder = {
                    Text(if (state.textInputFocused) "Gõ bằng bàn phím điện thoại"
                        else "Chọn một ô nhập trên tivi")
                },
                supportingText = {
                    Text(if (state.textInputFocused) "Tivi đang chờ nhập chữ."
                        else "Khi tivi focus vào ô nhập, bàn phím điện thoại sẽ tự hiện.")
                },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    textEnter()
                    keyboardController?.hide()
                }),
                trailingIcon = {
                    IconButton(onClick = textEnter, enabled = enabled) {
                        Icon(Icons.Default.KeyboardReturn, "Gửi Enter")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }
    FilledTonalButton(
        onClick = { key("BACK") },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
        Spacer(Modifier.width(10.dp))
        Text("Quay lại", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SoundButton("Giảm tiếng", Icons.AutoMirrored.Filled.VolumeDown, "−", enabled, Modifier.weight(1f)) { volume(false) }
        SoundButton("Tăng tiếng", Icons.AutoMirrored.Filled.VolumeUp, "+", enabled, Modifier.weight(1f)) { volume(true) }
    }
    OutlinedButton(
        onClick = mute,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(if (state.volume.muted == true) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, null)
        Spacer(Modifier.width(10.dp))
        Text(if (state.volume.muted == true) "Bật lại tiếng" else "Tắt tiếng", fontSize = 19.sp)
        if (state.volume.level != null) Text("  ·  ${state.volume.level}", fontSize = 17.sp)
    }
}

@Composable
private fun CircularDpad(enabled: Boolean, key: (String) -> Unit) {
    Surface(
        modifier = Modifier.size(264.dp),
        shape = CircleShape,
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            DpadButton(
                Icons.Default.KeyboardArrowUp, "Lên", enabled,
                Modifier.align(Alignment.TopCenter)
            ) { key("UP") }
            DpadButton(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Trái", enabled,
                Modifier.align(Alignment.CenterStart)
            ) { key("LEFT") }
            Button(
                onClick = { key("ENTER") },
                enabled = enabled,
                modifier = Modifier.align(Alignment.Center).size(86.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("OK", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            DpadButton(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, "Phải", enabled,
                Modifier.align(Alignment.CenterEnd)
            ) { key("RIGHT") }
            DpadButton(
                Icons.Default.KeyboardArrowDown, "Xuống", enabled,
                Modifier.align(Alignment.BottomCenter)
            ) { key("DOWN") }
        }
    }
}

@Composable
private fun DpadButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    modifier: Modifier,
    click: () -> Unit
) {
    FilledTonalIconButton(
        onClick = click,
        enabled = enabled,
        modifier = modifier.size(72.dp),
        shape = CircleShape
    ) {
        Icon(icon, description, Modifier.size(40.dp))
    }
}

@Composable
private fun TouchpadSurface(
    enabled: Boolean,
    move: (Float, Float) -> Unit,
    click: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(264.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var last = down.position
                    var travelled = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (travelled < 14f) click()
                            break
                        }
                        val delta = change.position - last
                        if (delta.x != 0f || delta.y != 0f) {
                            travelled += abs(delta.x) + abs(delta.y)
                            move(delta.x * 1.7f, delta.y * 1.7f)
                            last = change.position
                            change.consume()
                        }
                    }
                }
            },
        shape = CircleShape,
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Mouse, "Rê chuột", Modifier.size(54.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text("Rê để di chuyển", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Chạm để bấm", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModeButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    enabled: Boolean,
    click: () -> Unit
) {
    FilledTonalIconButton(
        onClick = click,
        enabled = enabled,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(icon, description, Modifier.size(27.dp))
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
private fun SoundButton(
    label: String,
    icon: ImageVector,
    symbol: String,
    enabled: Boolean,
    modifier: Modifier,
    click: () -> Unit
) {
    FilledTonalButton(
        onClick = click,
        enabled = enabled,
        modifier = modifier.heightIn(min = 84.dp),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(34.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-7).dp)
                        .size(22.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            symbol,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Text(
                label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
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
            Text("Bật tivi bằng remote thường và dùng cùng mạng nhà. Khi ghép lần đầu, tivi có thể hiện mã: nhập mã đó trên điện thoại. Nếu tivi chỉ hỏi quyền, chọn Cho phép.", fontSize = 17.sp)
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
