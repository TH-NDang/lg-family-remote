package vn.ndang.lgfamilyremote.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Xml
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import vn.ndang.lgfamilyremote.TvConfig
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
fun localNetwork(context: Context): Network? {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    return cm.allNetworks.firstOrNull { network ->
        cm.getNetworkCapabilities(network)?.let {
            it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } == true
    }
}

class TvDiscovery(private val context: Context) {
    suspend fun scan(): List<TvConfig> = withContext(Dispatchers.IO) {
        val network = localNetwork(context)
        val lock = context.applicationContext.getSystemService(WifiManager::class.java)
            .createMulticastLock("lg-family-discovery").apply { setReferenceCounted(false) }
        val found = linkedMapOf<String, Pair<String, String>>()
        try {
            lock.acquire()
            DatagramSocket(null).use { socket ->
                network?.bindSocket(socket)
                socket.bind(InetSocketAddress(0))
                socket.soTimeout = 650
                val group = InetAddress.getByName("239.255.255.250")
                val message = ("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:lge-com:service:webos-second-screen:1\r\n\r\n")
                    .toByteArray(Charsets.UTF_8)
                repeat(2) { socket.send(DatagramPacket(message, message.size, group, 1900)) }
                val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
                while (System.nanoTime() < until && found.size < 12) {
                    ensureActive()
                    val packet = DatagramPacket(ByteArray(8192), 8192)
                    try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                    val host = packet.address.hostAddress ?: continue
                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    if (!text.startsWith("HTTP/1.1 200", true)) continue
                    val headers = Protocol.ssdpHeaders(text)
                    if (!headers["st"].orEmpty().contains("webos-second-screen", true)) continue
                    if (!LanRules.isPrivateIpv4(host)) continue
                    found[host] = headers["usn"].orEmpty().substringBefore("::") to headers["location"].orEmpty()
                }
            }
            val builder = OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS)
                .followRedirects(false).followSslRedirects(false)
            network?.let { builder.socketFactory(it.socketFactory) }
            val client = builder.build()
            try {
                found.map { (host, details) ->
                    ensureActive()
                    val (uid, location) = details
                    var name = "Tivi LG · $host"
                    var actualUid = uid
                    if (LanRules.descriptionAllowed(location, host)) {
                        try {
                            client.newCall(Request.Builder().url(location).build()).execute().use { response ->
                                val xml = response.body?.source()?.let { source ->
                                    source.request(65537)
                                    if (source.buffer.size <= 65536) source.buffer.clone().readUtf8() else null
                                }
                                if (response.isSuccessful && xml != null && !xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true)) {
                                    val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
                                    var event = parser.eventType
                                    while (event != XmlPullParser.END_DOCUMENT) {
                                        if (event == XmlPullParser.START_TAG && parser.name == "friendlyName") {
                                            name = parser.nextText().trim().take(80).ifBlank { name }
                                        } else if (event == XmlPullParser.START_TAG && parser.name == "UDN") {
                                            actualUid = parser.nextText().trim().take(150).ifBlank { actualUid }
                                        }
                                        event = parser.next()
                                    }
                                }
                            }
                        } catch (e: CancellationException) { throw e } catch (_: Exception) { /* IP remains selectable. */ }
                    }
                    TvConfig(host = host, name = name, uid = actualUid)
                }
            } finally {
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        } finally { if (lock.isHeld) lock.release() }
    }
}
