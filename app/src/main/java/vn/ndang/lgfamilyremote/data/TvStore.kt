package vn.ndang.lgfamilyremote.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import vn.ndang.lgfamilyremote.TvConfig
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** All functions are called off the main thread. Credentials never leave noBackupFilesDir. */
class TvStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "paired-tv.enc"))
    private val alias = "lg_family_remote_pairing_v1"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    @Synchronized fun load(): TvConfig? {
        if (!file.baseFile.exists()) return null
        val bytes = file.openRead().use { it.readBytes() }
        require(bytes.size <= 65536)
        val j = JSONObject(bytes.toString(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(j.getString("iv"), Base64.NO_WRAP)))
        val clear = cipher.doFinal(Base64.decode(j.getString("data"), Base64.NO_WRAP))
        return TvConfig.fromJson(JSONObject(clear.toString(Charsets.UTF_8)))
    }
    @Synchronized fun save(tv: TvConfig) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.doFinal(tv.toJson().toString().toByteArray(Charsets.UTF_8))
        val j = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(data, Base64.NO_WRAP))
        val stream = file.startWrite()
        try {
            stream.write(j.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (e: Exception) { file.failWrite(stream); throw e }
    }
    @Synchronized fun clear() { file.delete() }
}
