package no.timnor.app.core.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Tokens are encrypted with an Android Keystore key and kept outside user backups. */
class PlannorSessionStore(context: Context) : PlannorSessionStorage {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = false }

    @Synchronized
    override fun read(): PlannorSession? = runCatching {
        val iv = preferences.getString(IV, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val ciphertext = preferences.getString(DATA, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        json.decodeFromString<PlannorSession>(cipher.doFinal(ciphertext).decodeToString())
    }.getOrNull()

    @Synchronized
    override fun write(session: PlannorSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(json.encodeToString(PlannorSession.serializer(), session).encodeToByteArray())
        check(
            preferences.edit()
                .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString(DATA, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .commit(),
        )
    }

    @Synchronized
    override fun clear() {
        check(preferences.edit().clear().commit())
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "plannor_session_secure"
        const val KEY_ALIAS = "no.timnor.app.plannor.session.v1"
        const val IV = "iv"
        const val DATA = "data"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
