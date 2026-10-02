package vn.ndang.lgfamilyremote.network

import vn.ndang.lgfamilyremote.ErrorKind
import vn.ndang.lgfamilyremote.TvException

/** A disconnected socket does not prove the television switched off. This command is never replayed. */
suspend fun WebOsSession.requestPowerOff(): Boolean {
    if (!isOpen) throw TvException(ErrorKind.NETWORK, "Tivi chưa kết nối. Hãy bật tivi bằng remote thường.")
    return try {
        request("system/turnOff")
        true
    } catch (e: TvException) {
        // Some TVs close the connection before returning the acknowledgement.
        // Return an unknown outcome, rather than displaying a false success or retrying the command.
        if (e.kind == ErrorKind.NETWORK && !isOpen) false else throw e
    }
}
