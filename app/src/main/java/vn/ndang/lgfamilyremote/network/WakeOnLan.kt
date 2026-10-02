package vn.ndang.lgfamilyremote.network

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vn.ndang.lgfamilyremote.ErrorKind
import vn.ndang.lgfamilyremote.TvConfig
import vn.ndang.lgfamilyremote.TvException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Locale

/** Pure packet/parsing functions can be tested without a TV or Android runtime. */
object WakeProtocol {
    fun normalizeMac(raw: String): String? {
        val text = raw.trim()
        val hex = when {
            text.matches(Regex("[0-9a-fA-F]{12}")) -> text
            text.matches(Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")) -> text.replace(":", "")
            text.matches(Regex("([0-9a-fA-F]{2}-){5}[0-9a-fA-F]{2}")) -> text.replace("-", "")
            else -> return null
        }
        if (hex.all { it == '0' } || hex.take(2).toInt(16) and 1 != 0) return null
        return hex.uppercase(Locale.ROOT).chunked(2).joinToString(":")
    }
    fun parseMacs(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val parts = raw.trim().split(Regex("[,;\\s]+"))
        require(parts.size <= 4) { "Chỉ nhập tối đa 4 địa chỉ MAC của tivi." }
        return parts.map { normalizeMac(it) ?: throw IllegalArgumentException(
            "Địa chỉ MAC chưa đúng. Ví dụ: AA:BB:CC:DD:EE:02") }.distinct()
    }
    fun packet(mac: String): ByteArray {
        val normalized = requireNotNull(normalizeMac(mac)) { "Invalid TV MAC address" }
        val bytes = normalized.split(':').map { it.toInt(16).toByte() }.toByteArray()
        return ByteArray(102).also { packet ->
            for (i in 0 until 6) packet[i] = 0xff.toByte()
            repeat(16) { bytes.copyInto(packet, 6 + it * 6) }
        }
    }
    /**
     * LG firmware is inconsistent here:
     * newer TVs commonly return wifiInfo/wiredInfo, while some older examples use wifi/wired.
     * Only inspect TV-interface fields; never accidentally persist a gateway/router MAC.
     */
    fun fromConnectionInfo(payload: JSONObject): List<String> {
        val containers = listOfNotNull(
            payload,
            payload.optJSONObject("networkInfo"),
            payload.optJSONObject("connectionInfo")
        )
        val keys = listOf("wifiInfo", "wiredInfo", "wifi", "wired", "ethernetInfo")
        val values = mutableListOf<String>()
        for (container in containers) {
            for (key in keys) {
                container.optJSONObject(key)?.optString("macAddress")
                    ?.takeIf { it.isNotBlank() }?.let(values::add)
            }
            container.optString("macAddress").takeIf { it.isNotBlank() }?.let(values::add)
        }
        return values.mapNotNull(::normalizeMac).distinct()
    }

    fun deliveryTargets(local: String?, prefix: Int?, tvIp: String): List<String> {
        val result = linkedSetOf<String>()
        result += "255.255.255.255"
        if (local != null && prefix != null) broadcastFor(local, prefix, tvIp)?.let(result::add)
        if (LanRules.isPrivateIpv4(tvIp)) result += tvIp
        return result.toList()
    }

    /** Only derive broadcasts for a matching local IPv4 subnet; never assume /24. */
    fun broadcastFor(local: String, prefix: Int, target: String): String? {
        if (prefix !in 1..30 || !LanRules.isPrivateIpv4(local) || !LanRules.isPrivateIpv4(target)) return null
        fun number(ip: String): Long = ip.split('.').fold(0L) { n, part -> (n shl 8) or part.toLong() }
        val mask = (0xffffffffL shl (32 - prefix)) and 0xffffffffL
        val address = number(local)
        if ((address and mask) != (number(target) and mask)) return null
        val broadcast = (address and mask) or (mask xor 0xffffffffL)
        return listOf(24, 16, 8, 0).joinToString(".") { ((broadcast shr it) and 255).toString() }
    }
}

/** No cloud, ARP scanning or background wake-ups. Called only by the Bật tivi action. */
class WakeOnLan(private val context: Context) {
    suspend fun send(tv: TvConfig) = withContext(Dispatchers.IO) {
        LanRules.requireHost(tv.host)
        val macs = tv.wakeMacs.mapNotNull(WakeProtocol::normalizeMac).distinct().take(4)
        if (macs.isEmpty()) throw TvException(ErrorKind.COMMAND,
            "Chưa có địa chỉ MAC để bật tivi. Khi TV đang bật, app sẽ thử đọc lại tự động; bạn cũng có thể nhập MAC trong Cài đặt → Nhập IP / tùy chọn.")
        val network = localNetwork(context) ?: throw TvException(ErrorKind.NETWORK,
            "Hãy kết nối điện thoại với Wi-Fi nhà để bật tivi.")
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val targets = linkedSetOf<String>()
        targets += "255.255.255.255"
        manager.getLinkProperties(network)?.linkAddresses?.forEach { link ->
            if (link.address is Inet4Address) {
                WakeProtocol.deliveryTargets(link.address.hostAddress.orEmpty(), link.prefixLength, tv.host)
                    .forEach(targets::add)
            }
        }
        targets += tv.host
        val addresses = targets.mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }
        val ports = intArrayOf(9, 7)
        var sent = false
        DatagramSocket(null).use { socket ->
            network.bindSocket(socket)
            socket.bind(InetSocketAddress(0))
            socket.broadcast = true
            repeat(5) { attempt ->
                ensureActive()
                for (mac in macs) {
                    val bytes = WakeProtocol.packet(mac)
                    for (address in addresses) {
                        for (port in ports) {
                            try {
                                socket.send(DatagramPacket(bytes, bytes.size, address, port))
                                sent = true
                            } catch (_: java.io.IOException) {
                                // Continue through the local delivery plan.
                            }
                        }
                    }
                }
                if (attempt < 4) delay(200)
            }
        }
        if (!sent) throw TvException(ErrorKind.NETWORK, "Chưa gửi được lệnh bật tivi. Kiểm tra Wi-Fi nhà.")
        // UDP transmission is not an acknowledgement. The UI waits for the real connection dot.
    }
}
