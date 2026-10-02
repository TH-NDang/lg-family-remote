# Security and privacy

- No advertising, analytics, account service or cloud command proxy. No microphone, camera, location or contacts permission.
- Main and pointer sockets are confined to the selected RFC1918/link-local IPv4 TV address. Device-description HTTP URLs must match the SSDP sender; redirects and XML entities are rejected. IPv6 and hostnames are deliberately out of scope in v0.1.0.
- Secure mode uses WSS 3001. A per-session trust manager observes the local TV certificate only during user-initiated pairing, pins it only after successful TV registration, and rejects subsequent mismatches. First-use pairing must occur on a trusted LAN. This does not provide independent cryptographic proof that the peer is a genuine LG television; some LG models share certificates. Expired certificates are not silently trusted.
- Pointer URLs returned as WS are upgraded to WSS in secure mode on the same selected IP. Secure mode is never silently downgraded. Explicit legacy WS 3000 is unencrypted and exposes pairing/control traffic to capable LAN observers.
- Pairing config is AES-GCM encrypted with an Android Keystore key in noBackupFilesDir. Android backup is disabled. Credentials are not logged or exported. Uninstalling / clearing app data loses pairing.
- Commands are not automatically retried or carried into a new connection. Closed sessions cancel their sockets and fail pending requests. Like other network remotes, an already delivered command cannot be undone.
- Backgrounding the app closes connections; there is no foreground/background service. Reconnection runs only while the app is visible.
- Signing keys belong in GitHub Actions Secrets, never repository files, artifacts or logs. Preview builds use ephemeral debug keys and are explicitly labeled as previews.
- SSAP interfaces vary by firmware. Build/tests are not a physical compatibility certification.

Report a security problem privately to the repository owner; never publish client keys, signing bundles, or exact private household connection details in an issue.
