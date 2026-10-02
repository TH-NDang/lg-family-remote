package vn.ndang.lgfamilyremote.network

/** Compatibility entry point: a closed socket is not a verified power-off acknowledgement. */
suspend fun WebOsSession.requestPowerOff(): Boolean = !turnOff().optBoolean("disconnected", false)
